import { todas } from "./api.js";
import { botao, campo, camposEndereco, enderecoTexto, exibirErro, exibirNumero, formulario,
  h, lerEndereco, listaVazia, paginador, painel, situacoes, titulo, unidades } from "./ui.js";
import { camposPeriodo, periodo } from "./solicitacoes.js";

export function materiaisAdmin(ctx) {
  return catalogo(ctx, "Materiais", "Novo material", "/admin/materiais", "/materiais", camposMaterial,
    v => v, material => material.nome, (m, recarregar, estado) =>
      botaoApi(ctx, `/materiais/${m.id}/${m.ativo ? "inativar" : "reativar"}`,
        m.ativo ? "Inativar material" : "Reativar material", "PATCH", recarregar, estado));
}

function camposMaterial(m = {}) {
  return h("div", { class: "grade-campos" },
    campo("nome", "Nome", { valor: m.nome, required: true, maxlength: 80 }),
    campo("unidadeMedida", "Unidade", { valor: m.unidadeMedida || "KG",
      itens: [["KG", "Quilograma (kg)"], ["LITRO", "Litro (L)"], ["UNIDADE", "Peça (un.)"]], required: true }),
    campo("descricao", "Descrição", { tipo: "textarea", valor: m.descricao, maxlength: 4000 }),
    campo("orientacaoPreparo", "Orientação de preparo", { tipo: "textarea", valor: m.orientacaoPreparo, maxlength: 4000 }));
}

export async function pontosAdmin(ctx) {
  return catalogo(ctx, "Gerenciar pontos", "Novo ponto", "/admin/pontos", "/pontos", camposPonto, valores => ({
    nome: valores.nome, endereco: lerEndereco(valores, "endereco."), contato: valores.contato, horarios: valores.horarios,
    ativo: Boolean(valores.ativo), recolhimentoDomiciliar: Boolean(valores.recolhimentoDomiciliar),
  }), ponto => ponto.nome + " · " + enderecoTexto(ponto.endereco),
  (ponto, recarregar, estado) => [
    botaoApi(ctx, `/pontos/${ponto.id}/${ponto.ativo ? "inativar" : "reativar"}`,
      ponto.ativo ? "Inativar ponto" : "Reativar ponto", "PATCH", recarregar, estado),
    materiaisAceitos(ctx, ponto, recarregar),
  ]);
}

function camposPonto(p = { ativo: true }) {
  return h("div", { class: "pilha" },
    h("div", { class: "grade-campos" },
      campo("nome", "Nome", { valor: p.nome, required: true, maxlength: 120 }),
      campo("contato", "Contato público", { valor: p.contato, maxlength: 80 }),
      campo("horarios", "Horários", { valor: p.horarios, maxlength: 200 }),
      campo("ativo", "Ponto ativo", { tipo: "checkbox", valor: p.ativo }),
      campo("recolhimentoDomiciliar", "Recolhimento domiciliar", { tipo: "checkbox", valor: p.recolhimentoDomiciliar })),
    camposEndereco(p.endereco, "endereco."));
}

function materiaisAceitos(ctx, ponto, recarregar) {
  const alvo = h("div", { class: "pilha" });
  const bloco = h("details", { class: "interno" }, h("summary", {}, "Materiais aceitos"), alvo);
  const caminho = `/pontos/${ponto.id}/materiais`;
  const estado = h("div", { "aria-live": "polite" });
  let carregado = false;
  bloco.addEventListener("toggle", async () => {
    if (!bloco.open || carregado) return;
    carregado = true;
    try {
      const materiais = await todas(ctx.pedir, "/materiais");
      const restantes = materiais.filter(m => !ponto.materiaisAceitos.some(v => v.material.id === m.id));
      alvo.replaceChildren(
        ...ponto.materiaisAceitos.map(v => h("div", { class: "painel" }, h("h3", {}, v.material.nome),
          formulario("Salvar vínculo", async valores => {
            await ctx.pedir(caminho + "/" + v.material.id, { metodo: "PUT", corpo: lerVinculo(valores) });
            await recarregar(ponto.id, true);
          }, camposVinculo(v)),
          h("div", {}, botaoApi(ctx, caminho + "/" + v.material.id, "Remover vínculo", "DELETE",
            () => recarregar(ponto.id, true), estado)))),
        restantes.length ? formulario("Vincular material", async valores => {
          await ctx.pedir(caminho, { metodo: "POST", corpo: { ...lerVinculo(valores), materialId: Number(valores.materialId) } });
          await recarregar(ponto.id, true);
        }, campo("materialId", "Material", { itens: restantes.map(m => [m.id, m.nome]), vazio: "Selecione", required: true }),
        camposVinculo()) : listaVazia("Todos os materiais ativos estão vinculados."), estado);
    } catch (erro) {
      carregado = false;
      exibirErro(alvo, erro);
    }
  });
  return bloco;
}

function camposVinculo(v = {}) {
  return h("div", { class: "grade-campos" },
    campo("limitePorSolicitacao", "Limite por solicitação", { valor: v.limitePorSolicitacao,
      tipo: "number", min: .001, step: .001, max: 999999999.999 }),
    campo("observacoes", "Observações", { valor: v.observacoes, maxlength: 200 }));
}
function lerVinculo(v) { return { observacoes: v.observacoes, limitePorSolicitacao: v.limitePorSolicitacao ? Number(v.limitePorSolicitacao) : null }; }

