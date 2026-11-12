package com.coletaseletiva.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "endereco")
public class Endereco extends Cadastro {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Morador morador;

    @Embedded
    private Localizacao localizacao;

    private boolean semNumero;

    protected Endereco() {}

    public Endereco(Morador morador, Localizacao localizacao) {
        this.morador = morador;
        atualizar(localizacao);
    }

    public void atualizar(Localizacao localizacao) {
        this.localizacao = localizacao;
        this.semNumero = localizacao.numero() == null;
    }

    public Morador getMorador() {
        return morador;
    }

    public Localizacao getLocalizacao() {
        return localizacao;
    }

    public boolean getSemNumero() {
        return semNumero;
    }
}
