package com.coletaseletiva.exception;

public class EntradaInvalidaException extends RuntimeException {
    private final String campo;

    public EntradaInvalidaException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
