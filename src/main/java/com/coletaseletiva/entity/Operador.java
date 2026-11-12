package com.coletaseletiva.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "operador")
public class Operador extends Cadastro {
    private String nome;

    private String email;

    private boolean ativo;

    @ManyToMany
    @JoinTable(name = "operador_ponto", joinColumns = @JoinColumn(name = "operador_id"),
            inverseJoinColumns = @JoinColumn(name = "ponto_id"))
    private Set<PontoColeta> pontos;

    protected Operador() {}

    public Operador(String nome, String email, boolean ativo) {
        atualizar(nome, email, ativo);
        this.pontos = new HashSet<>();
    }

    public void atualizar(String nome, String email, boolean ativo) {
        this.nome = nome;
        this.email = email;
        this.ativo = ativo;
    }

    public void vincular(PontoColeta ponto) {
        pontos.add(ponto);
    }

    public void desvincular(PontoColeta ponto) {
        pontos.remove(ponto);
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public boolean getAtivo() {
        return ativo;
    }

    public Set<PontoColeta> getPontos() {
        return pontos;
    }
}
