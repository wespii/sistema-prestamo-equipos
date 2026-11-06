package co.edu.uniajc.prestamos.repo;

import co.edu.uniajc.prestamos.model.Solicitud;
import co.edu.uniajc.prestamos.model.Estados.SolicitudEstado;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findBySolicitanteIdOrderByFechaSolicitudDesc(Long id);
    List<Solicitud> findByEstadoOrderByFechaSolicitudAsc(SolicitudEstado estado);
    Optional<Solicitud> findFirstByEquipoIdAndEstadoOrderByFechaFinDesc(Long equipoId, SolicitudEstado estado);
    List<Solicitud> findByEstado(SolicitudEstado estado);
    @Query("select s.id from Solicitud s where s.estado = :estado")
    List<Long> findIdsByEstado(@Param("estado") SolicitudEstado estado);
    List<Solicitud> findByEquipoIdOrderByFechaSolicitudDesc(Long id);
    long countByEstado(SolicitudEstado estado);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Solicitud s where s.id = :id")
    Optional<Solicitud> findLockedById(@Param("id") Long id);
}
