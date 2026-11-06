package co.edu.uniajc.prestamos.api;

import co.edu.uniajc.prestamos.api.ApiDtos.ReporteView;
import co.edu.uniajc.prestamos.api.ApiDtos.IndicadoresDetalleView;
import co.edu.uniajc.prestamos.api.ApiDtos.PrestamoView;
import co.edu.uniajc.prestamos.service.CatalogoService;
import co.edu.uniajc.prestamos.service.PrestamoService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportesController {
    private final PrestamoService service;
    private final CatalogoService catalogo;
    public ReportesController(PrestamoService service, CatalogoService catalogo) {
        this.service = service;
        this.catalogo = catalogo;
    }

    @GetMapping("/api/reportes/resumen")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO')")
    public ReporteView resumen() { return service.reporte(); }

    @GetMapping("/api/reportes/detalle")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO')")
    public IndicadoresDetalleView detalle() {
        List<PrestamoView> activos = service.prestamos(
            co.edu.uniajc.prestamos.model.Estados.PrestamoEstado.ACTIVO, null, null);
        List<PrestamoView> porVencer = service.prestamosPorVencer();
        return new IndicadoresDetalleView(
            catalogo.equiposAdministracion(null, null, null),
            service.pendientes(),
            activos,
            porVencer,
            activos.stream().filter(PrestamoView::vencido).toList());
    }
}
