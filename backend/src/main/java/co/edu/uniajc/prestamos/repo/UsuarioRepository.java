package co.edu.uniajc.prestamos.repo;

import co.edu.uniajc.prestamos.model.Usuario;
import co.edu.uniajc.prestamos.model.Rol;
import co.edu.uniajc.prestamos.model.Estados.RolCodigo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByRolCodigoAndActivo(RolCodigo codigo, boolean activo);
    List<Usuario> findByRol(Rol rol);
}
