package com.coletaseletiva.repository;

import java.util.Optional;
import com.coletaseletiva.entity.PontoColeta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

public interface PontoColetaRepository extends JpaRepository<PontoColeta, Long>, JpaSpecificationExecutor<PontoColeta> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PontoColeta p where p.id = :id")
    Optional<PontoColeta> bloquear(long id);
}
