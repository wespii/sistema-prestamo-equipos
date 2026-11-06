package co.edu.uniajc.prestamos.repo;

import co.edu.uniajc.prestamos.model.Rol;
import co.edu.uniajc.prestamos.model.Estados.RolCodigo;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByCodigo(RolCodigo codigo);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rol r where r.codigo = :codigo")
    Optional<Rol> findLockedByCodigo(@Param("codigo") RolCodigo codigo);
}
