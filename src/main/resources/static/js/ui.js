export const situacoes = {
  SOLICITADA: "Solicitada", AGENDADA: "Agendada", EM_RECOLHIMENTO: "Em recolhimento",
  CONCLUIDA: "Concluída", RECUSADA: "Recusada", CANCELADA: "Cancelada",
};
export const unidades = { KG: "kg", LITRO: "L", UNIDADE: "un." };
export const perfis = { MORADOR: "Morador", OPERADOR: "Operador", ADMINISTRADOR: "Administrador" };

export function h(tag, atributos = {}, ...filhos) {
  const no = document.createElement(tag);
  for (const [chave, valor] of Object.entries(atributos)) {
    if (valor === false || valor == null) continue;
    if (chave.startsWith("on")) no.addEventListener(chave.slice(2), valor);
    else no.setAttribute(chave, valor === true ? "" : String(valor));
  }
  no.append(...filhos.flat(Infinity).filter(x => x !== null && x !== undefined && x !== false));
  return no;
}

export function botao(texto, acao, classe = "") {
  return h("button", { type: "button", class: "botao " + classe, onclick: acao }, texto);
}

export function link(texto, rota, classe = "botao secundario") {
  return h("a", { href: "#" + rota, class: classe }, texto);
}

export function titulo(texto, ajuda) {
  return h("div", { class: "titulo" }, h("h1", {}, texto), ajuda && h("p", {}, ajuda));
}

export function painel(...conteudo) {
  return h("section", { class: "painel" }, conteudo);
}

export function campo(nome, rotulo, opcoes = {}) {
  const { valor, itens, vazio, tipo = "text", ...attrs } = opcoes;
  let controle;
  if (itens) {
    const opcaoVazia = vazio !== undefined ? h("option", { value: "" }, vazio) : null;
    controle = h("select", { name: nome, ...attrs }, opcaoVazia,
      itens.map(([id, texto]) => h("option", { value: id }, texto)));
    if (valor != null) controle.value = String(valor);
  } else if (tipo === "textarea") {
    controle = h("textarea", { name: nome, ...attrs }, valor ?? "");
  } else if (tipo === "checkbox") {
    controle = h("input", { name: nome, type: tipo, checked: valor === true, ...attrs });
    return h("label", { class: "campo campo-check" }, controle, rotulo);
  } else {
    controle = h("input", { name: nome, type: tipo, value: valor ?? "", ...attrs });
  }
  return h("label", { class: "campo" }, rotulo + (attrs.required ? " *" : ""), controle);
}

export function dados(form) {
  return Object.fromEntries(new FormData(form));
}

export function formulario(rotulo, salvar, ...filhos) {
  const retorno = h("div", { "aria-live": "polite" });
  const enviar = h("button", { class: "botao", type: "submit" }, rotulo);
  const form = h("form", { class: "pilha" }, filhos, retorno, h("div", { class: "linha" }, enviar));
  form.addEventListener("submit", async evento => {
    evento.preventDefault();
    form.querySelectorAll(".campo-erro").forEach(no => no.remove());
    form.querySelectorAll("[aria-invalid]").forEach(no => no.removeAttribute("aria-invalid"));
    retorno.replaceChildren();
    enviar.disabled = true;
    try {
      await salvar(dados(form), form, retorno);
    } catch (erro) {
      exibirErro(retorno, erro, form);
    } finally {
      enviar.disabled = false;
    }
  });
  return form;
}

export function exibirErro(alvo, erro, form) {
  let foco;
  const restantes = [];
  for (const { campo: nome, mensagem } of erro.campos || []) {
    const controle = form?.elements.namedItem(nome);
    if (controle instanceof HTMLElement && controle.closest("label")) {
      controle.setAttribute("aria-invalid", "true");
      controle.closest("label").append(h("small", { class: "campo-erro" }, mensagem));
      foco ||= controle;
    } else {
      restantes.push(mensagem);
    }
  }
  const mensagem = erro.codigo === "CONFLITO_VERSAO"
    ? "A solicitação mudou. Atualize a página antes de tentar novamente." : erro.message;
  alvo.replaceChildren(h("div", { class: "erro", role: "alert" }, mensagem,
    restantes.length > 0 && h("ul", {}, restantes.map(texto => h("li", {}, texto)))));
  foco?.focus();
}

export function paginador(pagina, carregar) {
  if (pagina.totalPaginas < 2) return null;
  const anterior = botao("Anterior", () => carregar(pagina.pagina - 1), "secundario compacto");
  const proxima = botao("Próxima", () => carregar(pagina.pagina + 1), "secundario compacto");
  anterior.disabled = pagina.pagina === 0;
  proxima.disabled = pagina.pagina + 1 >= pagina.totalPaginas;
  return h("div", { class: "linha entre" }, anterior,
    h("span", { class: "nota" }, `Página ${pagina.pagina + 1} de ${pagina.totalPaginas}`), proxima);
}

export function listaVazia(mensagem) {
  return h("p", { class: "nota" }, mensagem);
}

export function exibirData(data) {
  return data ? new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(new Date(data)) : "—";
}

export function exibirNumero(numero) {
  return numero == null ? "—" : new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 3 }).format(numero);
}

export function enderecoTexto(endereco) {
  return `${endereco.logradouro}, ${endereco.semNumero ? "s/n" : endereco.numero} · ${endereco.bairro}, ${endereco.municipio}–${endereco.uf}`;
}

export function camposEndereco(valor = {}, prefixo = "") {
  return h("div", { class: "grade-campos" },
    campo(prefixo + "logradouro", "Logradouro", { valor: valor.logradouro, required: true, maxlength: 160 }),
    campo(prefixo + "numero", "Número", { valor: valor.numero, maxlength: 10 }),
    campo(prefixo + "bairro", "Bairro", { valor: valor.bairro, required: true, maxlength: 80 }),
    campo(prefixo + "municipio", "Município", { valor: valor.municipio, required: true, maxlength: 80 }),
    campo(prefixo + "uf", "UF", { valor: valor.uf, required: true, maxlength: 2, pattern: "[A-Za-z]{2}" }),
    campo(prefixo + "cep", "CEP", { valor: valor.cep, required: true, inputmode: "numeric", pattern: "[0-9]{5}-?[0-9]{3}" }),
    campo(prefixo + "complemento", "Complemento", { valor: valor.complemento, maxlength: 80 }),
    campo(prefixo + "semNumero", "Sem número", { tipo: "checkbox", valor: valor.semNumero }));
}

export function lerEndereco(valores, prefixo = "") {
  const endereco = Object.fromEntries(["logradouro", "numero", "bairro", "municipio", "uf", "cep", "complemento"]
    .map(nome => [nome, valores[prefixo + nome]]));
  return { ...endereco, semNumero: Boolean(valores[prefixo + "semNumero"]) };
}
