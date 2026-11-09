package com.coletaseletiva.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI coletaSeletivaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API - Sistema de Coleta Seletiva")
                .description("Pontos de coleta, materiais recicláveis e solicitações de recolhimento.")
                .version("0.0.1"));
    }
}
