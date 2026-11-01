package co.edu.uniajc.prestamos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "decisiones_solicitud")
public class DecisionSolicitud {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
    public Solicitud solicitud;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "encargado_id", nullable = false)
    public Usuario encargado;
    @Column(nullable = false)
    public boolean aprobada;
    @Column(length = 1000)
    public String observacion;
    @Column(nullable = false)
    public LocalDateTime fecha;
}
