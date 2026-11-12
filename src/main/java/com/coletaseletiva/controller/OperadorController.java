package com.coletaseletiva.controller;

import com.coletaseletiva.dto.CadastroDtos.*;
import com.coletaseletiva.service.PessoasService;
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
@RequestMapping("/api/operadores")
@Tag(name = "Operadores")
public class OperadorController {
    private final PessoasService pessoas;

    public OperadorController(PessoasService pessoas) {
        this.pessoas = pessoas;
    }

    @GetMapping
    @Operation(summary = "Lista operadores")
    public PaginaResponse<OperadorSaida> listar(@Parameter(hidden = true) ContextoAtor ator,
                                                @ParameterObject Pageable pagina) {
        return pessoas.listarOperadores(ator, Paginacao.ordenar(pagina, "nome", "nome"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta operador")
    public OperadorSaida buscar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id) {
        return pessoas.buscarOperador(ator, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra operador")
    public OperadorSaida criar(@Parameter(hidden = true) ContextoAtor ator,
                               @Valid @RequestBody OperadorEntrada dados) {
        return pessoas.criarOperador(ator, dados);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza operador")
    public OperadorSaida atualizar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                                   @Valid @RequestBody OperadorEntrada dados) {
        return pessoas.atualizarOperador(ator, id, dados);
    }

    @PutMapping("/{id}/pontos/{pontoId}")
    @Operation(summary = "Vincula operador ao ponto")
    public OperadorSaida vincular(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                                  @PathVariable long pontoId) {
        return pessoas.vincularOperador(ator, id, pontoId, true);
    }

    @DeleteMapping("/{id}/pontos/{pontoId}")
    @Operation(summary = "Remove vínculo do operador")
    public OperadorSaida desvincular(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                                     @PathVariable long pontoId) {
        return pessoas.vincularOperador(ator, id, pontoId, false);
    }
}
