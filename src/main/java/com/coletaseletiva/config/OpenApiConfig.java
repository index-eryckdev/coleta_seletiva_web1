package com.coletaseletiva.config;

import java.util.Arrays;
import java.util.List;

import com.coletaseletiva.service.ContextoAtor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI coletaSeletivaOpenApi() {
        var campo = new ObjectSchema().addProperty("campo", new StringSchema()).addProperty("mensagem", new StringSchema());
        var erro = new ObjectSchema()
                .addProperty("timestamp", new StringSchema().format("date-time"))
                .addProperty("status", new IntegerSchema())
                .addProperty("codigo", new StringSchema())
                .addProperty("mensagem", new StringSchema())
                .addProperty("caminho", new StringSchema())
                .addProperty("erros", new ArraySchema().items(campo));
        return new OpenAPI().info(new Info()
                .title("API do Sistema de Coleta Seletiva")
                .description("Projeto de Programação Web I. Os cabeçalhos de ator servem para demonstração local.")
                .version("1.0"))
                .components(new Components().addSchemas("ErroApi", erro));
    }

    @Bean
    public OperationCustomizer documentarContextoEErros() {
        return (operacao, metodo) -> {
            boolean usaAtor = Arrays.asList(metodo.getMethod().getParameterTypes()).contains(ContextoAtor.class);
            if (usaAtor) {
                operacao.addParametersItem(new Parameter().name("X-Ator-Tipo").in("header").required(true)
                        .schema(new StringSchema()._enum(List.of("MORADOR", "OPERADOR", "ADMINISTRADOR")))
                        .description("Perfil fictício selecionado para a demonstração."));
                operacao.addParametersItem(new Parameter().name("X-Ator-Id").in("header")
                        .schema(new IntegerSchema().format("int64")).example(1)
                        .description("ID do morador ou operador. Omita para administrador."));
                operacao.getResponses().addApiResponse("403", erro("Perfil sem acesso a esta operação"));
            }
            operacao.getResponses().addApiResponse("400", erro("Dados ou parâmetros inválidos"));
            operacao.getResponses().addApiResponse("404", erro("Recurso ausente"));
            operacao.getResponses().addApiResponse("409", erro("Conflito de regra, integridade ou versão"));
            return operacao;
        };
    }

    private ApiResponse erro(String descricao) {
        return new ApiResponse().description(descricao).content(new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErroApi"))));
    }
}
