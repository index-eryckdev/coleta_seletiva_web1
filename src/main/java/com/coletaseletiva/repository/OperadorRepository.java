package com.coletaseletiva.repository;

import java.util.List;
import java.util.Optional;
import com.coletaseletiva.entity.Operador;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

public interface OperadorRepository extends JpaRepository<Operador, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, long id);
    boolean existsByIdAndAtivoTrue(long id);

    @Query("select p.id from Operador o join o.pontos p where o.id = :id and o.ativo = true")
    List<Long> pontosAtivosDoOperador(long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select o from Operador o where o.id = :id and o.ativo = true")
    Optional<Operador> lerAtivoBloqueado(long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Operador o where o.id = :id")
    Optional<Operador> bloquear(long id);
}
