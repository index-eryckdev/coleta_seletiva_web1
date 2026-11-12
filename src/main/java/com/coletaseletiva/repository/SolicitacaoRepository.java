package com.coletaseletiva.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import com.coletaseletiva.entity.Situacao;
import com.coletaseletiva.entity.Solicitacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long>, JpaSpecificationExecutor<Solicitacao> {
    @Override
    @EntityGraph(attributePaths = "ponto")
    Page<Solicitacao> findAll(Specification<Solicitacao> filtro, Pageable pageable);

    @Query(value = "select nextval('protocolo_seq')", nativeQuery = true)
    long proximoProtocolo();

    boolean existsByPontoIdAndSituacaoIn(long pontoId, Collection<Situacao> situacoes);

    @Query("""
            select count(i) > 0 from ItemSolicitacao i join i.solicitacao s
            where s.ponto.id = :pontoId and i.material.id = :materialId and s.situacao in :situacoes
            """)
    boolean existeComMaterial(long pontoId, long materialId, Collection<Situacao> situacoes);

    interface Contagem {
        String getSituacao();
        long getTotal();
    }

    @Query(value = """
            select situacao, count(*) as total from solicitacao
            where (cast(:inicio as timestamptz) is null or data_criacao >= cast(:inicio as timestamptz))
              and (cast(:fim as timestamptz) is null or data_criacao < cast(:fim as timestamptz))
            group by situacao
            """, nativeQuery = true)
    List<Contagem> contarPorSituacao(OffsetDateTime inicio, OffsetDateTime fim);
}
