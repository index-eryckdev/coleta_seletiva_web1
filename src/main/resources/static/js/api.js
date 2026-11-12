export class ErroApi extends Error {
  constructor(status, dados) {
    super(dados.mensagem || "A operação não pôde ser concluída.");
    this.status = status;
    this.codigo = dados.codigo;
    this.campos = dados.erros || [];
  }
}

export async function api(caminho, { ator, metodo = "GET", corpo, consulta, sinal } = {}) {
  const url = new URL("/api" + caminho, location.origin);
  for (const [chave, valor] of Object.entries(consulta || {})) {
    if (valor !== null && valor !== undefined && valor !== "") url.searchParams.set(chave, valor);
  }
  const headers = { Accept: "application/json" };
  if (ator?.tipo) {
    headers["X-Ator-Tipo"] = ator.tipo;
    if (ator.id) headers["X-Ator-Id"] = ator.id;
  }
  if (corpo !== undefined) headers["Content-Type"] = "application/json";
  let resposta;
  try {
    resposta = await fetch(url, { method: metodo, headers, body: JSON.stringify(corpo), signal: sinal });
  } catch (erro) {
    if (erro.name === "AbortError") throw erro;
    throw new Error("A API está indisponível. Confira se o servidor está em execução.");
  }
  if (resposta.status === 204) return null;
  const dados = await resposta.json().catch(() => ({}));
  if (!resposta.ok) throw new ErroApi(resposta.status, dados);
  return dados;
}

export async function todas(pedir, caminho, consulta = {}) {
  const primeira = await pedir(caminho, { consulta: { ...consulta, size: 100 } });
  const registros = [...primeira.conteudo];
  for (let page = 1; page < primeira.totalPaginas; page++) {
    const proxima = await pedir(caminho, { consulta: { ...consulta, size: 100, page } });
    registros.push(...proxima.conteudo);
  }
  return registros;
}
