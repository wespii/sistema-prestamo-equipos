package co.edu.uniajc.prestamos.api;

import co.edu.uniajc.prestamos.api.ApiDtos.*;
import co.edu.uniajc.prestamos.model.Estados.EquipoEstado;
import co.edu.uniajc.prestamos.service.AuditService;
import co.edu.uniajc.prestamos.service.CatalogoService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class CatalogoController {
    private final CatalogoService catalogo;
    private final AuditService audit;

    public CatalogoController(CatalogoService catalogo, AuditService audit) {
        this.catalogo = catalogo;
        this.audit = audit;
    }

    @GetMapping("/api/categorias")
    public List<CategoriaView> categorias() { return catalogo.categorias(false); }

    @GetMapping("/api/equipos")
    public List<EquipoView> equipos(@RequestParam(required = false) String texto,
                                    @RequestParam(required = false) Long categoriaId,
                                    @RequestParam(required = false) EquipoEstado estado) {
        return catalogo.equipos(texto, categoriaId, estado);
    }

    @GetMapping("/api/equipos/{id}")
    public EquipoView equipo(@PathVariable Long id) { return catalogo.equipo(id); }

    @GetMapping("/api/equipos/{id}/historial")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO')")
    public List<AuditoriaView> historial(@PathVariable Long id) {
        catalogo.equipo(id);
        return audit.equipmentHistory(id);
    }
}
