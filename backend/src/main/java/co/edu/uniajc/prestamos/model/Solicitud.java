package co.edu.uniajc.prestamos.model;

import co.edu.uniajc.prestamos.model.Estados.SolicitudEstado;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes")
public class Solicitud {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    public Usuario solicitante;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    public Equipo equipo;
    @Column(nullable = false)
    public LocalDate fechaInicio;
    @Column(nullable = false)
    public LocalDate fechaFin;
    @Column(nullable = false, length = 1000)
    public String motivo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    public SolicitudEstado estado = SolicitudEstado.PENDIENTE;
    @Column(nullable = false)
    public LocalDateTime fechaSolicitud;
    @OneToOne(mappedBy = "solicitud", fetch = FetchType.LAZY)
    public DecisionSolicitud decision;
    @OneToOne(mappedBy = "solicitud", fetch = FetchType.LAZY)
    public Prestamo prestamo;
}
