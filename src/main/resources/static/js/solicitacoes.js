import { todas } from "./api.js";
import { botao, campo, enderecoTexto, exibirData, exibirErro, exibirNumero, formulario, h, link,
  listaVazia, paginador, painel, perfis, situacoes, titulo, unidades } from "./ui.js";

export async function listaSolicitacoes(ctx) {
  const pontos = await todas(ctx.pedir, "/pontos");
  const resultados = h("div", { class: "pilha", "aria-live": "polite" });
  let consulta = {};
  let chamada = 0;

  async function carregar(page = 0) {
    const atual = ++chamada;
    resultados.replaceChildren(listaVazia("Carregando solicitações…"));
    try {
      const lista = await ctx.pedir("/solicitacoes", { consulta: { ...consulta, page, size: 10 } });
      if (atual !== chamada) return;
      resultados.replaceChildren(...[
        lista.conteudo.length ? h("div", { class: "grade" }, lista.conteudo.map(s =>
          h("article", { class: "cartao" },
            h("div", { class: "linha entre" }, h("h3", {}, s.protocolo), etiqueta(s.situacao)),
            h("p", {}, s.ponto), h("p", { class: "nota" }, "Criada em " + exibirData(s.dataCriacao)),
            s.dataAgendamento && h("p", {}, "Agendada para " + exibirData(s.dataAgendamento)),
            h("div", {}, link("Ver detalhes", "solicitacao/" + s.id)))))
          : listaVazia("Nenhuma solicitação para os filtros e o perfil selecionados."),
        paginador(lista, carregar),
      ].filter(Boolean));
    } catch (erro) {
      if (atual === chamada && erro.name !== "AbortError") exibirErro(resultados, erro);
    }
  }

  const filtro = formulario("Filtrar", async valores => {
    consulta = { ...valores, ...periodo(valores) };
    await carregar();
  }, h("div", { class: "grade-campos" },
    campo("situacao", "Situação", { itens: Object.entries(situacoes), vazio: "Todas" }),
    campo("pontoId", "Ponto de coleta", { itens: pontos.map(p => [p.id, p.nome]), vazio: "Todos" }),
    camposPeriodo()));
  await carregar();
  const nomes = { MORADOR: "Minhas solicitações", OPERADOR: "Painel do operador", ADMINISTRADOR: "Solicitações" };
  return [titulo(nomes[ctx.ator.tipo]), painel(filtro), resultados];
}

