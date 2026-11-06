package co.edu.uniajc.prestamos.service;

import co.edu.uniajc.prestamos.api.ApiException;
import co.edu.uniajc.prestamos.model.Estados.RolCodigo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PoliticaPrestamo {
    private final int maxDiasEstudiante;
    private final int maxDiasDocente;

    public PoliticaPrestamo(@Value("${app.prestamos.max-dias-estudiante}") int maxDiasEstudiante,
                            @Value("${app.prestamos.max-dias-docente}") int maxDiasDocente) {
        if (maxDiasEstudiante < 1 || maxDiasDocente <= maxDiasEstudiante) {
            throw new IllegalArgumentException("El límite docente debe superar el límite estudiante y ambos deben ser positivos");
        }
        this.maxDiasEstudiante = maxDiasEstudiante;
        this.maxDiasDocente = maxDiasDocente;
    }

    public int maxDiasEstudiante() { return maxDiasEstudiante; }
    public int maxDiasDocente() { return maxDiasDocente; }

    public int maxDias(RolCodigo rol) {
        return switch (rol) {
            case ESTUDIANTE, SOLICITANTE -> maxDiasEstudiante;
            case DOCENTE -> maxDiasDocente;
            default -> throw ApiException.forbidden("El perfil no puede solicitar préstamos");
        };
    }
}
