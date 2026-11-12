package com.coletaseletiva.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ponto_coleta")
public class PontoColeta extends Cadastro {
    private String nome;

    @Embedded
    private Localizacao localizacao;

    private String contato;

    private String horarios;

    private boolean ativo;

    private boolean recolhimentoDomiciliar;

    protected PontoColeta() {}

    public PontoColeta(String nome, Localizacao localizacao, String contato, String horarios,
                       boolean ativo, boolean recolhimentoDomiciliar) {
        atualizar(nome, localizacao, contato, horarios, ativo, recolhimentoDomiciliar);
    }

    public void atualizar(String nome, Localizacao localizacao, String contato, String horarios,
                          boolean ativo, boolean recolhimentoDomiciliar) {
        this.nome = nome;
        this.localizacao = localizacao;
        this.contato = contato;
        this.horarios = horarios;
        this.ativo = ativo;
        this.recolhimentoDomiciliar = recolhimentoDomiciliar;
    }

    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getNome() {
        return nome;
    }

    public Localizacao getLocalizacao() {
        return localizacao;
    }

    public String getContato() {
        return contato;
    }

    public String getHorarios() {
        return horarios;
    }

    public boolean getAtivo() {
        return ativo;
    }

    public boolean getRecolhimentoDomiciliar() {
        return recolhimentoDomiciliar;
    }
}
