package co.edu.uniajc.prestamos.api;

import co.edu.uniajc.prestamos.api.ApiDtos.AuditoriaView;
import co.edu.uniajc.prestamos.service.AuditService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditoriaController {
    private final AuditService audit;
    public AuditoriaController(AuditService audit) { this.audit = audit; }

    @GetMapping("/api/auditoria")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<AuditoriaView> recent() { return audit.recent(); }
}
