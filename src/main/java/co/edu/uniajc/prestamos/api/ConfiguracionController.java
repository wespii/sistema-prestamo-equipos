package co.edu.uniajc.prestamos.api;

import co.edu.uniajc.prestamos.api.ApiDtos.ConfiguracionPrestamosView;
import co.edu.uniajc.prestamos.service.PoliticaPrestamo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfiguracionController {
    private final PoliticaPrestamo politica;

    public ConfiguracionController(PoliticaPrestamo politica) {
        this.politica = politica;
    }

    @GetMapping("/api/configuracion/prestamos")
    public ConfiguracionPrestamosView prestamos() {
        return new ConfiguracionPrestamosView(politica.maxDiasEstudiante(), politica.maxDiasDocente());
    }
}
