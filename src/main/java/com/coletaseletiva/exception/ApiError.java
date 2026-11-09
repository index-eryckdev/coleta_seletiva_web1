package com.coletaseletiva.exception;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Formato padrão de resposta de erro da API (RNF03).
 * Nunca contém stack trace.
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String codigo,
        String mensagem,
        String caminho,
        List<CampoErro> erros) {
}
