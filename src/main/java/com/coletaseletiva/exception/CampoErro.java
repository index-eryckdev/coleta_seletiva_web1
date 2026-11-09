package com.coletaseletiva.exception;

/** Erro associado a um campo específico da requisição. */
public record CampoErro(String campo, String mensagem) {
}
