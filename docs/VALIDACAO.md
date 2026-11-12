# Resultados das validações

Validação local realizada em 12 de novembro de 2026. Ambiente com Fedora Linux 44, Intel Core i5-13420H, 16 GiB de RAM, Temurin 21.0.13, Maven 3.10.0 e PostgreSQL 17.11 em contêiner descartável.

## Integração da API

O comando `mvn verify` compilou, executou 7 testes de integração sem falhas e empacotou o JAR. A suíte usa JUnit, MockMvc e PostgreSQL e verifica:

| Cenário | Resultado verificado |
| --- | --- |
| Consulta pública e paginação | Filtros por município e material, limite de 100 e ausência de dados pessoais |
| Cadastros | E-mail normalizado e único, endereço completo, nome de material único e limites positivos |
| Solicitação com itens | Validação de compatibilidade, atendimento domiciliar, propriedade do endereço, quantidades e duplicação |
| Falha transacional | Um gatilho provoca falha na gravação do histórico. O banco confirma rollback da solicitação, dos itens e do evento |
| Preservação | Correção aumenta a versão. Alterar o cadastro do material preserva a unidade do item. O pedido aberto bloqueia inativação e remoção do vínculo |
| Atendimento | Agendamento futuro, escopo do operador, tentativa sem sucesso, conclusão com quantidade zero em um item e total efetivo por unidade |
| Concorrência | Duas requisições usam a mesma versão. Uma retorna 200 e a outra 409. O histórico ganha um único evento |

## Interface e persistência

O ensaio com Playwright e Google Chrome 156 percorreu pesquisa pública, cadastro do morador, formulário com dois materiais, painel do operador, agendamento e indicadores. A execução em 1366 px concluiu um recolhimento e conferiu 10 kg no resumo. A execução em 360 px confirmou o cancelamento de uma solicitação agendada. As telas verificadas nas duas larguras não apresentaram rolagem horizontal nem erros JavaScript.

- [Resultado do teste no Chrome](evidencias/interface.json)
- [Captura em 1366 px](evidencias/interface-1366.png)
- [Captura em 360 px](evidencias/interface-360.png)

Para reproduzir o teste no Chrome, inicie a aplicação com um banco descartável com os dados da migração V2. Com Node.js 20+ e Google Chrome disponíveis:

```bash
teste_web=$(mktemp -d)
npm install --prefix "$teste_web" --no-save playwright
NODE_PATH="$teste_web/node_modules" node scripts/verificar-interface.cjs
```

As variáveis opcionais `COLETA_BASE_URL`, `CHROME_PATH` e `COLETA_EVIDENCIAS` definem a URL da aplicação, o executável do navegador e o diretório de capturas.

Após reiniciar a API com o mesmo banco do ensaio de desempenho, o GET da solicitação 19 retornou o protocolo `CS-000000000019`, situação `CONCLUIDA`, dois itens e quatro eventos. A quantidade efetiva de papel permaneceu em 1,25 kg.

## Desempenho com 1.000 solicitações

O script usa Python e requisições HTTP sequenciais em loopback. A carga contém exatamente 1.000 solicitações com dois itens, distribuídas por dois pontos, das quais dez estão concluídas. Depois do aquecimento, o script mede listagem, listagem filtrada, detalhe, indicadores e busca de pontos.

| Medida | Resultado |
| --- | ---: |
| Consultas medidas | 100 |
| Consultas em até 2 segundos | 100 |
| Mediana | 3,65 ms |
| Percentil 95 | 6,19 ms |
| Máximo | 7,43 ms |

Para reproduzir, inicie a aplicação apontando para um banco descartável sem solicitações e execute `python3 scripts/desempenho.py`. O script encerra com erro se o percentil 95 superar dois segundos. Consulte os [resultados brutos](evidencias/desempenho.json).
