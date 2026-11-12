package com.coletaseletiva.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "material_aceito")
public class MaterialAceito extends Cadastro {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PontoColeta ponto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Material material;

    private String observacoes;

    private BigDecimal limitePorSolicitacao;

    protected MaterialAceito() {}

    public MaterialAceito(PontoColeta ponto, Material material, String observacoes, BigDecimal limite) {
        this.ponto = ponto;
        this.material = material;
        atualizar(observacoes, limite);
    }

    public void atualizar(String observacoes, BigDecimal limite) {
        this.observacoes = observacoes;
        this.limitePorSolicitacao = limite;
    }

    public PontoColeta getPonto() {
        return ponto;
    }

    public Material getMaterial() {
        return material;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public BigDecimal getLimitePorSolicitacao() {
        return limitePorSolicitacao;
    }
}
