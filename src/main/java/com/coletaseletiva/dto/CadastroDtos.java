package com.coletaseletiva.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import com.coletaseletiva.entity.UnidadeMedida;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public final class CadastroDtos {
    private CadastroDtos() {}

    public record MaterialEntrada(@NotBlank @Size(max = 80) String nome,
                                  @Size(max = 4000) String descricao,
                                  @NotNull UnidadeMedida unidadeMedida,
                                  @Size(max = 4000) String orientacaoPreparo) {}

    public record MaterialSaida(long id, String nome, String descricao, UnidadeMedida unidadeMedida,
                               String orientacaoPreparo, boolean ativo) {}

    public record MoradorEntrada(@NotBlank @Size(max = 120) String nome,
                                 @NotBlank @Email @Size(max = 160) String email,
                                 @Size(max = 20) String telefone) {
        public MoradorEntrada {
            email = normalizarEmail(email);
        }
    }

    public record MoradorSaida(long id, String nome, String email, String telefone, boolean ativo) {}

    public record EnderecoEntrada(@NotBlank @Size(max = 160) String logradouro,
                                  @Size(max = 10) String numero,
                                  @NotNull Boolean semNumero,
                                  @NotBlank @Size(max = 80) String bairro,
                                  @NotBlank @Size(max = 80) String municipio,
                                  @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String uf,
                                  @NotBlank @Pattern(regexp = "[0-9]{5}-?[0-9]{3}") String cep,
                                  @Size(max = 80) String complemento) {}

    public record EnderecoSaida(Long id, String logradouro, String numero, boolean semNumero,
                               String bairro, String municipio, String uf, String cep, String complemento) {}

    public record PontoEntrada(@NotBlank @Size(max = 120) String nome,
                               @NotNull @Valid EnderecoEntrada endereco,
                               @Size(max = 80) String contato,
                               @Size(max = 200) String horarios,
                               @NotNull Boolean ativo,
                               @NotNull Boolean recolhimentoDomiciliar) {}

    public record PontoSaida(long id, String nome, EnderecoSaida endereco, String contato, String horarios,
                            boolean ativo, boolean recolhimentoDomiciliar, List<MaterialAceitoSaida> materiaisAceitos) {}

    public record VinculoEntrada(@NotNull @Positive Long materialId,
                                 @Size(max = 200) String observacoes,
                                 @Positive @Digits(integer = 9, fraction = 3) BigDecimal limitePorSolicitacao) {}

    public record VinculoEdicao(@Size(max = 200) String observacoes,
                                @Positive @Digits(integer = 9, fraction = 3) BigDecimal limitePorSolicitacao) {}

    public record MaterialAceitoSaida(MaterialSaida material, String observacoes, BigDecimal limitePorSolicitacao) {}

    public record OperadorEntrada(@NotBlank @Size(max = 120) String nome,
                                  @NotBlank @Email @Size(max = 160) String email,
                                  @NotNull Boolean ativo) {
        public OperadorEntrada {
            email = normalizarEmail(email);
        }
    }

    public record OperadorSaida(long id, String nome, String email, boolean ativo, List<Long> pontosIds) {}

    private static String normalizarEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
