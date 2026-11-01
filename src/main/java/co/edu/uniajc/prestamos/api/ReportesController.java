package co.edu.uniajc.prestamos.api;

import co.edu.uniajc.prestamos.api.ApiDtos.ReporteView;
import co.edu.uniajc.prestamos.service.PrestamoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportesController {
    private final PrestamoService service;
    public ReportesController(PrestamoService service) { this.service = service; }

    @GetMapping("/api/reportes/resumen")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ReporteView resumen() { return service.reporte(); }
}
