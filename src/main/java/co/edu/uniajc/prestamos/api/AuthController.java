package co.edu.uniajc.prestamos.api;

import co.edu.uniajc.prestamos.api.ApiDtos.*;
import co.edu.uniajc.prestamos.security.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public LoginView login(@Valid @RequestBody LoginInput input) { return auth.login(input); }

    @GetMapping("/me")
    public UsuarioView me() { return AuthService.view(auth.currentUser()); }
}
