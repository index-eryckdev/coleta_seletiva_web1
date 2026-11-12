import { api, todas } from "./api.js";
import { campo, enderecoTexto, exibirErro, exibirNumero, formulario, h, link, listaVazia,
  paginador, painel, perfis, unidades } from "./ui.js";
import { cadastro, conta } from "./morador.js";
import { detalhe, listaSolicitacoes, formularioSolicitacao } from "./solicitacoes.js";
import { indicadores, materiaisAdmin, operadoresAdmin, pontosAdmin } from "./admin.js";

const main = document.querySelector("main");
const dialogo = document.querySelector("#seletor-perfil");
const perfil = document.querySelector("#perfil");
const formPerfil = document.querySelector("#form-perfil");
const tipo = document.querySelector("#tipo-perfil");
const id = document.querySelector("#id-perfil");
let ator = lerPerfil();
let operacao;
let aviso;

function lerPerfil() {
  try {
    const salvo = JSON.parse(sessionStorage.getItem("coleta-perfil"));
    if (salvo && Object.hasOwn(perfis, salvo.tipo)) return salvo;
  } catch {
    // A seleção funciona mesmo com o armazenamento bloqueado.
  }
  return {};
}

function usarAtor(novo) {
  ator = novo;
  try {
    sessionStorage.setItem("coleta-perfil", JSON.stringify(ator));
  } catch {
    // A seleção fica na memória desta página.
  }
}

function atualizarCamposPerfil(mudou) {
  const individual = tipo.value === "MORADOR" || tipo.value === "OPERADOR";
  document.querySelector("#campo-id").hidden = !individual;
  id.required = individual;
  if (mudou) id.value = individual ? "1" : "";
  document.querySelector("#ajuda-perfil").textContent = tipo.value === "MORADOR"
    ? "Exemplos: Ana (1) e Carlos (2)." : "Exemplos: Maria (1) e João (2).";
}

perfil.addEventListener("click", () => {
  tipo.value = ator.tipo || "";
  id.value = ator.id || "";
  atualizarCamposPerfil(false);
  dialogo.showModal();
});
tipo.addEventListener("change", () => atualizarCamposPerfil(true));
document.querySelector("#fechar-perfil").addEventListener("click", () => dialogo.close());
document.querySelector("#abrir-cadastro").addEventListener("click", () => dialogo.close());
formPerfil.addEventListener("submit", evento => {
  evento.preventDefault();
  usarAtor({ tipo: tipo.value, id: Number(id.value) || null });
  dialogo.close();
  navegar("pontos");
});

function menu(rota) {
  const itens = [["pontos", "Pontos de coleta"]];
  if (ator.tipo === "MORADOR") itens.push(["conta", "Meu cadastro"], ["solicitacoes", "Minhas solicitações"]);
  if (ator.tipo === "OPERADOR") itens.push(["solicitacoes", "Painel do operador"]);
  if (ator.tipo === "ADMINISTRADOR") itens.push(["materiais", "Materiais"], ["administrar-pontos", "Gerenciar pontos"],
    ["operadores", "Operadores"], ["solicitacoes", "Solicitações"], ["indicadores", "Indicadores"]);
  if (!ator.tipo) itens.push(["cadastro", "Cadastrar morador"]);
  document.querySelector("nav").replaceChildren(...itens.map(([chave, texto]) =>
    h("a", { href: "#" + chave, "aria-current": rota === chave ? "page" : null }, texto)));
  perfil.textContent = ator.tipo ? perfis[ator.tipo] + (ator.id ? " " + ator.id : "") : "Escolher perfil";
}

function navegar(rota, mensagem) {
  aviso = mensagem;
  if (location.hash === "#" + rota) renderizar();
  else location.hash = rota;
}

const rotas = {
  pontos: { tela: consultaPublica },
  cadastro: { tela: cadastro },
  conta: { tela: conta, papeis: ["MORADOR"] },
  solicitacoes: { tela: listaSolicitacoes, papeis: Object.keys(perfis) },
  solicitacao: { tela: detalhe, papeis: Object.keys(perfis) },
  solicitar: { tela: formularioSolicitacao, papeis: ["MORADOR"] },
  materiais: { tela: materiaisAdmin, papeis: ["ADMINISTRADOR"] },
  "administrar-pontos": { tela: pontosAdmin, papeis: ["ADMINISTRADOR"] },
  operadores: { tela: operadoresAdmin, papeis: ["ADMINISTRADOR"] },
  indicadores: { tela: indicadores, papeis: ["ADMINISTRADOR"] },
};

