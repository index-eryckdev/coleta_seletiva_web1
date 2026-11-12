package com.coletaseletiva.controller;

import com.coletaseletiva.dto.CadastroDtos.*;
import com.coletaseletiva.entity.TipoAtor;
import com.coletaseletiva.service.PontoService;
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
@Tag(name = "Pontos de coleta")
public class PontoController {
    private final PontoService pontos;

    public PontoController(PontoService pontos) {
        this.pontos = pontos;
    }

    @GetMapping("/pontos")
    @Operation(summary = "Pesquisa pontos ativos e seus materiais aceitos")
    public PaginaResponse<PontoSaida> listar(@RequestParam(required = false) String nome,
                                             @RequestParam(required = false) String municipio,
                                             @RequestParam(required = false) String bairro,
                                             @RequestParam(required = false) Long materialId,
                                             @RequestParam(required = false) Boolean recolhimentoDomiciliar,
                                             @ParameterObject Pageable pagina) {
        return pontos.listar(nome, municipio, bairro, materialId, recolhimentoDomiciliar, true,
                Paginacao.ordenar(pagina, "nome", "nome"));
    }

    @GetMapping("/pontos/{id}")
    @Operation(summary = "Consulta um ponto ativo")
    public PontoSaida buscar(@PathVariable long id) {
        return pontos.buscar(id, true);
    }

    @GetMapping("/admin/pontos")
    @Operation(summary = "Lista pontos para administração")
    public PaginaResponse<PontoSaida> administrar(@Parameter(hidden = true) ContextoAtor ator,
                                                  @RequestParam(required = false) String nome,
                                                  @RequestParam(required = false) String municipio,
                                                  @RequestParam(required = false) String bairro,
                                                  @RequestParam(required = false) Long materialId,
                                                  @RequestParam(required = false) Boolean recolhimentoDomiciliar,
                                                  @ParameterObject Pageable pagina) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        return pontos.listar(nome, municipio, bairro, materialId, recolhimentoDomiciliar, false,
                Paginacao.ordenar(pagina, "nome", "nome"));
    }

    @PostMapping("/pontos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra ponto")
    public PontoSaida criar(@Parameter(hidden = true) ContextoAtor ator, @Valid @RequestBody PontoEntrada dados) {
        return pontos.criar(ator, dados);
    }

    @PutMapping("/pontos/{id}")
    @Operation(summary = "Atualiza ponto")
    public PontoSaida atualizar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                                @Valid @RequestBody PontoEntrada dados) {
        return pontos.atualizar(ator, id, dados);
    }

    @PatchMapping("/pontos/{id}/inativar")
    @Operation(summary = "Inativa ponto sem solicitações abertas")
    public PontoSaida inativar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id) {
        return pontos.alterarAtivo(ator, id, false);
    }

    @PatchMapping("/pontos/{id}/reativar")
    @Operation(summary = "Reativa ponto")
    public PontoSaida reativar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id) {
        return pontos.alterarAtivo(ator, id, true);
    }

    @PostMapping("/pontos/{id}/materiais")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Vincula material ao ponto")
    public MaterialAceitoSaida vincular(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                                        @Valid @RequestBody VinculoEntrada dados) {
        return pontos.vincular(ator, id, dados);
    }

    @PutMapping("/pontos/{id}/materiais/{materialId}")
    @Operation(summary = "Atualiza limite e observações")
    public MaterialAceitoSaida editarVinculo(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                                             @PathVariable long materialId,
                                             @Valid @RequestBody VinculoEdicao dados) {
        return pontos.editarVinculo(ator, id, materialId, dados);
    }

    @DeleteMapping("/pontos/{id}/materiais/{materialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove vínculo sem pedidos abertos afetados")
    public void desvincular(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                            @PathVariable long materialId) {
        pontos.desvincular(ator, id, materialId);
    }
}
