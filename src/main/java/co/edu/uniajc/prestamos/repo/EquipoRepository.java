package co.edu.uniajc.prestamos.repo;

import co.edu.uniajc.prestamos.model.Equipo;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipoRepository extends JpaRepository<Equipo, Long>, JpaSpecificationExecutor<Equipo> {
    boolean existsByCodigoIgnoreCase(String codigo);
    Optional<Equipo> findByCodigoIgnoreCase(String codigo);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipo e where e.id = :id")
    Optional<Equipo> findLockedById(@Param("id") Long id);
}
