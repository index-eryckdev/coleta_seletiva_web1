package com.coletaseletiva.service;

import com.coletaseletiva.dto.CadastroDtos.*;
import com.coletaseletiva.dto.PaginaResponse;
import com.coletaseletiva.entity.*;
import com.coletaseletiva.exception.*;
import com.coletaseletiva.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PessoasService {
    private final MoradorRepository moradores;
    private final EnderecoRepository enderecos;
    private final OperadorRepository operadores;
    private final PontoColetaRepository pontos;

    public PessoasService(MoradorRepository moradores, EnderecoRepository enderecos,
                          OperadorRepository operadores, PontoColetaRepository pontos) {
        this.moradores = moradores;
        this.enderecos = enderecos;
        this.operadores = operadores;
        this.pontos = pontos;
    }

    @Transactional
    public MoradorSaida criarMorador(MoradorEntrada dados) {
        if (moradores.existsByEmailIgnoreCase(dados.email())) {
            throw new RegraNegocioException("Já existe um morador com este e-mail.");
        }
        return moradorSaida(moradores.save(new Morador(dados.nome().strip(), dados.email(),
                CadastroSaidas.opcional(dados.telefone()))));
    }

    public PaginaResponse<MoradorSaida> listarMoradores(ContextoAtor ator, Pageable pagina) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        return PaginaResponse.de(moradores.findAll(pagina).map(this::moradorSaida));
    }

    public MoradorSaida buscarMorador(ContextoAtor ator, long id) {
        ator.exigirMoradorOuAdministrador(id);
        return moradorSaida(obterMorador(id));
    }

    @Transactional
    public MoradorSaida atualizarMorador(ContextoAtor ator, long id, MoradorEntrada dados) {
        ator.exigirMoradorOuAdministrador(id);
        Morador morador = moradores.bloquear(id).orElseThrow(PessoasService::moradorAusente);
        if (moradores.existsByEmailIgnoreCaseAndIdNot(dados.email(), id)) {
            throw new RegraNegocioException("Já existe um morador com este e-mail.");
        }
        morador.atualizar(dados.nome().strip(), dados.email(), CadastroSaidas.opcional(dados.telefone()));
        return moradorSaida(morador);
    }

    @Transactional
    public MoradorSaida alterarAtivo(ContextoAtor ator, long id, boolean ativo) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        Morador morador = moradores.bloquear(id).orElseThrow(PessoasService::moradorAusente);
        morador.definirAtivo(ativo);
        return moradorSaida(morador);
    }

    public PaginaResponse<EnderecoSaida> listarEnderecos(ContextoAtor ator, long moradorId, Pageable pagina) {
        ator.exigirMoradorOuAdministrador(moradorId);
        obterMorador(moradorId);
        return PaginaResponse.de(enderecos.findByMoradorId(moradorId, pagina).map(this::enderecoSaida));
    }

    public EnderecoSaida buscarEndereco(ContextoAtor ator, long moradorId, long id) {
        ator.exigirMoradorOuAdministrador(moradorId);
        return enderecoSaida(obterEndereco(id, moradorId));
    }

    @Transactional
    public EnderecoSaida criarEndereco(ContextoAtor ator, long moradorId, EnderecoEntrada dados) {
        ator.exigirMoradorOuAdministrador(moradorId);
        Morador morador = obterMorador(moradorId);
        Endereco endereco = new Endereco(morador, CadastroSaidas.localizacao(dados, ""));
        return enderecoSaida(enderecos.save(endereco));
    }

    @Transactional
    public EnderecoSaida atualizarEndereco(ContextoAtor ator, long moradorId, long id, EnderecoEntrada dados) {
        ator.exigirMoradorOuAdministrador(moradorId);
        Endereco endereco = obterEndereco(id, moradorId);
        endereco.atualizar(CadastroSaidas.localizacao(dados, ""));
        return enderecoSaida(endereco);
    }

    public PaginaResponse<OperadorSaida> listarOperadores(ContextoAtor ator, Pageable pagina) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        return PaginaResponse.de(operadores.findAll(pagina).map(this::operadorSaida));
    }

    public OperadorSaida buscarOperador(ContextoAtor ator, long id) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        return operadorSaida(operadores.findById(id).orElseThrow(PessoasService::operadorAusente));
    }

    @Transactional
    public OperadorSaida criarOperador(ContextoAtor ator, OperadorEntrada dados) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        if (operadores.existsByEmailIgnoreCase(dados.email())) {
            throw new RegraNegocioException("Já existe um operador com este e-mail.");
        }
        return operadorSaida(operadores.save(new Operador(dados.nome().strip(), dados.email(), dados.ativo())));
    }

    @Transactional
    public OperadorSaida atualizarOperador(ContextoAtor ator, long id, OperadorEntrada dados) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        Operador operador = operadores.bloquear(id).orElseThrow(PessoasService::operadorAusente);
        if (operadores.existsByEmailIgnoreCaseAndIdNot(dados.email(), id)) {
            throw new RegraNegocioException("Já existe um operador com este e-mail.");
        }
        operador.atualizar(dados.nome().strip(), dados.email(), dados.ativo());
        return operadorSaida(operador);
    }

    @Transactional
    public OperadorSaida vincularOperador(ContextoAtor ator, long id, long pontoId, boolean vincular) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        Operador operador = operadores.bloquear(id).orElseThrow(PessoasService::operadorAusente);
        PontoColeta ponto = pontos.findById(pontoId).orElseThrow(PontoService::ausente);
        if (vincular) {
            operador.vincular(ponto);
        } else {
            operador.desvincular(ponto);
        }
        return operadorSaida(operador);
    }

    private Morador obterMorador(long id) {
        return moradores.findById(id).orElseThrow(PessoasService::moradorAusente);
    }

    private Endereco obterEndereco(long id, long moradorId) {
        return enderecos.findByIdAndMoradorId(id, moradorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado para este morador."));
    }

    private MoradorSaida moradorSaida(Morador m) {
        return new MoradorSaida(m.getId(), m.getNome(), m.getEmail(), m.getTelefone(), m.getAtivo());
    }

    private EnderecoSaida enderecoSaida(Endereco e) {
        return CadastroSaidas.endereco(e.getId(), e.getLocalizacao());
    }

    private OperadorSaida operadorSaida(Operador o) {
        return new OperadorSaida(o.getId(), o.getNome(), o.getEmail(), o.getAtivo(),
                o.getPontos().stream().map(PontoColeta::getId).sorted().toList());
    }

    private static RecursoNaoEncontradoException moradorAusente() {
        return new RecursoNaoEncontradoException("Morador não encontrado.");
    }

    private static RecursoNaoEncontradoException operadorAusente() {
        return new RecursoNaoEncontradoException("Operador não encontrado.");
    }
}
