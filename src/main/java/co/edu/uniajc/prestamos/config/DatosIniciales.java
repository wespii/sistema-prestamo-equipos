package co.edu.uniajc.prestamos.config;

import co.edu.uniajc.prestamos.model.*;
import co.edu.uniajc.prestamos.model.Estados.*;
import co.edu.uniajc.prestamos.repo.*;
import co.edu.uniajc.prestamos.service.AuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatosIniciales implements ApplicationRunner {
    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final EquipoRepository equipos;
    private final PasswordEncoder passwords;
    private final AuditService audit;
    private final Environment environment;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;

    public DatosIniciales(RolRepository roles, UsuarioRepository usuarios,
                          CategoriaRepository categorias, EquipoRepository equipos,
                          PasswordEncoder passwords, AuditService audit, Environment environment,
                          @Value("${APP_BOOTSTRAP_ADMIN_EMAIL:}") String adminEmail,
                          @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}") String adminPassword,
                          @Value("${APP_BOOTSTRAP_ADMIN_NAME:Administrador inicial}") String adminName) {
        this.roles = roles;
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.equipos = equipos;
        this.passwords = passwords;
        this.audit = audit;
        this.environment = environment;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminName = adminName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RolCodigo codigo : RolCodigo.values()) {
            if (codigo == RolCodigo.SOLICITANTE) continue;
            roles.findByCodigo(codigo).orElseGet(() -> roles.save(new Rol(codigo, nombreRol(codigo))));
        }
        migrarSolicitantesAnteriores();
        if (!environment.acceptsProfiles(Profiles.of("prod"))
                && environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            semillaDemostracion();
        } else {
            bootstrapProduccion();
        }
    }

    private String nombreRol(RolCodigo code) {
        return switch (code) {
            case ADMINISTRADOR -> "Administrador del sistema";
            case ENCARGADO -> "Encargado de préstamos";
            case ESTUDIANTE -> "Estudiante";
            case DOCENTE -> "Docente";
            case SOLICITANTE -> "Solicitante anterior";
        };
    }

    private void migrarSolicitantesAnteriores() {
        Rol anterior = roles.findByCodigo(RolCodigo.SOLICITANTE).orElse(null);
        if (anterior == null) return;
        Rol estudiante = roles.findByCodigo(RolCodigo.ESTUDIANTE).orElseThrow();
        for (Usuario usuario : usuarios.findByRol(anterior)) {
            usuario.rol = estudiante;
            audit.record(null, "MIGRAR_ROL", "USUARIO", usuario.id,
                "Perfil anterior SOLICITANTE convertido a ESTUDIANTE");
        }
    }

    private void bootstrapProduccion() {
        if (usuarios.count() > 0) return;
        if (adminEmail.isBlank() || adminPassword.length() < 12) {
            throw new IllegalStateException("El primer inicio de producción requiere APP_BOOTSTRAP_ADMIN_EMAIL y APP_BOOTSTRAP_ADMIN_PASSWORD (mínimo 12 caracteres)");
        }
        crearUsuario(adminName, adminEmail, adminPassword, RolCodigo.ADMINISTRADOR);
    }

    private void semillaDemostracion() {
        crearUsuarioSiFalta("Administrador Demo", "admin@uniajc.edu.co", "Admin123!", RolCodigo.ADMINISTRADOR);
        crearUsuarioSiFalta("Encargado Demo", "encargado@uniajc.edu.co", "Encargado123!", RolCodigo.ENCARGADO);
        crearUsuarioSiFalta("Estudiante Demo", "estudiante@uniajc.edu.co", "Estudiante123!", RolCodigo.ESTUDIANTE);
        crearUsuarioSiFalta("Docente Demo", "docente@uniajc.edu.co", "Docente123!", RolCodigo.DOCENTE);

        Categoria portatiles = crearCategoriaSiFalta("Portátiles");
        Categoria proyectores = crearCategoriaSiFalta("Proyectores");
        Categoria accesorios = crearCategoriaSiFalta("Accesorios");
        crearEquipoSiFalta("PORT-001", "Portátil Lenovo ThinkPad", "Equipo portátil para actividades académicas", "Intel Core i5 · 16 GB RAM · 512 GB SSD", portatiles, EquipoEstado.DISPONIBLE);
        crearEquipoSiFalta("PORT-002", "Portátil HP ProBook", "Equipo portátil para trabajo en laboratorio", "Intel Core i7 · 16 GB RAM · 512 GB SSD", portatiles, EquipoEstado.DISPONIBLE);
        crearEquipoSiFalta("PROY-001", "Proyector Epson", "Proyector para aulas y presentaciones", "Full HD · HDMI", proyectores, EquipoEstado.DISPONIBLE);
        crearEquipoSiFalta("PROY-002", "Proyector ViewSonic", "Proyector pendiente de revisión técnica", "HDMI · 3.000 lúmenes", proyectores, EquipoEstado.MANTENIMIENTO);
        crearEquipoSiFalta("ACCE-001", "Cámara web Logitech", "Cámara para clases híbridas", "1080p · USB", accesorios, EquipoEstado.DISPONIBLE);
    }

    private void crearUsuarioSiFalta(String nombre, String email, String password, RolCodigo rol) {
        if (!usuarios.existsByEmailIgnoreCase(email)) crearUsuario(nombre, email, password, rol);
    }

    private void crearUsuario(String nombre, String email, String password, RolCodigo rol) {
        Usuario u = new Usuario();
        u.nombre = nombre;
        u.email = email.toLowerCase();
        u.passwordHash = passwords.encode(password);
        u.rol = roles.findByCodigo(rol).orElseThrow();
        usuarios.save(u);
    }

    private Categoria crearCategoriaSiFalta(String nombre) {
        return categorias.findByNombreIgnoreCase(nombre).orElseGet(() -> {
            Categoria c = new Categoria();
            c.nombre = nombre;
            return categorias.save(c);
        });
    }

    private void crearEquipoSiFalta(String codigo, String nombre, String descripcion,
                                    String caracteristicas, Categoria categoria, EquipoEstado estado) {
        if (equipos.existsByCodigoIgnoreCase(codigo)) return;
        Equipo e = new Equipo();
        e.codigo = codigo;
        e.nombre = nombre;
        e.descripcion = descripcion;
        e.caracteristicas = caracteristicas;
        e.categoria = categoria;
        e.estado = estado;
        equipos.save(e);
    }
}
