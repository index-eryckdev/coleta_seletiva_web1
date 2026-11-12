package com.coletaseletiva.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "morador")
public class Morador extends Cadastro {
    private String nome;

    private String email;

    private String telefone;

    private boolean ativo;

    protected Morador() {}

    public Morador(String nome, String email, String telefone) {
        atualizar(nome, email, telefone);
        this.ativo = true;
    }

    public void atualizar(String nome, String email, String telefone) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }

    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public boolean getAtivo() {
        return ativo;
    }
}
