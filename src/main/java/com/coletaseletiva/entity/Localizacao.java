package com.coletaseletiva.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public record Localizacao(String logradouro, String numero, String bairro,
                          String municipio, String uf, String cep, String complemento) {
}
