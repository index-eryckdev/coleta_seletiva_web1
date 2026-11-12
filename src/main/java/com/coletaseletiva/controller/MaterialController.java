package com.coletaseletiva.controller;

import com.coletaseletiva.dto.CadastroDtos.*;
import com.coletaseletiva.entity.TipoAtor;
import com.coletaseletiva.service.MaterialService;
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
@Tag(name = "Materiais")
public class MaterialController {
    private final MaterialService materiais;

    public MaterialController(MaterialService materiais) {
        this.materiais = materiais;
    }

    @GetMapping("/materiais")
    @Operation(summary = "Consulta materiais ativos")
    public PaginaResponse<MaterialSaida> listar(@RequestParam(required = false) String nome,
                                                @ParameterObject Pageable pagina) {
        return materiais.listar(nome, true, Paginacao.ordenar(pagina, "nome", "nome", "unidadeMedida"));
    }

    @GetMapping("/materiais/{id}")
    @Operation(summary = "Consulta um material ativo")
    public MaterialSaida buscar(@PathVariable long id) {
        return materiais.buscar(id, true);
    }

    @GetMapping("/admin/materiais")
    @Operation(summary = "Lista materiais por situação")
    public PaginaResponse<MaterialSaida> administrar(@Parameter(hidden = true) ContextoAtor ator,
                                                     @RequestParam(required = false) String nome,
                                                     @RequestParam(required = false) Boolean ativo,
                                                     @ParameterObject Pageable pagina) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        return materiais.listar(nome, ativo, Paginacao.ordenar(pagina, "nome", "nome", "unidadeMedida"));
    }

    @PostMapping("/materiais")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra material")
    public MaterialSaida criar(@Parameter(hidden = true) ContextoAtor ator,
                               @Valid @RequestBody MaterialEntrada dados) {
        return materiais.criar(ator, dados);
    }

    @PutMapping("/materiais/{id}")
    @Operation(summary = "Atualiza material")
    public MaterialSaida atualizar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                                   @Valid @RequestBody MaterialEntrada dados) {
        return materiais.atualizar(ator, id, dados);
    }

    @PatchMapping("/materiais/{id}/inativar")
    @Operation(summary = "Inativa material")
    public MaterialSaida inativar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id) {
        return materiais.alterarAtivo(ator, id, false);
    }

    @PatchMapping("/materiais/{id}/reativar")
    @Operation(summary = "Reativa material")
    public MaterialSaida reativar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id) {
        return materiais.alterarAtivo(ator, id, true);
    }
}
