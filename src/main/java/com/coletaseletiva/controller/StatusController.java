package com.coletaseletiva.controller;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint simples apenas para confirmar que a API está no ar.
 * Pode ser removido quando os controllers reais existirem.
 */
@RestController
@RequestMapping("/api/status")
public class StatusController {

    @GetMapping
    public Map<String, Object> status() {
        return Map.of(
                "status", "ok",
                "aplicacao", "coleta-seletiva",
                "horario", OffsetDateTime.now().toString());
    }
}
