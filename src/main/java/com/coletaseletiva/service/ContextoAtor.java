package com.coletaseletiva.service;

import com.coletaseletiva.entity.TipoAtor;
import com.coletaseletiva.exception.AcessoDemoException;

public record ContextoAtor(TipoAtor tipo, Long id) {
    public void exigir(TipoAtor esperado) {
        if (tipo != esperado || (esperado != TipoAtor.ADMINISTRADOR && (id == null || id <= 0))) {
            throw new AcessoDemoException("Selecione um perfil de demonstração autorizado para esta operação.");
        }
    }

    public void exigirMoradorOuAdministrador(long moradorId) {
        if (tipo != TipoAtor.ADMINISTRADOR) {
            exigir(TipoAtor.MORADOR);
            if (id != moradorId) {
                throw new AcessoDemoException("O perfil selecionado não corresponde a este morador.");
            }
        }
    }
}
