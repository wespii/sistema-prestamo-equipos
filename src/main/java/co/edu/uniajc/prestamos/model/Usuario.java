package co.edu.uniajc.prestamos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 120)
    public String nombre;
    @Column(nullable = false, unique = true, length = 180)
    public String email;
    @Column(name = "password_hash", nullable = false, length = 100)
    public String passwordHash;
    @Column(nullable = false)
    public boolean activo = true;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    public Rol rol;
}
