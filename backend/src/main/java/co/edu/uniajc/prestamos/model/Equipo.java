package co.edu.uniajc.prestamos.model;

import co.edu.uniajc.prestamos.model.Estados.EquipoEstado;
import jakarta.persistence.*;

@Entity
@Table(name = "equipos")
public class Equipo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Version
    public long version;
    @Column(nullable = false, unique = true, length = 60)
    public String codigo;
    @Column(nullable = false, length = 140)
    public String nombre;
    @Column(nullable = false, length = 1000)
    public String descripcion;
    @Column(length = 2000)
    public String caracteristicas;
    @Basic(fetch = FetchType.LAZY) @Lob
    @Column(name = "imagen")
    public byte[] imagen;
    @Column(name = "imagen_content_type", length = 80)
    public String imagenContentType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    public EquipoEstado estado = EquipoEstado.DISPONIBLE;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    public Categoria categoria;
}
