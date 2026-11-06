package co.edu.uniajc.prestamos.api;

import co.edu.uniajc.prestamos.api.ApiDtos.*;
import co.edu.uniajc.prestamos.model.Estados.SolicitudEstado;
import co.edu.uniajc.prestamos.security.AuthService;
import co.edu.uniajc.prestamos.service.PrestamoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {
    private final PrestamoService service;
    private final AuthService auth;

    public SolicitudController(PrestamoService service, AuthService auth) {
        this.service = service;
        this.auth = auth;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DOCENTE')")
    public ResponseEntity<SolicitudView> crear(@Valid @RequestBody SolicitudInput input) {
        SolicitudView result = service.crearSolicitud(input, auth.currentUser());
        return ResponseEntity.created(URI.create("/api/solicitudes/" + result.id())).body(result);
    }

    @GetMapping("/mias")
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DOCENTE')")
    public List<SolicitudView> mias() { return service.misSolicitudes(auth.currentUser().id); }

    @GetMapping("/{id}")
    public SolicitudView obtener(@PathVariable Long id) { return service.solicitud(id, auth.currentUser()); }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DOCENTE')")
    public SolicitudView cancelar(@PathVariable Long id) { return service.cancelarSolicitud(id, auth.currentUser()); }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO')")
    public List<SolicitudView> todas(@RequestParam(required = false) SolicitudEstado estado,
                                    @RequestParam(required = false) Long usuarioId,
                                    @RequestParam(required = false) Long equipoId) {
        return service.solicitudes(estado, usuarioId, equipoId);
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO')")
    public List<SolicitudView> pendientes() { return service.pendientes(); }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('ENCARGADO')")
    public SolicitudView decidir(@PathVariable Long id, @Valid @RequestBody DecisionInput input) {
        return service.decidir(id, input, auth.currentUser());
    }

    @PostMapping("/{id}/entregar")
    @PreAuthorize("hasRole('ENCARGADO')")
    public PrestamoView entregar(@PathVariable Long id) { return service.entregar(id, auth.currentUser()); }
}
