import { camposEndereco, campo, enderecoTexto, formulario, h, lerEndereco, listaVazia,
  paginador, painel, titulo } from "./ui.js";

function camposMorador(morador = {}) {
  return h("div", { class: "grade-campos" },
    campo("nome", "Nome", { valor: morador.nome, required: true, maxlength: 120, autocomplete: "name" }),
    campo("email", "E-mail", { valor: morador.email, tipo: "email", required: true, maxlength: 160, autocomplete: "email" }),
    campo("telefone", "Telefone", { valor: morador.telefone, tipo: "tel", maxlength: 20, autocomplete: "tel" }));
}

export function cadastro(ctx) {
  return [titulo("Cadastro de morador", "Use dados fictícios para testar o sistema."),
    painel(formulario("Cadastrar", async corpo => {
      const morador = await ctx.pedir("/moradores", { metodo: "POST", corpo });
      ctx.usarAtor({ tipo: "MORADOR", id: morador.id });
      ctx.navegar("conta", "Cadastro criado. Adicione um endereço para solicitar recolhimento.");
    }, camposMorador()))];
}

export async function conta(ctx) {
  const caminho = "/moradores/" + ctx.ator.id;
  const morador = await ctx.pedir(caminho);
  const retorno = h("div", { class: "pilha" });
  const formEndereco = formulario("Adicionar endereço", async (valores, form, saida) => {
    await ctx.pedir(caminho + "/enderecos", { metodo: "POST", corpo: lerEndereco(valores) });
    form.reset();
    saida.append(h("p", { class: "sucesso", role: "status" }, "Endereço cadastrado."));
    await carregar();
  }, camposEndereco());

  async function carregar(page = 0) {
    const lista = await ctx.pedir(caminho + "/enderecos", { consulta: { page, size: 8 } });
    retorno.replaceChildren(...[
      lista.conteudo.length ? h("div", { class: "lista" }, lista.conteudo.map(e =>
        h("details", { class: "interno" }, h("summary", {}, enderecoTexto(e)),
          formulario("Salvar endereço", async (valores, form, saida) => {
            const salvo = await ctx.pedir(caminho + "/enderecos/" + e.id, { metodo: "PUT", corpo: lerEndereco(valores) });
            form.closest("details").querySelector("summary").textContent = enderecoTexto(salvo);
            saida.append(h("p", { class: "sucesso", role: "status" }, "Endereço atualizado."));
          }, camposEndereco(e)))))
        : listaVazia("Você ainda não tem endereços cadastrados."),
      paginador(lista, pagina => carregar(pagina).catch(erro =>
        retorno.replaceChildren(h("p", { class: "erro", role: "alert" }, erro.message)))),
    ].filter(Boolean));
  }

  await carregar();
  return [titulo("Meu cadastro"),
    painel(h("h2", {}, "Dados do morador"),
      formulario("Salvar dados", async (corpo, form, saida) => {
        await ctx.pedir(caminho, { metodo: "PUT", corpo });
        saida.append(h("p", { class: "sucesso", role: "status" }, "Cadastro atualizado."));
      }, camposMorador(morador))),
    painel(h("h2", {}, "Meus endereços"), retorno),
    painel(h("h2", {}, "Novo endereço"), formEndereco)];
}
