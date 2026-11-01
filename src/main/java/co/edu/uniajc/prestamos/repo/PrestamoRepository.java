package co.edu.uniajc.prestamos.repo;

import co.edu.uniajc.prestamos.model.Prestamo;
import co.edu.uniajc.prestamos.model.Estados.PrestamoEstado;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    List<Prestamo> findBySolicitudSolicitanteIdOrderByFechaEntregaDesc(Long id);
    List<Prestamo> findAllByOrderByFechaEntregaDesc();
    boolean existsBySolicitudEquipoIdAndEstado(Long equipoId, PrestamoEstado estado);
    boolean existsBySolicitudId(Long solicitudId);
    long countByEstado(PrestamoEstado estado);
    long countByEstadoAndFechaDevolucionEsperadaBefore(PrestamoEstado estado, LocalDate fecha);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prestamo p where p.id = :id")
    Optional<Prestamo> findLockedById(@Param("id") Long id);
}