export async function formularioSolicitacao(ctx, pontoId, anterior = null) {
  const [ponto, enderecos] = await Promise.all([
    ctx.pedir("/pontos/" + pontoId),
    todas(ctx.pedir, "/moradores/" + ctx.ator.id + "/enderecos"),
  ]);
  const solicitacao = anterior?.solicitacao;
  const cabecalho = titulo(solicitacao ? "Corrigir solicitação" : "Solicitar recolhimento", ponto.nome);
  if (!enderecos.length) return [cabecalho, painel(listaVazia("Cadastre um endereço para continuar."),
    h("div", {}, link("Cadastrar endereço", "conta")))];
  if (!ponto.recolhimentoDomiciliar || !ponto.materiaisAceitos.length) {
    return [cabecalho, painel(listaVazia("O ponto está indisponível para novas solicitações."))];
  }

  const opcoes = ponto.materiaisAceitos.map(v => [v.material.id, v.material.nome + " (" + unidades[v.material.unidadeMedida] + ")"]);
  const linhas = h("div", { class: "lista" });
  const adicionar = botao("Adicionar material", () => adicionarItem(), "secundario");
  let sequencia = 0;
  function atualizarItens() {
    [...linhas.children].forEach((linha, i) => {
      linha.querySelector("select").name = `itens[${i}].materialId`;
      linha.querySelector("input").name = `itens[${i}].quantidadeEstimada`;
      linha.querySelector("button").disabled = linhas.children.length === 1;
    });
    adicionar.disabled = linhas.children.length >= Math.min(100, opcoes.length);
  }
  function adicionarItem(item = {}) {
    const chave = ++sequencia;
    const material = campo("material-" + chave, "Material", { itens: opcoes, vazio: "Selecione",
      valor: item.materialId, required: true });
    const quantidade = campo("quantidade-" + chave, "Quantidade estimada", { tipo: "number",
      valor: item.quantidadeEstimada, required: true, min: .001, step: .001, max: 999999999.999 });
    const explicacao = h("p", { class: "nota" });
    const linha = h("div", { class: "interno pilha" }, h("div", { class: "grade-campos" }, material, quantidade),
      explicacao, h("div", { class: "linha" }, botao("Remover material", () => {
        linha.remove();
        atualizarItens();
      }, "secundario compacto perigo")));
    const seletor = material.querySelector("select");
    function ajustar() {
      const vinculo = ponto.materiaisAceitos.find(v => v.material.id === Number(seletor.value));
      const controle = quantidade.querySelector("input");
      const pecas = vinculo?.material.unidadeMedida === "UNIDADE";
      controle.min = pecas ? 1 : .001;
      controle.step = pecas ? 1 : .001;
      controle.max = vinculo?.limitePorSolicitacao ?? 999999999.999;
      explicacao.textContent = vinculo?.limitePorSolicitacao
        ? `Limite por solicitação de ${exibirNumero(vinculo.limitePorSolicitacao)} ${unidades[vinculo.material.unidadeMedida]}.`
        : "";
    }
    seletor.addEventListener("change", ajustar);
    ajustar();
    linhas.append(linha);
    atualizarItens();
  }
  (anterior?.itens.length ? anterior.itens : [{}]).forEach(adicionarItem);
  const enderecoAtual = anterior && enderecos.find(e => enderecoTexto(e) === enderecoTexto(anterior.endereco)
    && e.cep === anterior.endereco.cep && e.complemento === anterior.endereco.complemento);
  const form = formulario(solicitacao ? "Salvar correção" : "Enviar solicitação", async valores => {
    const corpo = { enderecoId: Number(valores.enderecoId), observacoes: valores.observacoes,
      itens: [...linhas.children].map((linha, i) => ({ materialId: Number(valores[`itens[${i}].materialId`]),
        quantidadeEstimada: Number(valores[`itens[${i}].quantidadeEstimada`]) })) };
    const dados = solicitacao
      ? await ctx.pedir("/solicitacoes/" + solicitacao.id, { metodo: "PUT", corpo: { ...corpo, versao: solicitacao.versao } })
      : await ctx.pedir("/solicitacoes", { metodo: "POST", corpo: { ...corpo, pontoId: Number(pontoId) } });
    ctx.navegar("solicitacao/" + dados.solicitacao.id, solicitacao ? "Solicitação corrigida." : "Solicitação registrada.");
  },
  campo("enderecoId", "Endereço de atendimento", { itens: enderecos.map(e => [e.id, enderecoTexto(e)]),
    vazio: "Selecione", valor: enderecoAtual?.id, required: true }),
  h("h2", {}, "Materiais"), linhas, h("div", {}, adicionar),
  campo("observacoes", "Observações", { tipo: "textarea", valor: anterior?.observacoes, maxlength: 4000 }));
  return [cabecalho, painel(form)];
}

