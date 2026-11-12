package com.coletaseletiva.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.coletaseletiva.dto.CadastroDtos.*;
import com.coletaseletiva.dto.PaginaResponse;
import com.coletaseletiva.entity.*;
import com.coletaseletiva.exception.*;
import com.coletaseletiva.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PontoService {
    private final PontoColetaRepository pontos;
    private final MaterialRepository materiais;
    private final MaterialAceitoRepository vinculos;
    private final SolicitacaoRepository solicitacoes;

    public PontoService(PontoColetaRepository pontos, MaterialRepository materiais,
                        MaterialAceitoRepository vinculos, SolicitacaoRepository solicitacoes) {
        this.pontos = pontos;
        this.materiais = materiais;
        this.vinculos = vinculos;
        this.solicitacoes = solicitacoes;
    }

    public PaginaResponse<PontoSaida> listar(String nome, String municipio, String bairro, Long materialId,
                                           Boolean domiciliar, boolean publico, Pageable pagina) {
        Specification<PontoColeta> filtro = Specification.allOf(List.of());
        if (publico) {
            filtro = filtro.and((p, q, cb) -> cb.isTrue(p.get("ativo")));
        }
        filtro = filtro.and(texto(false, "nome", nome))
                .and(texto(true, "municipio", municipio)).and(texto(true, "bairro", bairro));
        if (domiciliar != null) {
            filtro = filtro.and((p, q, cb) -> cb.equal(p.get("recolhimentoDomiciliar"), domiciliar));
        }
        if (materialId != null) {
            filtro = filtro.and((p, q, cb) -> {
                var sub = q.subquery(Long.class);
                var aceito = sub.from(MaterialAceito.class);
                var material = aceito.join("material");
                sub.select(aceito.get("id")).where(cb.equal(aceito.get("ponto"), p),
                        cb.equal(material.get("id"), materialId), cb.isTrue(material.get("ativo")));
                return cb.exists(sub);
            });
        }
        Page<PontoColeta> resultado = pontos.findAll(filtro, pagina);
        Map<Long, List<MaterialAceitoSaida>> aceitos = carregarVinculos(
                resultado.getContent().stream().map(PontoColeta::getId).toList(), publico);
        return PaginaResponse.de(resultado.map(p -> saida(p, aceitos.getOrDefault(p.getId(), List.of()))));
    }

    private Specification<PontoColeta> texto(boolean endereco, String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            return Specification.allOf(List.of());
        }
        return (p, q, cb) -> {
            var caminho = endereco ? p.get("localizacao").<String>get(campo) : p.<String>get(campo);
            return cb.like(cb.lower(caminho), CadastroSaidas.trecho(valor), '\\');
        };
    }

    public PontoSaida buscar(long id, boolean publico) {
        PontoColeta ponto = pontos.findById(id).filter(p -> !publico || p.getAtivo()).orElseThrow(PontoService::ausente);
        return detalhar(ponto, publico);
    }

    @Transactional
    public PontoSaida criar(ContextoAtor ator, PontoEntrada dados) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        var local = CadastroSaidas.localizacao(dados.endereco(), "endereco.");
        var ponto = new PontoColeta(dados.nome().strip(), local, CadastroSaidas.opcional(dados.contato()),
                CadastroSaidas.opcional(dados.horarios()), dados.ativo(), dados.recolhimentoDomiciliar());
        return saida(pontos.save(ponto), List.of());
    }

    @Transactional
    public PontoSaida atualizar(ContextoAtor ator, long id, PontoEntrada dados) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        var ponto = pontos.bloquear(id).orElseThrow(PontoService::ausente);
        validarAtividade(ponto, dados.ativo());
        var local = CadastroSaidas.localizacao(dados.endereco(), "endereco.");
        ponto.atualizar(dados.nome().strip(), local, CadastroSaidas.opcional(dados.contato()),
                CadastroSaidas.opcional(dados.horarios()), dados.ativo(), dados.recolhimentoDomiciliar());
        return detalhar(ponto, false);
    }

    @Transactional
    public PontoSaida alterarAtivo(ContextoAtor ator, long id, boolean ativo) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        var ponto = pontos.bloquear(id).orElseThrow(PontoService::ausente);
        validarAtividade(ponto, ativo);
        ponto.definirAtivo(ativo);
        return detalhar(ponto, false);
    }

    @Transactional
    public MaterialAceitoSaida vincular(ContextoAtor ator, long pontoId, VinculoEntrada dados) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        var ponto = pontos.bloquear(pontoId).orElseThrow(PontoService::ausente);
        var material = materiais.bloquear(dados.materialId()).orElseThrow(MaterialService::ausente);
        if (vinculos.findByPontoIdAndMaterialId(pontoId, material.getId()).isPresent()) {
            throw new RegraNegocioException("O ponto já aceita este material.");
        }
        if (!material.getAtivo()) {
            throw new RegraNegocioException("Reative o material antes de vinculá-lo.");
        }
        validarLimite(material, dados.limitePorSolicitacao());
        return saida(vinculos.save(new MaterialAceito(ponto, material, CadastroSaidas.opcional(dados.observacoes()),
                dados.limitePorSolicitacao())));
    }

    @Transactional
    public MaterialAceitoSaida editarVinculo(ContextoAtor ator, long pontoId, long materialId, VinculoEdicao dados) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        pontos.bloquear(pontoId).orElseThrow(PontoService::ausente);
        var material = materiais.bloquear(materialId).orElseThrow(MaterialService::ausente);
        var vinculo = obterVinculo(pontoId, materialId);
        validarLimite(material, dados.limitePorSolicitacao());
        vinculo.atualizar(CadastroSaidas.opcional(dados.observacoes()), dados.limitePorSolicitacao());
        return saida(vinculo);
    }

    @Transactional
    public void desvincular(ContextoAtor ator, long pontoId, long materialId) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        pontos.bloquear(pontoId).orElseThrow(PontoService::ausente);
        var vinculo = obterVinculo(pontoId, materialId);
        if (solicitacoes.existeComMaterial(pontoId, materialId, Situacao.ABERTAS)) {
            throw new RegraNegocioException("Há solicitações abertas com este material no ponto.");
        }
        vinculos.delete(vinculo);
    }

    private void validarAtividade(PontoColeta ponto, boolean ativo) {
        if (ponto.getAtivo() && !ativo && solicitacoes.existsByPontoIdAndSituacaoIn(ponto.getId(), Situacao.ABERTAS)) {
            throw new RegraNegocioException("Há solicitações abertas neste ponto.");
        }
    }

    private void validarLimite(Material material, BigDecimal limite) {
        if (limite != null && material.getUnidadeMedida() == UnidadeMedida.UNIDADE && limite.stripTrailingZeros().scale() > 0) {
            throw new EntradaInvalidaException("limitePorSolicitacao", "O limite em peças deve ser inteiro.");
        }
    }

    private MaterialAceito obterVinculo(long pontoId, long materialId) {
        return vinculos.findByPontoIdAndMaterialId(pontoId, materialId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo de material não encontrado."));
    }

    private Map<Long, List<MaterialAceitoSaida>> carregarVinculos(List<Long> ids, boolean publico) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return vinculos.findByPontoIdInOrderByMaterialNomeAscMaterialIdAsc(ids).stream()
                .filter(v -> !publico || v.getMaterial().getAtivo())
                .collect(Collectors.groupingBy(v -> v.getPonto().getId(),
                        Collectors.mapping(this::saida, Collectors.toList())));
    }

    private PontoSaida detalhar(PontoColeta ponto, boolean publico) {
        return saida(ponto, carregarVinculos(List.of(ponto.getId()), publico).getOrDefault(ponto.getId(), List.of()));
    }

    private PontoSaida saida(PontoColeta p, List<MaterialAceitoSaida> aceitos) {
        return new PontoSaida(p.getId(), p.getNome(), CadastroSaidas.endereco(null, p.getLocalizacao()),
                p.getContato(), p.getHorarios(), p.getAtivo(), p.getRecolhimentoDomiciliar(), aceitos);
    }

    private MaterialAceitoSaida saida(MaterialAceito v) {
        return new MaterialAceitoSaida(CadastroSaidas.material(v.getMaterial()), v.getObservacoes(),
                v.getLimitePorSolicitacao());
    }

    static RecursoNaoEncontradoException ausente() {
        return new RecursoNaoEncontradoException("Ponto de coleta não encontrado.");
    }
}
