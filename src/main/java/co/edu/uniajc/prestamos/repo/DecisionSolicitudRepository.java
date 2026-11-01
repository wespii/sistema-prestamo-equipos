package co.edu.uniajc.prestamos.repo;

import co.edu.uniajc.prestamos.model.DecisionSolicitud;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionSolicitudRepository extends JpaRepository<DecisionSolicitud, Long> {
    Optional<DecisionSolicitud> findBySolicitudId(Long id);
}