export async function detalhe(ctx, id) {
  const dados = await ctx.pedir("/solicitacoes/" + id);
  const s = dados.solicitacao;
  const operador = ctx.ator.tipo === "OPERADOR";
  const morador = ctx.ator.tipo === "MORADOR";
  const acoes = [];
  if (morador && s.situacao === "SOLICITADA") {
    acoes.push(h("div", {}, botao("Corrigir endereço ou itens", async () => {
      const local = document.querySelector("#acoes-solicitacao");
      try {
        const tela = await formularioSolicitacao(ctx, s.pontoId, dados);
        local.replaceChildren(...tela);
      } catch (erro) {
        exibirErro(local, erro);
      }
    }, "secundario")));
  }
  if (operador && s.situacao === "SOLICITADA") {
    acoes.push(acao(ctx, s, "agendar", "Agendar", [campoData()], v => ({ dataAgendamento: new Date(v.dataAgendamento).toISOString() })));
    acoes.push(acao(ctx, s, "recusar", "Recusar", [campoMotivo()], v => ({ motivo: v.motivo })));
  }
  if (operador && s.situacao === "AGENDADA") {
    acoes.push(acao(ctx, s, "iniciar", "Iniciar recolhimento", [], () => ({})));
  }
  if (operador && ["AGENDADA", "EM_RECOLHIMENTO"].includes(s.situacao)) {
    const falha = s.situacao === "EM_RECOLHIMENTO";
    acoes.push(acao(ctx, s, "reagendar", falha ? "Registrar falha e reagendar" : "Reagendar",
      [campoData(), campoMotivo(), falha && campo("detalhesTentativa", "Detalhes da tentativa", { tipo: "textarea", maxlength: 4000 })],
      v => ({ motivo: v.motivo, detalhesTentativa: v.detalhesTentativa,
        dataAgendamento: new Date(v.dataAgendamento).toISOString() })));
  }
  if (operador && s.situacao === "EM_RECOLHIMENTO") {
    acoes.push(acao(ctx, s, "concluir", "Concluir recolhimento", dados.itens.map((i, pos) =>
      campo(`itens[${pos}].quantidadeEfetiva`, i.material + " (" + unidades[i.unidade] + ")",
        { tipo: "number", min: 0, max: 999999999.999, step: i.unidade === "UNIDADE" ? 1 : .001, required: true })),
    v => ({ itens: dados.itens.map((i, pos) => ({ materialId: i.materialId,
      quantidadeEfetiva: Number(v[`itens[${pos}].quantidadeEfetiva`]) })) })));
  }
  if (["SOLICITADA", "AGENDADA"].includes(s.situacao)) {
    acoes.push(acao(ctx, s, "cancelar", "Cancelar solicitação", [campoMotivo()], v => ({ motivo: v.motivo })));
  }

  return [titulo(s.protocolo, s.ponto),
    h("div", { class: "linha entre" }, etiqueta(s.situacao), link("Voltar às solicitações", "solicitacoes")),
    painel(h("h2", {}, "Atendimento"),
      h("dl", { class: "dados" },
        dado("Endereço", enderecoTexto(dados.endereco)), dado("CEP", dados.endereco.cep),
        dados.endereco.complemento && dado("Complemento", dados.endereco.complemento),
        dado("Criação", exibirData(s.dataCriacao)), dado("Agendamento", exibirData(s.dataAgendamento)),
        s.dataConclusao && dado("Conclusão", exibirData(s.dataConclusao)),
        dados.observacoes && dado("Observações", dados.observacoes))),
    painel(h("h2", {}, "Materiais"), h("div", { class: "grade" }, dados.itens.map(i =>
      h("article", { class: "interno pilha" }, h("h3", {}, i.material), h("dl", { class: "dados" },
        dado("Estimado", exibirNumero(i.quantidadeEstimada) + " " + unidades[i.unidade]),
        dado("Recolhido", i.quantidadeEfetiva == null ? "—" : exibirNumero(i.quantidadeEfetiva) + " " + unidades[i.unidade])))))),
    acoes.length && h("section", { class: "painel", id: "acoes-solicitacao" }, h("h2", {}, "Ações"), acoes),
    painel(h("h2", {}, "Histórico"), h("ol", {}, dados.historico.map(evento =>
      h("li", {},
        h("strong", {}, (evento.situacaoAnterior ? situacoes[evento.situacaoAnterior] + " → " : "") + situacoes[evento.situacaoNova]),
        h("p", { class: "nota" }, exibirData(evento.dataHora) + " · " + perfis[evento.responsavelTipo]
          + (evento.responsavelId ? " " + evento.responsavelId : "")),
        evento.motivo && h("p", {}, evento.motivo),
        evento.numeroTentativa && h("p", {}, "Tentativa " + evento.numeroTentativa),
        evento.detalhesTentativa && h("p", {}, evento.detalhesTentativa),
        evento.dataAgendamento && h("p", {}, "Agendamento para " + exibirData(evento.dataAgendamento))))))].filter(Boolean);
}

function acao(ctx, s, caminho, rotulo, campos, corpo) {
  return h("details", { class: "interno" }, h("summary", {}, rotulo),
    formulario(caminho === "cancelar" ? "Confirmar cancelamento" : rotulo, async valores => {
      if (caminho === "cancelar" && !window.confirm("Confirmar o cancelamento desta solicitação?")) return;
      await ctx.pedir(`/solicitacoes/${s.id}/${caminho}`, { metodo: "PATCH", corpo: { versao: s.versao, ...corpo(valores) } });
      ctx.navegar("solicitacao/" + s.id, "Solicitação atualizada.");
    }, campos));
}

function campoData() { return campo("dataAgendamento", "Data e hora futuras", { tipo: "datetime-local", required: true }); }
function campoMotivo() { return campo("motivo", "Motivo", { tipo: "textarea", required: true, maxlength: 4000 }); }
function etiqueta(situacao) { return h("span", { class: "etiqueta" }, situacoes[situacao]); }
function dado(rotulo, valor) { return h("div", {}, h("dt", {}, rotulo), h("dd", {}, valor)); }

export function camposPeriodo() {
  return [campo("inicio", "De", { tipo: "date" }), campo("fim", "Até", { tipo: "date" })];
}

export function periodo({ inicio, fim }) {
  const proximoDia = fim && new Date(fim + "T00:00:00");
  if (proximoDia) proximoDia.setDate(proximoDia.getDate() + 1);
  return { inicio: inicio ? new Date(inicio + "T00:00:00").toISOString() : null,
    fim: proximoDia ? proximoDia.toISOString() : null };
}
