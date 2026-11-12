package com.coletaseletiva.entity;

import java.util.Set;

public enum Situacao {
    SOLICITADA, AGENDADA, EM_RECOLHIMENTO, CONCLUIDA, RECUSADA, CANCELADA;

    public static final Set<Situacao> ABERTAS = Set.of(SOLICITADA, AGENDADA, EM_RECOLHIMENTO);
}
