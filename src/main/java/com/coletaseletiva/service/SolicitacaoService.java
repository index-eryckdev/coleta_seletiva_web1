package com.coletaseletiva.service;

import static com.coletaseletiva.entity.Situacao.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.coletaseletiva.dto.PaginaResponse;
import com.coletaseletiva.dto.SolicitacaoDtos.*;
import com.coletaseletiva.entity.*;
import com.coletaseletiva.exception.*;
import com.coletaseletiva.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SolicitacaoService {
    private final SolicitacaoRepository solicitacoes;
    private final ItemSolicitacaoRepository itens;
    private final HistoricoRepository historicos;
    private final MoradorRepository moradores;
    private final EnderecoRepository enderecos;
    private final PontoColetaRepository pontos;
    private final OperadorRepository operadores;
    private final MaterialRepository materiais;
    private final MaterialAceitoRepository vinculos;

    public SolicitacaoService(SolicitacaoRepository solicitacoes, ItemSolicitacaoRepository itens,
                              HistoricoRepository historicos, MoradorRepository moradores,
                              EnderecoRepository enderecos, PontoColetaRepository pontos, OperadorRepository operadores,
                              MaterialRepository materiais, MaterialAceitoRepository vinculos) {
        this.solicitacoes = solicitacoes;
        this.itens = itens;
        this.historicos = historicos;
        this.moradores = moradores;
        this.enderecos = enderecos;
        this.pontos = pontos;
        this.operadores = operadores;
        this.materiais = materiais;
        this.vinculos = vinculos;
    }

    public PaginaResponse<ResumoSolicitacao> listar(ContextoAtor ator, Situacao situacao, Long pontoId,
                                                   OffsetDateTime inicio, OffsetDateTime fim, Pageable pagina) {
        validarPeriodo(inicio, fim);
        Specification<Solicitacao> filtro = filtroAtor(ator);
        if (situacao != null) {
            filtro = filtro.and((s, q, cb) -> cb.equal(s.get("situacao"), situacao));
        }
        if (pontoId != null) {
            filtro = filtro.and((s, q, cb) -> cb.equal(s.get("ponto").get("id"), pontoId));
        }
        if (inicio != null) {
            filtro = filtro.and((s, q, cb) -> cb.greaterThanOrEqualTo(s.get("dataCriacao"), inicio));
        }
        if (fim != null) {
            filtro = filtro.and((s, q, cb) -> cb.lessThan(s.get("dataCriacao"), fim));
        }
        return PaginaResponse.de(solicitacoes.findAll(filtro, pagina).map(this::resumo));
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Detalhe buscar(ContextoAtor ator, long id) {
        var solicitacao = obter(id);
        validarAcesso(ator, solicitacao, false);
        return detalhar(solicitacao);
    }

    @Transactional
    public Detalhe criar(ContextoAtor ator, Criacao dados) {
        Morador morador = bloquearMorador(ator);
        var ponto = pontos.bloquear(dados.pontoId()).orElseThrow(PontoService::ausente);
        var local = endereco(ator.id(), dados.enderecoId());
        var materiaisValidados = validarItens(ponto, dados.itens());
        long numero = solicitacoes.proximoProtocolo();
        String protocolo = String.format(Locale.ROOT, "CS-%012d", numero);
        var solicitacao = solicitacoes.save(new Solicitacao(protocolo, morador, ponto, local,
                CadastroSaidas.opcional(dados.observacoes()), agora()));
        salvarItens(solicitacao, dados.itens(), materiaisValidados);
        historico(solicitacao, null, ator, "Solicitação criada.", null, null, null);
        return detalhar(solicitacao);
    }

    @Transactional
    public Detalhe corrigir(ContextoAtor ator, long id, Correcao dados) {
        ator.exigir(TipoAtor.MORADOR);
        var solicitacao = obter(id);
        validarMutacao(ator, solicitacao, dados.versao(), SOLICITADA);
        var ponto = pontos.bloquear(solicitacao.getPonto().getId()).orElseThrow(PontoService::ausente);
        var local = endereco(ator.id(), dados.enderecoId());
        var materiaisValidados = validarItens(ponto, dados.itens());
        solicitacao.corrigir(local, CadastroSaidas.opcional(dados.observacoes()), proximoEvento(solicitacao));
        // A atualização da versão precede a substituição dos itens e o evento de auditoria.
        solicitacoes.flush();
        itens.deleteAll(itens.findBySolicitacaoIdOrderById(id));
        itens.flush();
        salvarItens(solicitacao, dados.itens(), materiaisValidados);
        historico(solicitacao, SOLICITADA, ator, "Endereço ou itens revisados.", null, null, null);
        return detalhar(solicitacao);
    }

    @Transactional
    public Detalhe agendar(ContextoAtor ator, long id, Agendamento dados) {
        var solicitacao = operacaoDoPonto(ator, id, dados.versao(), SOLICITADA);
        validarFuturo(dados.dataAgendamento());
        return transicao(solicitacao, AGENDADA, ator, motivo(dados.motivo(), "Recolhimento agendado."),
                null, null, dados.dataAgendamento());
    }

    @Transactional
    public Detalhe recusar(ContextoAtor ator, long id, Motivo dados) {
        var solicitacao = operacaoDoPonto(ator, id, dados.versao(), SOLICITADA);
        return transicao(solicitacao, RECUSADA, ator, dados.motivo().strip(), null, null, null);
    }

    @Transactional
    public Detalhe cancelar(ContextoAtor ator, long id, Motivo dados) {
        var solicitacao = obter(id);
        validarMutacao(ator, solicitacao, dados.versao(), SOLICITADA, AGENDADA);
        return transicao(solicitacao, CANCELADA, ator, dados.motivo().strip(), null, null, null);
    }

    @Transactional
    public Detalhe iniciar(ContextoAtor ator, long id, Versao dados) {
        var solicitacao = operacaoDoPonto(ator, id, dados.versao(), AGENDADA);
        return transicao(solicitacao, EM_RECOLHIMENTO, ator, "Recolhimento iniciado.",
                solicitacao.getTentativa() + 1, null, null);
    }

    @Transactional
    public Detalhe concluir(ContextoAtor ator, long id, Conclusao dados) {
        var solicitacao = operacaoDoPonto(ator, id, dados.versao(), EM_RECOLHIMENTO);
        var itensSolicitados = itens.findBySolicitacaoIdOrderById(id);
        var quantidades = validarConclusao(dados.itens(), itensSolicitados);
        itensSolicitados.forEach(item -> item.concluir(quantidades.get(item.getMaterial().getId())));
        return transicao(solicitacao, CONCLUIDA, ator, "Recolhimento concluído.", solicitacao.getTentativa(), null, null);
    }

    @Transactional
    public Detalhe reagendar(ContextoAtor ator, long id, Reagendamento dados) {
        var solicitacao = operacaoDoPonto(ator, id, dados.versao(), AGENDADA, EM_RECOLHIMENTO);
        validarFuturo(dados.dataAgendamento());
        boolean falha = solicitacao.getSituacao() == EM_RECOLHIMENTO;
        String detalhes = CadastroSaidas.opcional(dados.detalhesTentativa());
        if (detalhes != null && !falha) {
            throw new EntradaInvalidaException("detalhesTentativa", "Registre detalhes após iniciar o recolhimento.");
        }
        return transicao(solicitacao, AGENDADA, ator, dados.motivo().strip(),
                falha ? solicitacao.getTentativa() : null, detalhes, dados.dataAgendamento());
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Indicadores indicadores(ContextoAtor ator, OffsetDateTime inicio, OffsetDateTime fim) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        validarPeriodo(inicio, fim);
        var contagens = new EnumMap<Situacao, Long>(Situacao.class);
        for (Situacao situacao : Situacao.values()) {
            contagens.put(situacao, 0L);
        }
        solicitacoes.contarPorSituacao(inicio, fim)
                .forEach(c -> contagens.put(Situacao.valueOf(c.getSituacao()), c.getTotal()));
        var totais = itens.totalRecolhido(inicio, fim).stream()
                .map(t -> new TotalMaterial(t.getMaterialId(), t.getMaterial(), UnidadeMedida.valueOf(t.getUnidade()),
                        t.getQuantidade())).toList();
        return new Indicadores(contagens, totais);
    }

    private Map<Long, Material> validarItens(PontoColeta ponto, List<ItemEntrada> entradas) {
        if (!ponto.getAtivo() || !ponto.getRecolhimentoDomiciliar()) {
            throw new RegraNegocioException("O ponto não está disponível para recolhimento domiciliar.");
        }

        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < entradas.size(); i++) {
            if (!ids.add(entradas.get(i).materialId())) {
                throw new EntradaInvalidaException("itens[" + i + "].materialId", "Escolha cada material uma única vez.");
            }
        }

        Map<Long, Material> porId = materiais.lerBloqueados(ids).stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));
        Map<Long, MaterialAceito> aceitos = vinculos.findByPontoIdInOrderByMaterialNomeAscMaterialIdAsc(List.of(ponto.getId()))
                .stream().collect(Collectors.toMap(v -> v.getMaterial().getId(), Function.identity()));

        for (int i = 0; i < entradas.size(); i++) {
            var entrada = entradas.get(i);
            var material = porId.get(entrada.materialId());
            var aceito = aceitos.get(entrada.materialId());
            if (material == null) {
                throw MaterialService.ausente();
            }
            if (!material.getAtivo() || aceito == null) {
                throw new RegraNegocioException("O ponto não aceita o material " + material.getNome() + " para novas solicitações.");
            }
            var quantidade = entrada.quantidadeEstimada();
            validarUnidade(material.getUnidadeMedida(), quantidade, "itens[" + i + "].quantidadeEstimada");
            var limite = aceito.getLimitePorSolicitacao();
            if (limite != null && quantidade.compareTo(limite) > 0) {
                throw new EntradaInvalidaException("itens[" + i + "].quantidadeEstimada",
                        "O limite de " + material.getNome() + " é " + limite.stripTrailingZeros().toPlainString()
                                + " " + material.getUnidadeMedida() + ".");
            }
        }
        return porId;
    }

    private void salvarItens(Solicitacao solicitacao, List<ItemEntrada> entradas, Map<Long, Material> porId) {
        var novos = entradas.stream().map(i -> new ItemSolicitacao(solicitacao, porId.get(i.materialId()),
                i.quantidadeEstimada())).toList();
        itens.saveAll(novos);
    }

    private Map<Long, BigDecimal> validarConclusao(List<ItemConclusao> efetivos, List<ItemSolicitacao> esperados) {
        Map<Long, ItemSolicitacao> porId = esperados.stream()
                .collect(Collectors.toMap(i -> i.getMaterial().getId(), Function.identity()));
        Map<Long, BigDecimal> quantidades = new HashMap<>();
        for (int i = 0; i < efetivos.size(); i++) {
            var efetivo = efetivos.get(i);
            var item = porId.get(efetivo.materialId());
            if (item == null || quantidades.putIfAbsent(efetivo.materialId(), efetivo.quantidadeEfetiva()) != null) {
                throw new EntradaInvalidaException("itens[" + i + "].materialId",
                        "Informe uma quantidade por material da solicitação.");
            }
            validarUnidade(item.getUnidade(), efetivo.quantidadeEfetiva(), "itens[" + i + "].quantidadeEfetiva");
        }
        if (quantidades.size() != esperados.size() || quantidades.values().stream().noneMatch(q -> q.signum() > 0)) {
            throw new EntradaInvalidaException("itens",
                    "Informe a quantidade de todos os itens, com pelo menos uma quantidade positiva.");
        }
        return quantidades;
    }

    private void validarUnidade(UnidadeMedida unidade, BigDecimal quantidade, String campo) {
        if (unidade == UnidadeMedida.UNIDADE && quantidade.stripTrailingZeros().scale() > 0) {
            throw new EntradaInvalidaException(campo, "A quantidade em peças deve ser inteira.");
        }
    }

    private Solicitacao operacaoDoPonto(ContextoAtor ator, long id, long versao, Situacao... permitidas) {
        ator.exigir(TipoAtor.OPERADOR);
        var solicitacao = obter(id);
        validarMutacao(ator, solicitacao, versao, permitidas);
        return solicitacao;
    }

    private void validarMutacao(ContextoAtor ator, Solicitacao solicitacao, long versao, Situacao... permitidas) {
        validarAcesso(ator, solicitacao, true);
        if (solicitacao.getVersao() != versao) {
            throw new ConflitoVersaoException();
        }
        if (!Set.of(permitidas).contains(solicitacao.getSituacao())) {
            throw new RegraNegocioException("A operação não é permitida na situação " + solicitacao.getSituacao() + ".");
        }
    }

    private void validarAcesso(ContextoAtor ator, Solicitacao solicitacao, boolean mutacao) {
        if (ator.tipo() == TipoAtor.OPERADOR) {
            if (!pontosDoOperador(ator, mutacao).contains(solicitacao.getPonto().getId())) {
                throw new AcessoDemoException("O operador não está vinculado a este ponto.");
            }
        } else {
            ator.exigirMoradorOuAdministrador(solicitacao.getMorador().getId());
            if (ator.tipo() == TipoAtor.MORADOR) {
                if (mutacao) {
                    bloquearMorador(ator);
                } else {
                    validarMorador(ator);
                }
            }
        }
    }

    private Specification<Solicitacao> filtroAtor(ContextoAtor ator) {
        if (ator.tipo() == TipoAtor.OPERADOR) {
            List<Long> pontosDoAtor = pontosDoOperador(ator, false);
            return (s, q, cb) -> pontosDoAtor.isEmpty() ? cb.disjunction() : s.get("ponto").get("id").in(pontosDoAtor);
        }
        if (ator.tipo() == TipoAtor.ADMINISTRADOR) {
            return Specification.allOf(List.of());
        }
        validarMorador(ator);
        return (s, q, cb) -> cb.equal(s.get("morador").get("id"), ator.id());
    }

    private List<Long> pontosDoOperador(ContextoAtor ator, boolean mutacao) {
        ator.exigir(TipoAtor.OPERADOR);
        boolean ativo = mutacao ? operadores.lerAtivoBloqueado(ator.id()).isPresent()
                : operadores.existsByIdAndAtivoTrue(ator.id());
        if (!ativo) {
            throw new AcessoDemoException("O operador selecionado não está ativo ou não existe.");
        }
        return operadores.pontosAtivosDoOperador(ator.id());
    }

    private Morador bloquearMorador(ContextoAtor ator) {
        ator.exigir(TipoAtor.MORADOR);
        return moradores.lerAtivoBloqueado(ator.id()).orElseThrow(this::moradorInvalido);
    }

    private void validarMorador(ContextoAtor ator) {
        ator.exigir(TipoAtor.MORADOR);
        if (!moradores.existsByIdAndAtivoTrue(ator.id())) {
            throw moradorInvalido();
        }
    }

    private AcessoDemoException moradorInvalido() {
        return new AcessoDemoException("O morador selecionado não está ativo ou não existe.");
    }

    private Localizacao endereco(long moradorId, long enderecoId) {
        return enderecos.findByIdAndMoradorId(enderecoId, moradorId).map(Endereco::getLocalizacao)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado para este morador."));
    }

    private Solicitacao obter(long id) {
        return solicitacoes.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada."));
    }

    private Detalhe transicao(Solicitacao solicitacao, Situacao nova, ContextoAtor ator, String motivo,
                              Integer tentativa, String detalhes, OffsetDateTime agendamento) {
        Situacao anterior = solicitacao.getSituacao();
        solicitacao.registrar(nova, agendamento, proximoEvento(solicitacao));
        solicitacoes.flush();
        historico(solicitacao, anterior, ator, motivo, tentativa, detalhes, agendamento);
        return detalhar(solicitacao);
    }

    private void historico(Solicitacao s, Situacao anterior, ContextoAtor ator, String motivo, Integer tentativa,
                           String detalhes, OffsetDateTime agendamento) {
        historicos.save(new Historico(s, anterior, ator.tipo(), ator.id(), motivo, tentativa, detalhes, agendamento));
    }

    private Detalhe detalhar(Solicitacao solicitacao) {
        var itensSaida = itens.findBySolicitacaoIdOrderById(solicitacao.getId()).stream()
                .map(i -> new ItemSaida(i.getMaterial().getId(), i.getMaterial().getNome(), i.getUnidade(),
                        i.getQuantidadeEstimada(), i.getQuantidadeEfetiva())).toList();
        var eventos = historicos.findBySolicitacaoIdOrderByDataHoraAscIdAsc(solicitacao.getId()).stream()
                .map(h -> new EventoSaida(h.getSituacaoAnterior(), h.getSituacaoNova(), h.getDataHora(),
                        h.getResponsavelTipo(), h.getResponsavelId(), h.getMotivo(), h.getNumeroTentativa(),
                        h.getDetalhesTentativa(), h.getDataAgendamento())).toList();
        return new Detalhe(resumo(solicitacao), CadastroSaidas.endereco(null, solicitacao.getEndereco()),
                solicitacao.getObservacoes(), itensSaida, eventos);
    }

    private ResumoSolicitacao resumo(Solicitacao s) {
        return new ResumoSolicitacao(s.getId(), s.getProtocolo(), s.getMorador().getId(), s.getPonto().getId(),
                s.getPonto().getNome(), s.getSituacao(), s.getDataCriacao(), s.getDataAgendamento(),
                s.getDataConclusao(), s.getVersao());
    }

    private OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private OffsetDateTime proximoEvento(Solicitacao solicitacao) {
        OffsetDateTime agora = agora();
        OffsetDateTime anterior = solicitacao.getDataAtualizacao();
        return agora.isAfter(anterior) ? agora : anterior.plus(1, ChronoUnit.MICROS);
    }

    private void validarFuturo(OffsetDateTime agendamento) {
        if (!agendamento.isAfter(agora())) {
            throw new EntradaInvalidaException("dataAgendamento", "Informe uma data e hora futuras.");
        }
    }

    private void validarPeriodo(OffsetDateTime inicio, OffsetDateTime fim) {
        if (inicio != null && fim != null && !inicio.isBefore(fim)) {
            throw new EntradaInvalidaException("fim", "O fim deve ser posterior ao início.");
        }
    }

    private String motivo(String entrada, String padrao) {
        return entrada == null || entrada.isBlank() ? padrao : entrada.strip();
    }
}
