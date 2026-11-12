package com.coletaseletiva.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "item_solicitacao")
public class ItemSolicitacao extends Cadastro {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Solicitacao solicitacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Material material;

    @Enumerated(EnumType.STRING)
    private UnidadeMedida unidade;

    private BigDecimal quantidadeEstimada;

    private BigDecimal quantidadeEfetiva;

    protected ItemSolicitacao() {}

    public ItemSolicitacao(Solicitacao solicitacao, Material material, BigDecimal quantidadeEstimada) {
        this.solicitacao = solicitacao;
        this.material = material;
        this.unidade = material.getUnidadeMedida();
        this.quantidadeEstimada = quantidadeEstimada;
    }

    public void concluir(BigDecimal quantidadeEfetiva) {
        this.quantidadeEfetiva = quantidadeEfetiva;
    }

    public Solicitacao getSolicitacao() {
        return solicitacao;
    }

    public Material getMaterial() {
        return material;
    }

    public UnidadeMedida getUnidade() {
        return unidade;
    }

    public BigDecimal getQuantidadeEstimada() {
        return quantidadeEstimada;
    }

    public BigDecimal getQuantidadeEfetiva() {
        return quantidadeEfetiva;
    }
}
