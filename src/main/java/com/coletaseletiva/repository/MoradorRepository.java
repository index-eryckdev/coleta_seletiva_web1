package com.coletaseletiva.repository;

import java.util.Optional;
import com.coletaseletiva.entity.Morador;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

public interface MoradorRepository extends JpaRepository<Morador, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, long id);
    boolean existsByIdAndAtivoTrue(long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Morador m where m.id = :id")
    Optional<Morador> bloquear(long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select m from Morador m where m.id = :id and m.ativo = true")
    Optional<Morador> lerAtivoBloqueado(long id);
}
