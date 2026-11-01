package co.edu.uniajc.prestamos.service;

import co.edu.uniajc.prestamos.api.ApiDtos.*;
import co.edu.uniajc.prestamos.api.ApiException;
import co.edu.uniajc.prestamos.model.*;
import co.edu.uniajc.prestamos.model.Estados.*;
import co.edu.uniajc.prestamos.repo.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrestamoService {
    private final SolicitudRepository solicitudes;
    private final EquipoRepository equipos;
    private final DecisionSolicitudRepository decisiones;
    private final PrestamoRepository prestamos;
    private final NovedadRepository novedades;
    private final AuditService audit;
    private final Clock clock;
    private final PoliticaPrestamo politica;
    private final long horasVigencia;

    public PrestamoService(SolicitudRepository solicitudes, EquipoRepository equipos,
                           DecisionSolicitudRepository decisiones, PrestamoRepository prestamos,
                           NovedadRepository novedades, AuditService audit, Clock clock,
                           PoliticaPrestamo politica,
                           @Value("${app.solicitudes.horas-vigencia-aprobacion}") long horasVigencia) {
        this.solicitudes = solicitudes;
        this.equipos = equipos;
        this.decisiones = decisiones;
        this.prestamos = prestamos;
        this.novedades = novedades;
        this.audit = audit;
        this.clock = clock;
        this.politica = politica;
        this.horasVigencia = horasVigencia;
    }

    @Transactional
    public SolicitudView crearSolicitud(SolicitudInput input, Usuario actor) {
        if (!input.fechaFin().isAfter(input.fechaInicio())) throw ApiException.badRequest("La fecha final debe ser posterior a la inicial");
        if (input.fechaInicio().isBefore(LocalDate.now(clock))) throw ApiException.badRequest("La fecha inicial no puede estar en el pasado");
        validarPlazo(actor, input.fechaInicio(), input.fechaFin(), true);
        Equipo e = equipos.findLockedById(input.equipoId()).orElseThrow(() -> ApiException.notFound("Equipo"));
        if (e.estado != EquipoEstado.DISPONIBLE) throw ApiException.conflict("El equipo no está disponible");
        Solicitud s = new Solicitud();
        s.equipo = e;
        s.solicitante = actor;
        s.fechaInicio = input.fechaInicio();
        s.fechaFin = input.fechaFin();
        s.motivo = input.motivo().trim();
        s.fechaSolicitud = LocalDateTime.now(clock);
        s = solicitudes.save(s);
        audit.record(actor, "CREAR", "SOLICITUD", s.id, "Solicitud para equipo " + e.codigo);
        return view(s);
    }

    @Transactional
    public SolicitudView cancelarSolicitud(Long id, Usuario actor) {
        Solicitud s = solicitudes.findLockedById(id).orElseThrow(() -> ApiException.notFound("Solicitud"));
        if (!s.solicitante.id.equals(actor.id)) throw ApiException.forbidden("Solo el solicitante puede cancelar su solicitud");
        if (s.estado != SolicitudEstado.PENDIENTE) throw ApiException.conflict("Solo se puede cancelar una solicitud pendiente");
        s.estado = SolicitudEstado.CANCELADA;
        audit.record(actor, "CANCELAR", "SOLICITUD", s.id, "Solicitud cancelada");
        return view(s);
    }

    @Transactional
    public SolicitudView decidir(Long id, DecisionInput input, Usuario actor) {
        Solicitud s = solicitudes.findLockedById(id).orElseThrow(() -> ApiException.notFound("Solicitud"));
        if (s.estado != SolicitudEstado.PENDIENTE) throw ApiException.conflict("La solicitud ya fue procesada");
        Equipo e = equipos.findLockedById(s.equipo.id).orElseThrow(() -> ApiException.notFound("Equipo"));
        if (input.aprobada()) {
            validarPlazo(s.solicitante, s.fechaInicio, s.fechaFin, false);
            if (e.estado != EquipoEstado.DISPONIBLE || prestamos.existsBySolicitudEquipoIdAndEstado(e.id, PrestamoEstado.ACTIVO))
                throw ApiException.conflict("El equipo ya no está disponible");
            if (s.fechaFin.isBefore(LocalDate.now(clock))) throw ApiException.conflict("El período solicitado ya finalizó");
            s.estado = SolicitudEstado.APROBADA;
            e.estado = EquipoEstado.RESERVADO;
            audit.record(actor, "RESERVAR", "EQUIPO", e.id, "Equipo reservado por solicitud " + s.id);
        } else {
            s.estado = SolicitudEstado.RECHAZADA;
        }
        DecisionSolicitud d = new DecisionSolicitud();
        d.solicitud = s;
        d.encargado = actor;
        d.aprobada = input.aprobada();
        d.observacion = input.observacion() == null ? "" : input.observacion().trim();
        d.fecha = LocalDateTime.now(clock);
        decisiones.save(d);
        s.decision = d;
        audit.record(actor, input.aprobada() ? "APROBAR" : "RECHAZAR", "SOLICITUD", s.id,
            "Decisión para equipo " + e.codigo + (d.observacion.isBlank() ? "" : ": " + d.observacion));
        return view(s);
    }

    @Transactional
    public PrestamoView entregar(Long solicitudId, Usuario actor) {
        Solicitud s = solicitudes.findLockedById(solicitudId).orElseThrow(() -> ApiException.notFound("Solicitud"));
        if (s.estado != SolicitudEstado.APROBADA) throw ApiException.conflict("La entrega requiere una solicitud aprobada");
        DecisionSolicitud decision = decisiones.findBySolicitudId(s.id).orElseThrow(() -> ApiException.conflict("La solicitud no tiene decisión"));
        if (decision.fecha.plusHours(horasVigencia).isBefore(LocalDateTime.now(clock)))
            throw ApiException.conflict("La aprobación venció; espere la actualización del estado");
        validarPlazo(s.solicitante, s.fechaInicio, s.fechaFin, false);
        Equipo e = equipos.findLockedById(s.equipo.id).orElseThrow(() -> ApiException.notFound("Equipo"));
        if (e.estado != EquipoEstado.RESERVADO || prestamos.existsBySolicitudEquipoIdAndEstado(e.id, PrestamoEstado.ACTIVO))
            throw ApiException.conflict("El equipo no se puede entregar en su estado actual");
        if (s.fechaFin.isBefore(LocalDate.now(clock))) throw ApiException.conflict("La fecha de devolución esperada ya pasó");
        Prestamo p = new Prestamo();
        p.solicitud = s;
        p.fechaEntrega = LocalDateTime.now(clock);
        p.fechaDevolucionEsperada = s.fechaFin;
        p.encargadoEntrega = actor;
        p = prestamos.save(p);
        s.prestamo = p;
        e.estado = EquipoEstado.PRESTADO;
        audit.record(actor, "ENTREGAR", "PRESTAMO", p.id, "Entrega de solicitud " + s.id);
        audit.record(actor, "PRESTAR", "EQUIPO", e.id, "Equipo prestado en préstamo " + p.id);
        return view(p);
    }

    @Transactional
    public PrestamoView devolver(Long id, DevolucionInput input, Usuario actor) {
        Prestamo p = prestamos.findLockedById(id).orElseThrow(() -> ApiException.notFound("Préstamo"));
        if (p.estado != PrestamoEstado.ACTIVO) throw ApiException.conflict("El préstamo ya fue devuelto");
        Equipo e = equipos.findLockedById(p.solicitud.equipo.id).orElseThrow(() -> ApiException.notFound("Equipo"));
        if (e.estado != EquipoEstado.PRESTADO) throw ApiException.conflict("El equipo no figura como prestado");
        String observacion = input.observacion() == null ? "" : input.observacion().trim();
        if (input.condicion() != CondicionDevolucion.BUENO && observacion.isBlank())
            throw ApiException.badRequest("Describa la novedad o el daño");
        p.fechaDevolucionReal = LocalDateTime.now(clock);
        p.encargadoDevolucion = actor;
        p.observacionDevolucion = observacion;
        if (input.condicion() == CondicionDevolucion.BUENO) {
            p.estado = PrestamoEstado.DEVUELTO;
            e.estado = EquipoEstado.DISPONIBLE;
        } else {
            p.estado = PrestamoEstado.DEVUELTO_CON_NOVEDAD;
            e.estado = input.condicion() == CondicionDevolucion.DANADO ? EquipoEstado.MANTENIMIENTO : EquipoEstado.DISPONIBLE;
            Novedad n = new Novedad();
            n.prestamo = p;
            n.tipo = input.condicion().name();
            n.descripcion = observacion;
            n.fecha = p.fechaDevolucionReal;
            n.usuarioRegistra = actor;
            novedades.save(n);
            audit.record(actor, "REGISTRAR_NOVEDAD", "PRESTAMO", p.id, n.tipo + ": " + observacion);
        }
        audit.record(actor, "DEVOLVER", "PRESTAMO", p.id, "Estado final " + p.estado);
        audit.record(actor, "CAMBIAR_ESTADO", "EQUIPO", e.id, "Equipo ahora " + e.estado + "; préstamo " + p.id);
        return view(p);
    }

    @Transactional
    public List<SolicitudView> misSolicitudes(Long usuarioId) {
        caducarAprobadas();
        return solicitudes.findBySolicitanteIdOrderByFechaSolicitudDesc(usuarioId).stream().map(this::view).toList();
    }

    @Transactional
    public SolicitudView solicitud(Long id, Usuario actor) {
        caducarAprobadas();
        Solicitud s = solicitudes.findById(id).orElseThrow(() -> ApiException.notFound("Solicitud"));
        if (actor.rol.codigo != RolCodigo.ADMINISTRADOR && actor.rol.codigo != RolCodigo.ENCARGADO
                && !s.solicitante.id.equals(actor.id))
            throw ApiException.forbidden("Solo puede consultar sus propias solicitudes");
        return view(s);
    }

    @Transactional
    public List<SolicitudView> solicitudes(SolicitudEstado estado, Long usuarioId, Long equipoId) {
        caducarAprobadas();
        List<Solicitud> records = estado == null ? solicitudes.findAll() : solicitudes.findByEstado(estado);
        return records.stream()
            .filter(s -> usuarioId == null || s.solicitante.id.equals(usuarioId))
            .filter(s -> equipoId == null || s.equipo.id.equals(equipoId))
            .sorted((a, b) -> b.fechaSolicitud.compareTo(a.fechaSolicitud)).map(this::view).toList();
    }

    @Transactional
    public List<SolicitudView> pendientes() {
        return solicitudes.findByEstadoOrderByFechaSolicitudAsc(SolicitudEstado.PENDIENTE).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<PrestamoView> misPrestamos(Long usuarioId) {
        return prestamos.findBySolicitudSolicitanteIdOrderByFechaEntregaDesc(usuarioId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<PrestamoView> prestamos(PrestamoEstado estado, Long usuarioId, Long equipoId) {
        return prestamos.findAllByOrderByFechaEntregaDesc().stream()
            .filter(p -> estado == null || p.estado == estado)
            .filter(p -> usuarioId == null || p.solicitud.solicitante.id.equals(usuarioId))
            .filter(p -> equipoId == null || p.solicitud.equipo.id.equals(equipoId))
            .map(this::view).toList();
    }

    @Transactional
    public ReporteView reporte() {
        caducarAprobadas();
        EnumMap<EquipoEstado, Long> porEstado = new EnumMap<>(EquipoEstado.class);
        Arrays.stream(EquipoEstado.values()).forEach(e -> porEstado.put(e, 0L));
        equipos.findAll().forEach(e -> porEstado.merge(e.estado, 1L, Long::sum));
        return new ReporteView(porEstado,
            prestamos.countByEstado(PrestamoEstado.ACTIVO),
            prestamos.countByEstadoAndFechaDevolucionEsperadaBefore(PrestamoEstado.ACTIVO, LocalDate.now(clock)),
            solicitudes.countByEstado(SolicitudEstado.PENDIENTE));
    }

    @Scheduled(fixedDelay = 900000)
    @Transactional
    public void caducarAprobadas() {
        LocalDateTime limite = LocalDateTime.now(clock).minusHours(horasVigencia);
        for (Long id : solicitudes.findIdsByEstado(SolicitudEstado.APROBADA)) {
            Solicitud s = solicitudes.findLockedById(id).orElse(null);
            if (s == null || s.estado != SolicitudEstado.APROBADA || prestamos.existsBySolicitudId(s.id)) continue;
            DecisionSolicitud d = decisiones.findBySolicitudId(s.id).orElse(null);
            if (d == null || d.fecha.isAfter(limite)) continue;
            Equipo e = equipos.findLockedById(s.equipo.id).orElse(null);
            if (e == null || s.estado != SolicitudEstado.APROBADA) continue;
            s.estado = SolicitudEstado.VENCIDA;
            if (e.estado == EquipoEstado.RESERVADO) {
                e.estado = EquipoEstado.DISPONIBLE;
                audit.record(null, "LIBERAR", "EQUIPO", e.id, "Aprobación " + s.id + " vencida");
            }
            audit.record(null, "VENCER", "SOLICITUD", s.id, "Aprobación no materializada en " + horasVigencia + " horas");
        }
    }

    private SolicitudView view(Solicitud s) {
        DecisionSolicitud d = decisiones.findBySolicitudId(s.id).orElse(null);
        return new SolicitudView(s.id, s.equipo.id, s.equipo.codigo, s.equipo.nombre,
            s.solicitante.id, s.solicitante.nombre, s.fechaInicio, s.fechaFin, s.motivo,
            s.estado, s.fechaSolicitud, d == null ? null : d.fecha,
            d == null ? null : d.encargado.nombre, d == null ? null : d.observacion);
    }

    private void validarPlazo(Usuario solicitante, LocalDate inicio, LocalDate fin, boolean nueva) {
        if (!solicitante.activo && !nueva)
            throw ApiException.conflict("El solicitante ya no tiene una cuenta activa");
        RolCodigo rol = solicitante.rol.codigo;
        if (rol != RolCodigo.ESTUDIANTE && rol != RolCodigo.DOCENTE && rol != RolCodigo.SOLICITANTE) {
            if (nueva) throw ApiException.forbidden("El perfil no puede solicitar préstamos");
            throw ApiException.conflict("El titular ya no tiene perfil de estudiante o docente");
        }
        int maximo = politica.maxDias(rol);
        long dias = ChronoUnit.DAYS.between(inicio, fin);
        if (dias > maximo) {
            String perfil = rol == RolCodigo.DOCENTE ? "docente" : "estudiante";
            String mensaje = "El perfil " + perfil + " permite un máximo de " + maximo + " días de préstamo";
            if (nueva) throw ApiException.badRequest(mensaje);
            throw ApiException.conflict(mensaje + "; revise el rol actual antes de aprobar o entregar");
        }
    }

    private PrestamoView view(Prestamo p) {
        List<NovedadView> ns = novedades.findByPrestamoIdOrderByFechaAsc(p.id).stream()
            .map(n -> new NovedadView(n.id, n.tipo, n.descripcion, n.fecha, n.usuarioRegistra.nombre)).toList();
        return new PrestamoView(p.id, p.solicitud.id, p.solicitud.equipo.id, p.solicitud.equipo.nombre,
            p.solicitud.solicitante.nombre, p.fechaEntrega, p.fechaDevolucionEsperada,
            p.fechaDevolucionReal, p.observacionDevolucion, p.estado,
            p.estado == PrestamoEstado.ACTIVO && p.fechaDevolucionEsperada.isBefore(LocalDate.now(clock)),
            p.encargadoEntrega.nombre, p.encargadoDevolucion == null ? null : p.encargadoDevolucion.nombre, ns);
    }
}
