package co.edu.uniajc.prestamos.repo;

import co.edu.uniajc.prestamos.model.Auditoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findTop200ByOrderByFechaDesc();
    List<Auditoria> findByEntidadAndReferenciaOrderByFechaDesc(String entidad, String referencia);
}
