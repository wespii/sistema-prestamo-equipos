package co.edu.uniajc.prestamos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "novedades")
public class Novedad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false)
    public Prestamo prestamo;
    @Column(nullable = false, length = 30)
    public String tipo;
    @Column(nullable = false, length = 1000)
    public String descripcion;
    @Column(nullable = false)
    public LocalDateTime fecha;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_registra_id", nullable = false)
    public Usuario usuarioRegistra;
}
