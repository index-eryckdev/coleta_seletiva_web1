package com.coletaseletiva.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.coletaseletiva.entity.Material;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

public interface MaterialRepository extends JpaRepository<Material, Long>, JpaSpecificationExecutor<Material> {
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Material m where m.id = :id")
    Optional<Material> bloquear(long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select m from Material m where m.id in :ids order by m.id")
    List<Material> lerBloqueados(Collection<Long> ids);
}
