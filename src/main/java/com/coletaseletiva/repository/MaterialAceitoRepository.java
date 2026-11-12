package com.coletaseletiva.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.coletaseletiva.entity.MaterialAceito;
import org.springframework.data.jpa.repository.*;

public interface MaterialAceitoRepository extends JpaRepository<MaterialAceito, Long> {
    @EntityGraph(attributePaths = "material")
    List<MaterialAceito> findByPontoIdInOrderByMaterialNomeAscMaterialIdAsc(Collection<Long> pontos);

    Optional<MaterialAceito> findByPontoIdAndMaterialId(long pontoId, long materialId);
    boolean existsByMaterialIdAndLimitePorSolicitacaoIsNotNull(long materialId);
}
