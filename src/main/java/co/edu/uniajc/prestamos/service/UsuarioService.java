package co.edu.uniajc.prestamos.service;

import co.edu.uniajc.prestamos.api.ApiDtos.UsuarioInput;
import co.edu.uniajc.prestamos.api.ApiDtos.UsuarioView;
import co.edu.uniajc.prestamos.api.ApiException;
import co.edu.uniajc.prestamos.model.Usuario;
import co.edu.uniajc.prestamos.model.Estados.RolCodigo;
import co.edu.uniajc.prestamos.repo.RolRepository;
import co.edu.uniajc.prestamos.repo.UsuarioRepository;
import co.edu.uniajc.prestamos.security.AuthService;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder passwords;
    private final AuditService audit;

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder passwords, AuditService audit) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.passwords = passwords;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UsuarioView> list() {
        return usuarios.findAll().stream().map(AuthService::view).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioView get(Long id) {
        return AuthService.view(usuarios.findById(id).orElseThrow(() -> ApiException.notFound("Usuario")));
    }

    @Transactional
    public UsuarioView create(UsuarioInput input, Usuario actor) {
        String email = input.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) throw ApiException.conflict("El correo ya está registrado");
        if (input.password() == null || input.password().length() < 8) throw ApiException.badRequest("La contraseña debe tener al menos 8 caracteres");
        Usuario user = new Usuario();
        apply(user, input, email);
        user.passwordHash = passwords.encode(input.password());
        user = usuarios.save(user);
        audit.record(actor, "CREAR", "USUARIO", user.id, "Cuenta " + user.email + " creada");
        return AuthService.view(user);
    }

    @Transactional
    public UsuarioView update(Long id, UsuarioInput input, Usuario actor) {
        Usuario user = usuarios.findById(id).orElseThrow(() -> ApiException.notFound("Usuario"));
        String email = input.email().trim().toLowerCase();
        usuarios.findByEmailIgnoreCase(email).filter(other -> !other.id.equals(id)).ifPresent(other -> {
            throw ApiException.conflict("El correo ya está registrado");
        });
        boolean willRemainAdmin = input.rol() == RolCodigo.ADMINISTRADOR && !Boolean.FALSE.equals(input.activo());
        if (user.rol.codigo == RolCodigo.ADMINISTRADOR && user.activo && !willRemainAdmin) {
            roles.findLockedByCodigo(RolCodigo.ADMINISTRADOR).orElseThrow();
            if (usuarios.countByRolCodigoAndActivo(RolCodigo.ADMINISTRADOR, true) <= 1)
                throw ApiException.conflict("No se puede desactivar o cambiar el rol del último administrador");
        }
        apply(user, input, email);
        if (input.password() != null && !input.password().isBlank()) {
            if (input.password().length() < 8) throw ApiException.badRequest("La contraseña debe tener al menos 8 caracteres");
            user.passwordHash = passwords.encode(input.password());
        }
        audit.record(actor, "ACTUALIZAR", "USUARIO", user.id, "Cuenta " + user.email + " actualizada");
        return AuthService.view(user);
    }

    private void apply(Usuario user, UsuarioInput input, String email) {
        if (input.rol() == RolCodigo.SOLICITANTE)
            throw ApiException.badRequest("El perfil SOLICITANTE fue reemplazado por ESTUDIANTE y DOCENTE");
        user.nombre = input.nombre().trim();
        user.email = email;
        user.rol = roles.findByCodigo(input.rol()).orElseThrow(() -> ApiException.badRequest("Rol inválido"));
        if (input.activo() != null) user.activo = input.activo();
    }
}
