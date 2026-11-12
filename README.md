# Sistema de Coleta Seletiva

Projeto de **Programação Web I** do Instituto Federal de Sergipe, Campus Lagarto. O sistema cadastra pontos de coleta e materiais recicláveis, recebe solicitações de recolhimento domiciliar e registra o atendimento até a conclusão.

**Equipe:** Eryck Jhonata Lima Carvalho, Felipe Barbosa Brito e Pedro Henrique dos Santos Lino.

## Executar localmente

Requisitos: JDK 21, Maven 3.9+ e PostgreSQL 14+. Em um terminal SQL com permissões administrativas, crie um usuário e os dois bancos:

```sql
CREATE USER coleta_user WITH PASSWORD 'escolha_uma_senha';
CREATE DATABASE coleta_seletiva OWNER coleta_user;
CREATE DATABASE coleta_teste OWNER coleta_user;
```

Cada integrante usa seu próprio banco. O Flyway aplica o esquema, carrega dados fictícios e registra as migrações em `flyway_schema_history`. A conta configurada precisa ser proprietária do banco local para aplicar as migrações.

Copie `.env.example` para `.env` e preencha `DB_PASSWORD` com a senha que você definiu. O Git ignora `.env`.

| Variável | Padrão |
| --- | --- |
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `coleta_seletiva` |
| `DB_USER` | `coleta_user` |
| `DB_PASSWORD` | Obrigatória |
| `TEST_DB_NAME` | `coleta_teste` |
| `SERVER_ADDRESS` | `127.0.0.1` |
| `SERVER_PORT` | `8080` |

No VS Code, use a configuração **Coleta Seletiva (API)**. Pelo terminal no Linux ou macOS:

```bash
cp .env.example .env  # Primeira execução
# Preencha DB_PASSWORD no .env antes de carregar
set -a
source .env
set +a
mvn spring-boot:run
```

No PowerShell:

```powershell
Copy-Item .env.example .env  # Primeira execução
# Preencha DB_PASSWORD no .env antes de carregar
Get-Content .env | ForEach-Object { if ($_ -match '^\s*([^#=]+)=(.*)$') { Set-Item "env:$($matches[1].Trim())" $matches[2].Trim() } }
mvn spring-boot:run
```

- Estado da API em <http://127.0.0.1:8080/api/status>
- Swagger UI em <http://127.0.0.1:8080/swagger-ui.html> e especificação em <http://127.0.0.1:8080/v3/api-docs>

Os cabeçalhos da API selecionam atores fictícios sem autenticação. Use apenas dados fictícios no ambiente acadêmico local.

## Testar e empacotar

Com as variáveis do banco carregadas e `coleta_teste` disponível:

```bash
mvn verify
java -jar target/coleta-seletiva-0.0.1-SNAPSHOT.jar
```

A suíte usa PostgreSQL de verdade. Use um banco descartável dedicado para os testes e um banco vazio separado para o ensaio de desempenho. O GitHub Actions executa a suíte com Java 21 e PostgreSQL 17.

## Organização

| Caminho | Conteúdo |
| --- | --- |
| `src/main/java/com/coletaseletiva/` | Controllers REST, DTOs, serviços transacionais, entidades, repositórios e tratamento de erros |
| `src/main/resources/db/migration/` | Esquema Flyway, dados fictícios e evolução do modelo |
| `src/test/` | Testes de integração HTTP e PostgreSQL |
| `scripts/` | Ensaio reproduzível de desempenho |

- [Contrato da API com exemplos](docs/API.md)

O Hibernate valida o esquema. Novas mudanças no banco entram por migrações Flyway numeradas. O fluxo Git da equipe usa branches por módulo, commits com a conta responsável e pull requests para revisão em `main`.
