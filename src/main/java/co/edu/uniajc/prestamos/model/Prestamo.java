package co.edu.uniajc.prestamos.model;

import co.edu.uniajc.prestamos.model.Estados.PrestamoEstado;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prestamos")
public class Prestamo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
    public Solicitud solicitud;
    @Column(nullable = false)
    public LocalDateTime fechaEntrega;
    @Column(nullable = false)
    public LocalDate fechaDevolucionEsperada;
    public LocalDateTime fechaDevolucionReal;
    @Column(length = 1000)
    public String observacionDevolucion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    public PrestamoEstado estado = PrestamoEstado.ACTIVO;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "encargado_entrega_id", nullable = false)
    public Usuario encargadoEntrega;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "encargado_devolucion_id")
    public Usuario encargadoDevolucion;
}
