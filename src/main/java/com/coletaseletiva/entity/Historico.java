package com.coletaseletiva.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "historico")
public class Historico extends Cadastro {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Solicitacao solicitacao;

    @Enumerated(EnumType.STRING)
    private Situacao situacaoAnterior;

    @Enumerated(EnumType.STRING)
    private Situacao situacaoNova;

    private OffsetDateTime dataHora;

    @Enumerated(EnumType.STRING)
    private TipoAtor responsavelTipo;

    private Long responsavelId;

    private String motivo;

    private Integer numeroTentativa;

    private String detalhesTentativa;

    private OffsetDateTime dataAgendamento;

    protected Historico() {}

    public Historico(Solicitacao solicitacao, Situacao anterior, TipoAtor tipo, Long responsavelId,
                     String motivo, Integer tentativa, String detalhes, OffsetDateTime agendamento) {
        this.solicitacao = solicitacao;
        this.situacaoAnterior = anterior;
        this.situacaoNova = solicitacao.getSituacao();
        this.dataHora = solicitacao.getDataAtualizacao();
        this.responsavelTipo = tipo;
        this.responsavelId = responsavelId;
        this.motivo = motivo;
        this.numeroTentativa = tentativa;
        this.detalhesTentativa = detalhes;
        this.dataAgendamento = agendamento;
    }

    public Solicitacao getSolicitacao() {
        return solicitacao;
    }

    public Situacao getSituacaoAnterior() {
        return situacaoAnterior;
    }

    public Situacao getSituacaoNova() {
        return situacaoNova;
    }

    public OffsetDateTime getDataHora() {
        return dataHora;
    }

    public TipoAtor getResponsavelTipo() {
        return responsavelTipo;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public String getMotivo() {
        return motivo;
    }

    public Integer getNumeroTentativa() {
        return numeroTentativa;
    }

    public String getDetalhesTentativa() {
        return detalhesTentativa;
    }

    public OffsetDateTime getDataAgendamento() {
        return dataAgendamento;
    }
}
