package co.edu.uniajc.prestamos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categorias_equipo")
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true, length = 100)
    public String nombre;
    @Column(nullable = false)
    public boolean activo = true;
}
