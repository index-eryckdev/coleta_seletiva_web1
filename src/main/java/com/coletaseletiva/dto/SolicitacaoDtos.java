package com.coletaseletiva.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.coletaseletiva.dto.CadastroDtos.EnderecoSaida;
import com.coletaseletiva.entity.Situacao;
import com.coletaseletiva.entity.TipoAtor;
import com.coletaseletiva.entity.UnidadeMedida;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public final class SolicitacaoDtos {
    private SolicitacaoDtos() {}

    public record ItemEntrada(@NotNull @Positive Long materialId,
                              @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal quantidadeEstimada) {}

    public record Criacao(@NotNull @Positive Long pontoId, @NotNull @Positive Long enderecoId,
                          @Size(max = 4000) String observacoes,
                          @NotEmpty @Size(max = 100) List<@NotNull @Valid ItemEntrada> itens) {}

    public record Correcao(@NotNull @PositiveOrZero Long versao, @NotNull @Positive Long enderecoId,
                           @Size(max = 4000) String observacoes,
                           @NotEmpty @Size(max = 100) List<@NotNull @Valid ItemEntrada> itens) {}

    public record Versao(@NotNull @PositiveOrZero Long versao) {}

    public record Motivo(@NotNull @PositiveOrZero Long versao, @NotBlank @Size(max = 4000) String motivo) {}

    public record Agendamento(@NotNull @PositiveOrZero Long versao,
                              @NotNull @Future OffsetDateTime dataAgendamento, @Size(max = 4000) String motivo) {}

    public record Reagendamento(@NotNull @PositiveOrZero Long versao,
                                @NotNull @Future OffsetDateTime dataAgendamento,
                                @NotBlank @Size(max = 4000) String motivo, @Size(max = 4000) String detalhesTentativa) {}

    public record ItemConclusao(@NotNull @Positive Long materialId,
                                @NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal quantidadeEfetiva) {}

    public record Conclusao(@NotNull @PositiveOrZero Long versao,
                            @NotEmpty @Size(max = 100) List<@NotNull @Valid ItemConclusao> itens) {}

    public record ItemSaida(long materialId, String material, UnidadeMedida unidade,
                           BigDecimal quantidadeEstimada, BigDecimal quantidadeEfetiva) {}

    public record EventoSaida(Situacao situacaoAnterior, Situacao situacaoNova, OffsetDateTime dataHora,
                              TipoAtor responsavelTipo, Long responsavelId, String motivo, Integer numeroTentativa,
                              String detalhesTentativa, OffsetDateTime dataAgendamento) {}

    public record ResumoSolicitacao(long id, String protocolo, long moradorId, long pontoId, String ponto,
                                   Situacao situacao, OffsetDateTime dataCriacao, OffsetDateTime dataAgendamento,
                                   OffsetDateTime dataConclusao, long versao) {}

    public record Detalhe(ResumoSolicitacao solicitacao, EnderecoSaida endereco, String observacoes,
                          List<ItemSaida> itens, List<EventoSaida> historico) {}

    public record TotalMaterial(long materialId, String material, UnidadeMedida unidade, BigDecimal quantidade) {}

    public record Indicadores(Map<Situacao, Long> solicitacoesPorSituacao, List<TotalMaterial> recolhidosPorMaterial) {}
}
