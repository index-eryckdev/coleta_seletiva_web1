package com.coletaseletiva.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import com.coletaseletiva.entity.ItemSolicitacao;
import org.springframework.data.jpa.repository.*;

public interface ItemSolicitacaoRepository extends JpaRepository<ItemSolicitacao, Long> {
    @EntityGraph(attributePaths = "material")
    List<ItemSolicitacao> findBySolicitacaoIdOrderById(long solicitacaoId);

    interface Total {
        long getMaterialId();
        String getMaterial();
        String getUnidade();
        BigDecimal getQuantidade();
    }

    @Query(value = """
            select m.id as "materialId", m.nome as material, i.unidade, sum(i.quantidade_efetiva) as quantidade
            from item_solicitacao i join solicitacao s on s.id = i.solicitacao_id
                 join material m on m.id = i.material_id
            where s.situacao = 'CONCLUIDA'
              and (cast(:inicio as timestamptz) is null or s.data_conclusao >= cast(:inicio as timestamptz))
              and (cast(:fim as timestamptz) is null or s.data_conclusao < cast(:fim as timestamptz))
              and i.quantidade_efetiva is not null
            group by m.id, m.nome, i.unidade
            order by m.nome, m.id, i.unidade
            """, nativeQuery = true)
    List<Total> totalRecolhido(OffsetDateTime inicio, OffsetDateTime fim);
}
