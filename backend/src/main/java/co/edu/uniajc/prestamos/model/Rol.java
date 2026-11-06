package co.edu.uniajc.prestamos.model;

import co.edu.uniajc.prestamos.model.Estados.RolCodigo;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "roles")
public class Rol {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, unique = true, length = 30)
    public RolCodigo codigo;
    @Column(nullable = false, length = 80)
    public String nombre;

    public Rol() {}
    public Rol(RolCodigo codigo, String nombre) { this.codigo = codigo; this.nombre = nombre; }
}
