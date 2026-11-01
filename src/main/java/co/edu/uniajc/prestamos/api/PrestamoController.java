package co.edu.uniajc.prestamos.api;

import co.edu.uniajc.prestamos.api.ApiDtos.*;
import co.edu.uniajc.prestamos.model.Estados.PrestamoEstado;
import co.edu.uniajc.prestamos.security.AuthService;
import co.edu.uniajc.prestamos.service.PrestamoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {
    private final PrestamoService service;
    private final AuthService auth;

    public PrestamoController(PrestamoService service, AuthService auth) {
        this.service = service;
        this.auth = auth;
    }

    @GetMapping("/mios")
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DOCENTE')")
    public List<PrestamoView> mios() { return service.misPrestamos(auth.currentUser().id); }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO')")
    public List<PrestamoView> todos(@RequestParam(required = false) PrestamoEstado estado,
                                    @RequestParam(required = false) Long usuarioId,
                                    @RequestParam(required = false) Long equipoId) {
        return service.prestamos(estado, usuarioId, equipoId);
    }

    @PostMapping("/{id}/devolver")
    @PreAuthorize("hasRole('ENCARGADO')")
    public PrestamoView devolver(@PathVariable Long id, @Valid @RequestBody DevolucionInput input) {
        return service.devolver(id, input, auth.currentUser());
    }
}
