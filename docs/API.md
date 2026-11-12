# Contrato da API

O Swagger UI em `/swagger-ui.html` expõe os schemas, os parâmetros, os códigos de resposta e a execução de cada operação. A especificação JSON fica em `/v3/api-docs`.

## Contexto de demonstração

| Perfil | Cabeçalhos | Alcance |
| --- | --- | --- |
| Visitante | Nenhum | Pontos e materiais ativos, status da API e cadastro de morador |
| Morador | `X-Ator-Tipo: MORADOR`, `X-Ator-Id: 1` | Cadastro próprio, endereços, solicitações próprias, correção e cancelamento |
| Operador | `X-Ator-Tipo: OPERADOR`, `X-Ator-Id: 1` | Solicitações dos pontos vinculados e ações de atendimento |
| Administrador | `X-Ator-Tipo: ADMINISTRADOR` | Cadastros gerenciais, consulta de todas as solicitações, cancelamento e indicadores |

Os IDs de exemplo representam Ana e Maria. Consulte os endereços da Ana pelo endpoint de endereços para escolher o ID da demonstração. Os cabeçalhos são enviados pelo cliente e identificam atores fictícios.

## Cadastros e catálogo

| Método e caminho | Operação |
| --- | --- |
| `GET /api/status` | Estado da aplicação |
| `GET /api/materiais`, `GET /api/materiais/{id}` | Materiais ativos. Filtro opcional `nome` na lista |
| `GET /api/admin/materiais` | Materiais para administração. Filtros `nome` e `ativo` |
| `POST /api/materiais`, `PUT /api/materiais/{id}` | Criar ou atualizar material |
| `PATCH /api/materiais/{id}/inativar`, `PATCH /api/materiais/{id}/reativar` | Alterar disponibilidade |
| `GET /api/pontos`, `GET /api/pontos/{id}` | Pontos ativos com endereço, horários e materiais aceitos |
| `GET /api/admin/pontos` | Pontos para administração |
| `POST /api/pontos`, `PUT /api/pontos/{id}` | Criar ou atualizar ponto |
| `PATCH /api/pontos/{id}/inativar`, `PATCH /api/pontos/{id}/reativar` | Alterar disponibilidade |
| `POST /api/pontos/{id}/materiais` | Adicionar material aceito e limite opcional |
| `PUT /api/pontos/{id}/materiais/{materialId}`, `DELETE /api/pontos/{id}/materiais/{materialId}` | Editar ou remover vínculo |
| `GET /api/moradores`, `POST /api/moradores` | Listar como administrador e cadastrar como visitante |
| `GET /api/moradores/{id}`, `PUT /api/moradores/{id}` | Consultar e editar morador |
| `PATCH /api/moradores/{id}/inativar`, `PATCH /api/moradores/{id}/reativar` | Alterar disponibilidade como administrador |
| `GET /api/moradores/{id}/enderecos`, `POST /api/moradores/{id}/enderecos` | Listar e cadastrar endereços |
| `GET /api/moradores/{id}/enderecos/{enderecoId}`, `PUT /api/moradores/{id}/enderecos/{enderecoId}` | Consultar e editar endereço |
| `GET /api/operadores`, `POST /api/operadores` | Listar e cadastrar operadores |
| `GET /api/operadores/{id}`, `PUT /api/operadores/{id}` | Consultar e editar operador |
| `PUT /api/operadores/{id}/pontos/{pontoId}`, `DELETE /api/operadores/{id}/pontos/{pontoId}` | Vincular ou desvincular operador |

Filtros dos pontos: `nome`, `municipio`, `bairro`, `materialId` e `recolhimentoDomiciliar`. Nome, município e bairro aceitam trechos sem distinção de maiúsculas e minúsculas. Listagens aceitam `page` a partir de 0, `size` de até 100 e `sort=campo,asc|desc`. As respostas paginadas usam `conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`.

## Recolhimento

| Método e caminho | Corpo ou filtros |
| --- | --- |
| `POST /api/solicitacoes` | `pontoId`, `enderecoId`, `observacoes` opcional e `itens` com `materialId` e `quantidadeEstimada` |
| `GET /api/solicitacoes` | `situacao`, `pontoId`, `inicio`, `fim`, paginação |
| `GET /api/solicitacoes/{id}` | Resumo, endereço copiado, observações, itens e histórico |
| `PUT /api/solicitacoes/{id}` | `versao`, novo `enderecoId`, `observacoes` opcional e lista completa de `itens` |
| `PATCH /api/solicitacoes/{id}/agendar` | `versao`, `dataAgendamento` futura e `motivo` opcional |
| `PATCH /api/solicitacoes/{id}/recusar` ou `/cancelar` | `versao` e `motivo` obrigatório |
| `PATCH /api/solicitacoes/{id}/iniciar` | `versao` |
| `PATCH /api/solicitacoes/{id}/concluir` | `versao` e `itens` com `materialId` e `quantidadeEfetiva` para todos os materiais |
| `PATCH /api/solicitacoes/{id}/reagendar` | `versao`, `dataAgendamento` futura, `motivo` e `detalhesTentativa` opcional quando em recolhimento |
| `GET /api/indicadores` | `inicio` e `fim`. Agrupa contagens pela data de criação e quantidades pela data de conclusão |

Datas e horas usam ISO 8601 com offset, como `2027-01-15T14:30:00-03:00`. O início do período é inclusivo e o fim é exclusivo. Cada mutação exige a `versao` recebida no detalhe ou na resposta anterior.

```bash
base=http://127.0.0.1:8080
curl "$base/api/pontos?municipio=Aracaju&materialId=3"
curl -H 'X-Ator-Tipo: MORADOR' -H 'X-Ator-Id: 1' "$base/api/moradores/1/enderecos"
curl -X POST "$base/api/solicitacoes" \
  -H 'X-Ator-Tipo: MORADOR' -H 'X-Ator-Id: 1' -H 'Content-Type: application/json' \
  -d '{"pontoId":1,"enderecoId":1,"itens":[{"materialId":1,"quantidadeEstimada":12.5},{"materialId":2,"quantidadeEstimada":8}]}'
```

A criação retorna HTTP 201 e um protocolo. Atualizações retornam 200 e a representação atual. A remoção de um vínculo de material retorna 204. A API usa 400 para entradas inválidas, 403 para contexto de ator incompatível, 404 para ausência de recurso e 409 para conflito de regra, integridade ou versão.

Exemplo de erro HTTP 400:

```json
{
  "timestamp": "2026-11-12T18:00:00Z",
  "status": 400,
  "codigo": "VALIDACAO",
  "mensagem": "A quantidade em peças deve ser inteira.",
  "caminho": "/api/solicitacoes",
  "erros": [
    {"campo": "itens[0].quantidadeEstimada", "mensagem": "A quantidade em peças deve ser inteira."}
  ]
}
```
