package com.coletaseletiva.service;

import com.coletaseletiva.dto.CadastroDtos.*;
import com.coletaseletiva.entity.*;
import com.coletaseletiva.exception.EntradaInvalidaException;
import java.util.Locale;

final class CadastroSaidas {
    private CadastroSaidas() {}

    static MaterialSaida material(Material m) {
        return new MaterialSaida(m.getId(), m.getNome(), m.getDescricao(), m.getUnidadeMedida(),
                m.getOrientacaoPreparo(), m.getAtivo());
    }

    static EnderecoSaida endereco(Long id, Localizacao local) {
        return new EnderecoSaida(id, local.logradouro(), local.numero(), local.numero() == null,
                local.bairro(), local.municipio(), local.uf(), local.cep(), local.complemento());
    }

    static Localizacao localizacao(EnderecoEntrada entrada, String prefixo) {
        String numero = opcional(entrada.numero());
        if (entrada.semNumero() == (numero != null)) {
            throw new EntradaInvalidaException(prefixo + "numero",
                    "Informe o número ou marque a opção sem número.");
        }
        return new Localizacao(entrada.logradouro().strip(), numero, entrada.bairro().strip(),
                entrada.municipio().strip(), entrada.uf().toUpperCase(Locale.ROOT),
                entrada.cep().replace("-", ""), opcional(entrada.complemento()));
    }

    static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    static String trecho(String texto) {
        return "%" + texto.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
