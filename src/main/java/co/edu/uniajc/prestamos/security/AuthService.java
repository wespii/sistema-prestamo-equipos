package co.edu.uniajc.prestamos.security;

import co.edu.uniajc.prestamos.api.ApiDtos.*;
import co.edu.uniajc.prestamos.api.ApiException;
import co.edu.uniajc.prestamos.model.Usuario;
import co.edu.uniajc.prestamos.repo.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;
    private final JwtService jwt;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwords, JwtService jwt) {
        this.usuarios = usuarios;
        this.passwords = passwords;
        this.jwt = jwt;
    }

    public LoginView login(LoginInput input) {
        Usuario user = usuarios.findByEmailIgnoreCase(input.email().trim())
            .orElseThrow(() -> invalidCredentials());
        if (!user.activo || !passwords.matches(input.password(), user.passwordHash)) {
            throw invalidCredentials();
        }
        return new LoginView(jwt.issue(user.id), view(user));
    }

    public Usuario currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarios.findByEmailIgnoreCase(email).filter(u -> u.activo)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", "Sesión inválida"));
    }

    public static UsuarioView view(Usuario user) {
        return new UsuarioView(user.id, user.nombre, user.email, user.rol.codigo, user.activo);
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos");
    }
}
