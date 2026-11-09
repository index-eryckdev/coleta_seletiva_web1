package com.coletaseletiva.exception;

/**
 * Violação de regra de negócio ou conflito de estado
 * (ex.: material incompatível com o ponto, transição inválida, e-mail duplicado).
 * Resulta em HTTP 409.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
