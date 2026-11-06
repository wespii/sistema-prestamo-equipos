package co.edu.uniajc.prestamos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
public class Auditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 180)
    public String usuario;
    @Column(nullable = false, length = 80)
    public String accion;
    @Column(nullable = false, length = 50)
    public String entidad;
    @Column(nullable = false, length = 60)
    public String referencia;
    @Column(nullable = false)
    public LocalDateTime fecha;
    @Column(length = 1000)
    public String detalle;
}
