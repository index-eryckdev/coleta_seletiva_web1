const assert = require("node:assert/strict");
const { mkdir } = require("node:fs/promises");
const { join } = require("node:path");
const { chromium } = require("playwright");

const url = process.env.COLETA_BASE_URL || "http://127.0.0.1:8080";
const pasta = process.env.COLETA_EVIDENCIAS;

async function totalPapel() {
  const resposta = await fetch(url + "/api/indicadores", { headers: { "X-Ator-Tipo": "ADMINISTRADOR" } });
  if (!resposta.ok) throw new Error("Falha ao consultar os indicadores.");
  const dados = await resposta.json();
  return dados.recolhidosPorMaterial.find(m => m.materialId === 1 && m.unidade === "KG")?.quantidade || 0;
}

(async () => {
  if (pasta) await mkdir(pasta, { recursive: true });
  const papelInicial = await totalPapel();
  const browser = await chromium.launch({ executablePath: process.env.CHROME_PATH || "/usr/bin/google-chrome", headless: true,
    args: ["--disable-dev-shm-usage", "--renderer-process-limit=2"] });
  try {
    const saida = [];
    for (const width of [1366, 360]) {
      const context = await browser.newContext({ viewport: { width, height: width === 360 ? 800 : 950 },
        timezoneId: "America/Fortaleza", locale: "pt-BR" });
      const page = await context.newPage();
      const erros = [];
      page.on("pageerror", e => erros.push(e.message));
      await page.goto(url);
      const heading = (nome) => page.getByRole("heading", { name: nome, exact: true }).first().waitFor();
      const nav = (nome) => page.getByRole("navigation").getByRole("link", { name: nome, exact: true }).click();
      const ajustarPerfil = async perfil => {
        await page.locator("#perfil").click();
        await page.locator("#tipo-perfil").selectOption(perfil);
        await page.getByRole("button", { name: "Usar perfil" }).click();
        await heading("Encontre pontos de coleta");
      };
      const verificar = async nome => {
        await heading(nome);
        const medidas = await page.evaluate(() => [document.documentElement.clientWidth, document.documentElement.scrollWidth]);
        assert.equal(medidas[0], width);
        assert.equal(medidas[1], width, `Rolagem horizontal em ${nome} a ${width}px`);
      };
      await verificar("Encontre pontos de coleta");
      if (pasta) await page.screenshot({ path: join(pasta, `interface-${width}.png`) });
      await page.getByLabel("Município", { exact: true }).fill("Lagarto");
      await page.getByRole("button", { name: "Buscar pontos" }).click();
      await heading("1 ponto encontrado");
      await heading("Recicla Lagarto");
      await ajustarPerfil("MORADOR");
      await nav("Meu cadastro");
      await verificar("Meu cadastro");
      await nav("Pontos de coleta");
      await page.locator("article.cartao").filter({ hasText: "Cooperativa EcoVida" })
        .getByRole("link", { name: "Solicitar recolhimento" }).click();
      await verificar("Solicitar recolhimento");
      await page.getByLabel("Endereço de atendimento *").selectOption({ index: 1 });
      await page.locator('[name="itens[0].materialId"]').selectOption("1");
      await page.locator('[name="itens[0].quantidadeEstimada"]').fill("51");
      assert.equal(await page.locator('[name="itens[0].quantidadeEstimada"]').evaluate(input => input.validity.rangeOverflow), true);
      await page.locator('[name="itens[0].quantidadeEstimada"]').fill("12.5");
      await page.getByRole("button", { name: "Adicionar material" }).click();
      await page.locator('[name="itens[1].materialId"]').selectOption("2");
      await page.locator('[name="itens[1].quantidadeEstimada"]').fill("8");
      await page.getByRole("button", { name: "Enviar solicitação" }).click();
      const protocolo = await page.getByRole("heading", { level: 1, name: /^CS-/ }).textContent();
      await verificar(protocolo);
      await ajustarPerfil("OPERADOR");
      await nav("Painel do operador");
      await verificar("Painel do operador");
      await page.locator("article.cartao").filter({ hasText: protocolo }).getByRole("link", { name: "Ver detalhes" }).click();
      await verificar(protocolo);
      const abrir = async nome => page.locator("summary").getByText(nome, { exact: true }).click();
      await abrir("Agendar");
      const futuro = new Date(Date.now() + 7 * 86_400_000);
      await page.getByLabel("Data e hora futuras *").fill(futuro.toISOString().slice(0, 16));
      await page.getByRole("button", { name: "Agendar", exact: true }).click();
      await page.locator(".etiqueta").getByText("Agendada", { exact: true }).waitFor();

      if (width === 360) {
        await ajustarPerfil("MORADOR");
        await nav("Minhas solicitações");
        await verificar("Minhas solicitações");
        await page.locator("article.cartao").filter({ hasText: protocolo }).getByRole("link", { name: "Ver detalhes" }).click();
        await abrir("Cancelar solicitação");
        await page.getByLabel("Motivo *").fill("Teste de cancelamento");
        page.once("dialog", dialog => dialog.accept());
        await page.getByRole("button", { name: "Confirmar cancelamento" }).click();
        await page.locator(".etiqueta").getByText("Cancelada", { exact: true }).waitFor();
      } else {
        await abrir("Iniciar recolhimento");
        await page.getByRole("button", { name: "Iniciar recolhimento", exact: true }).click();
        await page.locator(".etiqueta").getByText("Em recolhimento", { exact: true }).waitFor();
        await abrir("Concluir recolhimento");
        await page.getByLabel("Papel (kg) *").fill("10");
        await page.getByLabel("Plástico (kg) *").fill("0");
        await page.getByRole("button", { name: "Concluir recolhimento", exact: true }).click();
        await page.locator(".etiqueta").getByText("Concluída", { exact: true }).waitFor();
      }

      await ajustarPerfil("ADMINISTRADOR");
      await nav("Indicadores");
      await verificar("Indicadores");
      await heading("Quantidades recolhidas");
      const total = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 3 }).format(papelInicial + 10);
      const indicadorPapel = page.locator(".indicador").filter({ has: page.getByText("Papel", { exact: true }) });
      await indicadorPapel.getByText(total + " kg", { exact: true }).waitFor();
      await nav("Materiais");
      await verificar("Materiais");
      await nav("Gerenciar pontos");
      await verificar("Gerenciar pontos");
      await nav("Operadores");
      await verificar("Operadores");
      assert.deepEqual(erros, []);
      saida.push({ viewport_px: width, protocolo, fluxo: width === 360 ? "cancelamento confirmado" : "conclusão com quantidades efetivas",
        rolagem_horizontal: false, erros_javascript: erros.length });
      await context.close();
    }
    console.log(JSON.stringify({ navegador: browser.version(), casos: saida }, null, 2));
  } finally {
    await browser.close();
  }
})().catch(erro => { console.error(erro); process.exitCode = 1; });
