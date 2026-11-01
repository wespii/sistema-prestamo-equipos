package co.edu.uniajc.prestamos.api;

import co.edu.uniajc.prestamos.model.Estados.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ApiDtos {
    private ApiDtos() {}

    public record LoginInput(@NotBlank @Email String email, @NotBlank String password) {}
    public record LoginView(String token, UsuarioView user) {}
    public record ConfiguracionPrestamosView(int maxDiasEstudiante, int maxDiasDocente) {}
    public record UsuarioInput(@NotBlank @Size(max = 120) String nombre,
                               @NotBlank @Email @Size(max = 180) String email,
                               String password, @NotNull RolCodigo rol, Boolean activo) {}
    public record UsuarioView(Long id, String nombre, String email, RolCodigo rol, boolean activo) {}
    public record CategoriaInput(@NotBlank @Size(max = 100) String nombre, Boolean activo) {}
    public record CategoriaView(Long id, String nombre, boolean activo) {}
    public record EquipoInput(@NotBlank @Size(max = 60) String codigo,
                              @NotBlank @Size(max = 140) String nombre,
                              @NotBlank @Size(max = 1000) String descripcion,
                              @Size(max = 2000) String caracteristicas,
                              EquipoEstado estado, @NotNull Long categoriaId) {}
    public record EquipoView(Long id, String codigo, String nombre, String descripcion,
                             String caracteristicas, EquipoEstado estado, Long categoriaId,
                             String categoriaNombre, boolean disponible) {}
    public record SolicitudInput(@NotNull Long equipoId, @NotNull LocalDate fechaInicio,
                                 @NotNull LocalDate fechaFin,
                                 @NotBlank @Size(max = 1000) String motivo) {}
    public record SolicitudView(Long id, Long equipoId, String equipoCodigo, String equipoNombre,
                                Long solicitanteId, String solicitanteNombre,
                                LocalDate fechaInicio, LocalDate fechaFin, String motivo,
                                SolicitudEstado estado, LocalDateTime fechaSolicitud,
                                LocalDateTime fechaDecision, String encargadoNombre,
                                String observacionDecision) {}
    public record DecisionInput(@NotNull Boolean aprobada, @Size(max = 1000) String observacion) {}
    public record DevolucionInput(@NotNull CondicionDevolucion condicion,
                                  @Size(max = 1000) String observacion) {}
    public record NovedadView(Long id, String tipo, String descripcion,
                              LocalDateTime fecha, String usuarioRegistra) {}
    public record PrestamoView(Long id, Long solicitudId, Long equipoId, String equipoNombre,
                               String solicitanteNombre, LocalDateTime fechaEntrega,
                               LocalDate fechaDevolucionEsperada, LocalDateTime fechaDevolucionReal,
                               String observacionDevolucion, PrestamoEstado estado, boolean vencido,
                               String encargadoEntregaNombre, String encargadoDevolucionNombre,
                               List<NovedadView> novedades) {}
    public record ReporteView(Map<EquipoEstado, Long> equiposPorEstado,
                              long prestamosActivos, long prestamosVencidos,
                              long solicitudesPendientes) {}
    public record AuditoriaView(Long id, String usuario, String accion, String entidad,
                                String referencia, LocalDateTime fecha, String detalle) {}
}
