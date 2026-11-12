package com.coletaseletiva.controller;

import java.util.ArrayList;
import java.util.Set;

import com.coletaseletiva.exception.EntradaInvalidaException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class Paginacao {
    private Paginacao() {}

    static Pageable ordenar(Pageable pagina, String padrao, String... campos) {
        var aceitos = Set.of(campos);
        var ordens = new ArrayList<Sort.Order>();
        for (Sort.Order ordem : pagina.getSort()) {
            if (!ordem.getProperty().equals("id") && !aceitos.contains(ordem.getProperty())) {
                throw new EntradaInvalidaException("sort", "Campo de ordenação inválido: " + ordem.getProperty());
            }
            ordens.add(ordem);
        }
        if (ordens.isEmpty() && !padrao.equals("id")) {
            ordens.add(padrao.equals("dataCriacao") ? Sort.Order.desc(padrao) : Sort.Order.asc(padrao));
        }
        if (ordens.stream().noneMatch(o -> o.getProperty().equals("id"))) {
            ordens.add(Sort.Order.asc("id"));
        }
        return PageRequest.of(pagina.getPageNumber(), pagina.getPageSize(), Sort.by(ordens));
    }
}