export async function operadoresAdmin(ctx) {
  const pontos = await todas(ctx.pedir, "/admin/pontos");
  return catalogo(ctx, "Operadores", "Novo operador", "/operadores", "/operadores", camposOperador,
    v => ({ ...v, ativo: Boolean(v.ativo) }), o => o.nome, (o, recarregar, estado) => {
      const disponiveis = pontos.filter(p => !o.pontosIds.includes(p.id));
      return h("div", { class: "pilha separador" }, h("h3", {}, "Pontos atribuídos"),
        ...o.pontosIds.map(id => h("div", { class: "linha entre" }, h("span", {}, pontos.find(p => p.id === id)?.nome || "Ponto " + id),
          botaoApi(ctx, `/operadores/${o.id}/pontos/${id}`, "Remover ponto", "DELETE", recarregar, estado))),
        disponiveis.length > 0 && formulario("Vincular ponto", async ({ pontoId }) => {
          await ctx.pedir(`/operadores/${o.id}/pontos/${pontoId}`, { metodo: "PUT" });
          await recarregar();
        }, campo("pontoId", "Ponto de coleta", { itens: disponiveis.map(p => [p.id, p.nome]), vazio: "Selecione", required: true })));
    });
}

function camposOperador(o = { ativo: true }) {
  return h("div", { class: "grade-campos" },
    campo("nome", "Nome", { valor: o.nome, required: true, maxlength: 120 }),
    campo("email", "E-mail", { valor: o.email, tipo: "email", required: true, maxlength: 160 }),
    campo("ativo", "Operador ativo", { tipo: "checkbox", valor: o.ativo }));
}

async function catalogo(ctx, nome, novo, consulta, caminho, campos, ler, rotulo, extras) {
  const listagem = h("div", { class: "pilha" });
  const feedback = h("div", { "aria-live": "polite" });
  let atual = 0;

  async function carregar(page = atual, aberto, abrirVinculo) {
    const lista = await ctx.pedir(consulta, { consulta: { page, size: 8 } });
    atual = lista.pagina;
    listagem.replaceChildren(...[
      lista.conteudo.length ? h("div", { class: "lista" }, lista.conteudo.map(item => {
        const recarregar = async (id = item.id, vinculo = false) => {
          await carregar(atual, id, vinculo);
          feedback.replaceChildren(h("p", { class: "sucesso", role: "status" }, "Alteração salva."));
        };
        const acoes = extras(item, recarregar, feedback);
        const detalhe = h("details", { class: "interno", open: aberto === item.id },
          h("summary", {}, rotulo(item), " · ", item.ativo ? "Ativo" : "Inativo"),
          h("div", { class: "pilha" },
            formulario("Salvar alterações", async valores => {
              await ctx.pedir(caminho + "/" + item.id, { metodo: "PUT", corpo: ler(valores) });
              await recarregar();
            }, campos(item)), acoes));
        if (aberto === item.id && abrirVinculo) {
          const vinculo = detalhe.querySelector("details");
          if (vinculo) vinculo.open = true;
        }
        return detalhe;
      })) : listaVazia("Nenhum cadastro encontrado."),
      paginador(lista, page => carregar(page).catch(erro => exibirErro(feedback, erro))),
    ].filter(Boolean));
  }

  await carregar();
  return [titulo(nome), painel(h("h2", {}, novo), formulario("Cadastrar", async (valores, form, retorno) => {
    await ctx.pedir(caminho, { metodo: "POST", corpo: ler(valores) });
    form.reset();
    await carregar(0);
    retorno.append(h("p", { class: "sucesso", role: "status" }, "Cadastro criado."));
  }, campos())), painel(h("h2", {}, "Cadastros"), feedback, listagem)];
}

function botaoApi(ctx, caminho, rotulo, metodo, recarregar, estado) {
  const acao = botao(rotulo, async () => {
    acao.disabled = true;
    try {
      await ctx.pedir(caminho, { metodo });
      await recarregar();
    } catch (erro) {
      exibirErro(estado, erro);
    } finally {
      acao.disabled = false;
    }
  }, "secundario compacto");
  return acao;
}

export async function indicadores(ctx) {
  const resultados = h("div", { class: "pilha", "aria-live": "polite" });
  async function carregar(consulta = {}) {
    const dados = await ctx.pedir("/indicadores", { consulta });
    resultados.replaceChildren(
      painel(h("h2", {}, "Solicitações por situação"), h("div", { class: "grade" },
        Object.entries(situacoes).map(([k, v]) => h("div", { class: "interno indicador" },
          h("p", {}, v), h("strong", {}, exibirNumero(dados.solicitacoesPorSituacao[k])))))),
      painel(h("h2", {}, "Quantidades recolhidas"), dados.recolhidosPorMaterial.length
        ? h("div", { class: "grade" }, dados.recolhidosPorMaterial.map(m => h("div", { class: "interno indicador" },
          h("p", {}, m.material), h("strong", {}, exibirNumero(m.quantidade) + " " + unidades[m.unidade]))))
        : listaVazia("Nenhum recolhimento concluído no período.")));
  }
  await carregar();
  return [titulo("Indicadores"), painel(formulario("Filtrar período", v => carregar(periodo(v)),
    h("div", { class: "grade-campos" }, camposPeriodo()))), resultados];
}
