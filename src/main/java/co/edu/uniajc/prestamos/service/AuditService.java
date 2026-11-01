package co.edu.uniajc.prestamos.service;

import co.edu.uniajc.prestamos.api.ApiDtos.AuditoriaView;
import co.edu.uniajc.prestamos.model.Auditoria;
import co.edu.uniajc.prestamos.model.Usuario;
import co.edu.uniajc.prestamos.repo.AuditoriaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditoriaRepository auditoria;
    private final Clock clock;

    public AuditService(AuditoriaRepository auditoria, Clock clock) {
        this.auditoria = auditoria;
        this.clock = clock;
    }

    public void record(Usuario actor, String accion, String entidad, Object referencia, String detalle) {
        Auditoria event = new Auditoria();
        event.usuario = actor == null ? "SISTEMA" : actor.email;
        event.accion = accion;
        event.entidad = entidad;
        event.referencia = String.valueOf(referencia);
        event.detalle = detalle;
        event.fecha = LocalDateTime.now(clock);
        auditoria.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaView> recent() {
        return auditoria.findTop200ByOrderByFechaDesc().stream().map(AuditService::view).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaView> equipmentHistory(Long id) {
        return auditoria.findByEntidadAndReferenciaOrderByFechaDesc("EQUIPO", id.toString())
            .stream().map(AuditService::view).toList();
    }

    private static AuditoriaView view(Auditoria e) {
        return new AuditoriaView(e.id, e.usuario, e.accion, e.entidad, e.referencia, e.fecha, e.detalle);
    }
}
