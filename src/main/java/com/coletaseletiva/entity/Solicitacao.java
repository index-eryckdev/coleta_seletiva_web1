package com.coletaseletiva.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "solicitacao")
public class Solicitacao extends Cadastro {
    private String protocolo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Morador morador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PontoColeta ponto;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "logradouro", column = @Column(name = "endereco_logradouro")),
        @AttributeOverride(name = "numero", column = @Column(name = "endereco_numero")),
        @AttributeOverride(name = "bairro", column = @Column(name = "endereco_bairro")),
        @AttributeOverride(name = "municipio", column = @Column(name = "endereco_municipio")),
        @AttributeOverride(name = "uf", column = @Column(name = "endereco_uf")),
        @AttributeOverride(name = "cep", column = @Column(name = "endereco_cep")),
        @AttributeOverride(name = "complemento", column = @Column(name = "endereco_complemento"))
    })
    private Localizacao endereco;

    @Enumerated(EnumType.STRING)
    private Situacao situacao;

    private OffsetDateTime dataCriacao;

    private OffsetDateTime dataAgendamento;

    private OffsetDateTime dataConclusao;

    private String observacoes;

    @Version
    private long versao;

    private OffsetDateTime dataAtualizacao;

    private int tentativa;

    protected Solicitacao() {}

    public Solicitacao(String protocolo, Morador morador, PontoColeta ponto, Localizacao endereco,
                       String observacoes, OffsetDateTime agora) {
        this.protocolo = protocolo;
        this.morador = morador;
        this.ponto = ponto;
        this.situacao = Situacao.SOLICITADA;
        this.dataCriacao = agora;
        corrigir(endereco, observacoes, agora);
    }

    public void corrigir(Localizacao endereco, String observacoes, OffsetDateTime agora) {
        this.endereco = endereco;
        this.observacoes = observacoes;
        this.dataAtualizacao = agora;
    }

    public void registrar(Situacao nova, OffsetDateTime agendamento, OffsetDateTime agora) {
        this.situacao = nova;
        this.dataAtualizacao = agora;
        if (agendamento != null) {
            this.dataAgendamento = agendamento;
        }
        if (nova == Situacao.EM_RECOLHIMENTO) {
            this.tentativa++;
        }
        if (nova == Situacao.CONCLUIDA) {
            this.dataConclusao = agora;
        }
    }

    public String getProtocolo() {
        return protocolo;
    }

    public Morador getMorador() {
        return morador;
    }

    public PontoColeta getPonto() {
        return ponto;
    }

    public Localizacao getEndereco() {
        return endereco;
    }

    public Situacao getSituacao() {
        return situacao;
    }

    public OffsetDateTime getDataCriacao() {
        return dataCriacao;
    }

    public OffsetDateTime getDataAgendamento() {
        return dataAgendamento;
    }

    public OffsetDateTime getDataConclusao() {
        return dataConclusao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public long getVersao() {
        return versao;
    }

    public OffsetDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public int getTentativa() {
        return tentativa;
    }
}
