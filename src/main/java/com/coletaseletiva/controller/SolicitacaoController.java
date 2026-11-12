package com.coletaseletiva.controller;

import java.time.OffsetDateTime;
import com.coletaseletiva.dto.SolicitacaoDtos.*;
import com.coletaseletiva.entity.Situacao;
import com.coletaseletiva.service.SolicitacaoService;
import org.springframework.format.annotation.DateTimeFormat;
import com.coletaseletiva.dto.PaginaResponse;
import com.coletaseletiva.service.ContextoAtor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Solicitações e indicadores")
public class SolicitacaoController {
    private final SolicitacaoService solicitacoes;

    public SolicitacaoController(SolicitacaoService solicitacoes) {
        this.solicitacoes = solicitacoes;
    }

    @GetMapping("/solicitacoes")
    @Operation(summary = "Lista solicitações no contexto do ator")
    public PaginaResponse<ResumoSolicitacao> listar(@Parameter(hidden = true) ContextoAtor ator,
                                                    @RequestParam(required = false) Situacao situacao,
                                                    @RequestParam(required = false) Long pontoId,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim,
                                                    @ParameterObject Pageable pagina) {
        return solicitacoes.listar(ator, situacao, pontoId, inicio, fim,
                Paginacao.ordenar(pagina, "dataCriacao", "dataCriacao", "situacao", "protocolo"));
    }

    @GetMapping("/solicitacoes/{id}")
    @Operation(summary = "Consulta detalhes, itens e histórico")
    public Detalhe buscar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id) {
        return solicitacoes.buscar(ator, id);
    }

    @PostMapping("/solicitacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra solicitação com itens")
    public Detalhe criar(@Parameter(hidden = true) ContextoAtor ator, @Valid @RequestBody Criacao dados) {
        return solicitacoes.criar(ator, dados);
    }

    @PutMapping("/solicitacoes/{id}")
    @Operation(summary = "Corrige endereço e itens antes do agendamento")
    public Detalhe corrigir(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                            @Valid @RequestBody Correcao dados) {
        return solicitacoes.corrigir(ator, id, dados);
    }

    @PatchMapping("/solicitacoes/{id}/agendar")
    @Operation(summary = "Agenda recolhimento")
    public Detalhe agendar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                           @Valid @RequestBody Agendamento dados) {
        return solicitacoes.agendar(ator, id, dados);
    }

    @PatchMapping("/solicitacoes/{id}/recusar")
    @Operation(summary = "Recusa solicitação com motivo")
    public Detalhe recusar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                           @Valid @RequestBody Motivo dados) {
        return solicitacoes.recusar(ator, id, dados);
    }

    @PatchMapping("/solicitacoes/{id}/cancelar")
    @Operation(summary = "Cancela solicitação com motivo")
    public Detalhe cancelar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                            @Valid @RequestBody Motivo dados) {
        return solicitacoes.cancelar(ator, id, dados);
    }

    @PatchMapping("/solicitacoes/{id}/iniciar")
    @Operation(summary = "Inicia recolhimento")
    public Detalhe iniciar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                           @Valid @RequestBody Versao dados) {
        return solicitacoes.iniciar(ator, id, dados);
    }

    @PatchMapping("/solicitacoes/{id}/concluir")
    @Operation(summary = "Registra quantidades efetivas e conclui")
    public Detalhe concluir(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                            @Valid @RequestBody Conclusao dados) {
        return solicitacoes.concluir(ator, id, dados);
    }

    @PatchMapping("/solicitacoes/{id}/reagendar")
    @Operation(summary = "Reagenda ou registra tentativa sem sucesso")
    public Detalhe reagendar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                             @Valid @RequestBody Reagendamento dados) {
        return solicitacoes.reagendar(ator, id, dados);
    }

    @GetMapping("/indicadores")
    @Operation(summary = "Resume solicitações e quantidades recolhidas")
    public Indicadores indicadores(@Parameter(hidden = true) ContextoAtor ator,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim) {
        return solicitacoes.indicadores(ator, inicio, fim);
    }
}
