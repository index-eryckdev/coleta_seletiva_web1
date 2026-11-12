package com.coletaseletiva.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "material")
public class Material extends Cadastro {
    private String nome;

    private String descricao;

    @Enumerated(EnumType.STRING)
    private UnidadeMedida unidadeMedida;

    private String orientacaoPreparo;

    private boolean ativo;

    protected Material() {}

    public Material(String nome, String descricao, UnidadeMedida unidadeMedida, String orientacaoPreparo) {
        atualizar(nome, descricao, unidadeMedida, orientacaoPreparo);
        this.ativo = true;
    }

    public void atualizar(String nome, String descricao, UnidadeMedida unidadeMedida, String orientacaoPreparo) {
        this.nome = nome;
        this.descricao = descricao;
        this.unidadeMedida = unidadeMedida;
        this.orientacaoPreparo = orientacaoPreparo;
    }

    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public UnidadeMedida getUnidadeMedida() {
        return unidadeMedida;
    }

    public String getOrientacaoPreparo() {
        return orientacaoPreparo;
    }

    public boolean getAtivo() {
        return ativo;
    }
}
