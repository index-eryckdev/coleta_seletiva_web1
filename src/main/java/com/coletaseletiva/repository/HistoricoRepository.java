package com.coletaseletiva.repository;

import java.util.List;
import com.coletaseletiva.entity.Historico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoRepository extends JpaRepository<Historico, Long> {
    List<Historico> findBySolicitacaoIdOrderByDataHoraAscIdAsc(long solicitacaoId);
}
