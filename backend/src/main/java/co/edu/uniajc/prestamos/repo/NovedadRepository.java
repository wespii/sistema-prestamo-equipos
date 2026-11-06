package co.edu.uniajc.prestamos.repo;

import co.edu.uniajc.prestamos.model.Novedad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NovedadRepository extends JpaRepository<Novedad, Long> {
    List<Novedad> findByPrestamoIdOrderByFechaAsc(Long id);
}
