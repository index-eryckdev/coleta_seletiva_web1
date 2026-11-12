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
@RequestMapping("/api/moradores")
@Tag(name = "Moradores e endereços")
public class MoradorController {
    private final PessoasService pessoas;

    public MoradorController(PessoasService pessoas) {
        this.pessoas = pessoas;
    }

    @GetMapping
    @Operation(summary = "Lista moradores para administração")
    public PaginaResponse<MoradorSaida> listar(@Parameter(hidden = true) ContextoAtor ator,
                                               @ParameterObject Pageable pagina) {
        return pessoas.listarMoradores(ator, Paginacao.ordenar(pagina, "nome", "nome"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra morador")
    public MoradorSaida criar(@Valid @RequestBody MoradorEntrada dados) {
        return pessoas.criarMorador(dados);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta o próprio cadastro")
    public MoradorSaida buscar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id) {
        return pessoas.buscarMorador(ator, id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza o cadastro")
    public MoradorSaida atualizar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                                  @Valid @RequestBody MoradorEntrada dados) {
        return pessoas.atualizarMorador(ator, id, dados);
    }

    @PatchMapping("/{id}/inativar")
    @Operation(summary = "Inativa morador")
    public MoradorSaida inativar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id) {
        return pessoas.alterarAtivo(ator, id, false);
    }

    @PatchMapping("/{id}/reativar")
    @Operation(summary = "Reativa morador")
    public MoradorSaida reativar(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id) {
        return pessoas.alterarAtivo(ator, id, true);
    }

    @GetMapping("/{id}/enderecos")
    @Operation(summary = "Lista endereços do morador")
    public PaginaResponse<EnderecoSaida> enderecos(@Parameter(hidden = true) ContextoAtor ator,
                                                   @PathVariable long id, @ParameterObject Pageable pagina) {
        return pessoas.listarEnderecos(ator, id, Paginacao.ordenar(pagina, "id"));
    }

    @GetMapping("/{id}/enderecos/{enderecoId}")
    @Operation(summary = "Consulta endereço do morador")
    public EnderecoSaida endereco(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                                  @PathVariable long enderecoId) {
        return pessoas.buscarEndereco(ator, id, enderecoId);
    }

    @PostMapping("/{id}/enderecos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra endereço")
    public EnderecoSaida criarEndereco(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                                       @Valid @RequestBody EnderecoEntrada dados) {
        return pessoas.criarEndereco(ator, id, dados);
    }

    @PutMapping("/{id}/enderecos/{enderecoId}")
    @Operation(summary = "Atualiza endereço do morador")
    public EnderecoSaida atualizarEndereco(@Parameter(hidden = true) ContextoAtor ator, @PathVariable long id,
                                           @PathVariable long enderecoId,
                                           @Valid @RequestBody EnderecoEntrada dados) {
        return pessoas.atualizarEndereco(ator, id, enderecoId, dados);
    }
}