async function renderizar() {
  operacao?.abort();
  operacao = new AbortController();
  const { signal: sinal } = operacao;
  const [chave, argumento] = (location.hash.slice(1) || "pontos").split("/");
  const rota = Object.hasOwn(rotas, chave) ? rotas[chave] : rotas.pontos;
  const avisoAtual = aviso;
  aviso = undefined;
  const contexto = {
    ator, navegar, usarAtor, escolherPerfil: () => perfil.click(),
    pedir: (caminho, opcoes = {}) => api(caminho, { ator, sinal, ...opcoes }),
  };
  menu(chave);
  main.setAttribute("aria-busy", "true");
  main.replaceChildren(listaVazia("Carregando…"));
  try {
    const permitido = !rota.papeis || rota.papeis.includes(ator.tipo);
    const conteudo = permitido
      ? await rota.tela(contexto, argumento)
      : painel(h("h1", {}, "Escolha um perfil"), h("p", {}, "Selecione um perfil de demonstração para acessar esta área."));
    if (!sinal.aborted) {
      main.replaceChildren(...[avisoAtual && h("div", { role: "status", class: "sucesso" }, avisoAtual),
        ...[conteudo].flat()].filter(Boolean));
      main.setAttribute("aria-busy", "false");
      document.title = (main.querySelector("h1")?.textContent || "Coleta Seletiva") + " | IFS";
    }
  } catch (erro) {
    if (!sinal.aborted) {
      exibirErro(main, erro);
      main.setAttribute("aria-busy", "false");
    }
  }
}

async function consultaPublica(ctx) {
  const materiais = await todas(ctx.pedir, "/materiais");
  const resultados = h("div", { class: "pilha", "aria-live": "polite" });
  let consulta = {};
  let chamada = 0;

  async function carregar(page = 0) {
    const atual = ++chamada;
    resultados.replaceChildren(listaVazia("Buscando pontos…"));
    try {
      const dados = await ctx.pedir("/pontos", { consulta: { ...consulta, page, size: 6 } });
      if (atual !== chamada) return;
      resultados.replaceChildren(
        h("h2", {}, dados.totalElementos === 1 ? "1 ponto encontrado" : `${dados.totalElementos} pontos encontrados`),
        dados.conteudo.length ? h("div", { class: "grade" }, dados.conteudo.map(p => cartaoPonto(p, ctx)))
          : listaVazia("Nenhum ponto corresponde aos filtros."),
        ...[paginador(dados, carregar)].filter(Boolean));
    } catch (erro) {
      if (atual === chamada && erro.name !== "AbortError") exibirErro(resultados, erro);
    }
  }

  const filtro = formulario("Buscar pontos", async valores => {
    consulta = valores;
    await carregar();
  }, h("div", { class: "grade-campos" },
    campo("nome", "Nome do ponto", { placeholder: "Ex. EcoVida" }),
    campo("municipio", "Município", { placeholder: "Ex. Lagarto" }),
    campo("bairro", "Bairro"),
    campo("materialId", "Material aceito", { itens: materiais.map(m => [m.id, m.nome]), vazio: "Todos" }),
    campo("recolhimentoDomiciliar", "Recolhimento domiciliar", { itens: [["true", "Disponível"], ["false", "Somente entrega"]], vazio: "Todos" })));
  await carregar();
  return [
    h("section", { class: "hero" },
      h("div", { class: "hero-texto" }, h("h1", {}, "Encontre pontos de coleta"),
        h("p", {}, "Veja onde entregar seus recicláveis, como preparar cada material e quais pontos fazem recolhimento domiciliar.")),
      h("div", { class: "hero-arte", "aria-hidden": "true" }, h("span", {}, "♻"))),
    painel(h("h2", {}, "Pesquisar pontos"), filtro),
    resultados,
    painel(h("h2", {}, "Materiais recicláveis"), materiais.length
      ? h("div", { class: "grade" }, materiais.map(m => h("article", { class: "interno pilha" },
        h("h3", {}, m.nome), m.descricao && h("p", {}, m.descricao),
        h("p", { class: "nota" }, m.orientacaoPreparo || "Consulte a orientação com o ponto de coleta."))))
      : listaVazia("Nenhum material ativo cadastrado.")),
  ];
}

function cartaoPonto(ponto, ctx) {
  return h("article", { class: "cartao" },
    h("div", { class: "linha entre" }, h("h3", {}, ponto.nome),
      h("span", { class: "etiqueta" }, ponto.recolhimentoDomiciliar ? "Recolhimento em casa" : "Entrega no local")),
    h("p", {}, enderecoTexto(ponto.endereco)),
    ponto.horarios && h("p", { class: "nota" }, ponto.horarios),
    ponto.contato && h("p", { class: "nota" }, ponto.contato),
    h("details", { class: "separador" }, h("summary", {}, "Materiais aceitos"),
      ponto.materiaisAceitos.length ? h("ul", {}, ponto.materiaisAceitos.map(v =>
        h("li", {}, h("strong", {}, v.material.nome),
          v.limitePorSolicitacao && ` · até ${exibirNumero(v.limitePorSolicitacao)} ${unidades[v.material.unidadeMedida]}`,
          v.observacoes && h("p", { class: "nota" }, v.observacoes),
          v.material.orientacaoPreparo && h("p", { class: "nota" }, v.material.orientacaoPreparo))))
        : listaVazia("Nenhum material ativo neste ponto.")),
    ponto.recolhimentoDomiciliar && ctx.ator.tipo === "MORADOR" &&
      h("div", { class: "linha" }, link("Solicitar recolhimento", "solicitar/" + ponto.id)));
}

window.addEventListener("hashchange", () => {
  if (location.hash !== "#conteudo") renderizar();
});
renderizar();
