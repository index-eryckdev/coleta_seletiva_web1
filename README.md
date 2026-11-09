# Sistema de Coleta Seletiva — API

Trabalho de **Programação Web I** (IFS). Backend em Java 21 + Spring Boot + PostgreSQL.

**Equipe:** Eryck Jhonata Lima Carvalho, Felipe Barbosa Brito e Pedro Henrique dos Santos Lino.

## Requisitos

- JDK 21
- PostgreSQL 14 ou superior
- Maven 3.9+ (ou o Maven Wrapper, se o projeto tiver `mvnw`)
- VS Code com *Extension Pack for Java* e *Spring Boot Extension Pack* (recomendado)

## 1. Configurar o PostgreSQL

Abra o **SQL Shell (psql)** instalado com o PostgreSQL (ou o *Query Tool* do pgAdmin), entre como o usuário `postgres` e execute:

```sql
CREATE USER coleta_user WITH PASSWORD 'escolha_uma_senha';
CREATE DATABASE coleta_seletiva OWNER coleta_user;
```

Cada integrante usa o **próprio** banco local. As tabelas e os dados de demonstração são criados automaticamente pelo Flyway na primeira execução.

## 2. Configurar as variáveis de ambiente

Copie o arquivo de exemplo e preencha a senha que você escolheu:

```bash
cp .env.example .env        # Windows (PowerShell): Copy-Item .env.example .env
```

O arquivo `.env` está no `.gitignore` e **nunca** deve ser versionado. A aplicação lê estas variáveis:

| Variável      | Padrão            | Observação                 |
|---------------|-------------------|----------------------------|
| `DB_HOST`     | `localhost`       |                            |
| `DB_PORT`     | `5432`            |                            |
| `DB_NAME`     | `coleta_seletiva` |                            |
| `DB_USER`     | `coleta_user`     |                            |
| `DB_PASSWORD` | —                 | **obrigatória**, sem padrão |

## 3. Executar

**Pelo VS Code (recomendado):** abra a aba *Run and Debug*, escolha **Coleta Seletiva (API)** e clique em executar. A configuração em `.vscode/launch.json` já carrega o `.env`.

**Pelo terminal:** carregue as variáveis e rode o Maven.

```powershell
# Windows (PowerShell), na pasta do projeto
Get-Content .env | ForEach-Object { if ($_ -match '^\s*([^#=]+)=(.*)$') { Set-Item "env:$($matches[1].Trim())" $matches[2].Trim() } }
mvn spring-boot:run
```

```bash
# Linux / macOS
set -a; source .env; set +a
mvn spring-boot:run
```

## 4. Conferir se funcionou

- Status da API: <http://localhost:8080/api/status> (deve responder `{"status":"ok", ...}`)
- Documentação (Swagger UI): <http://localhost:8080/swagger-ui.html>
- No banco, a tabela `flyway_schema_history` deve listar as migrações V1 e V2.

## Estrutura do projeto

```
src/main/java/com/coletaseletiva/
├── config/        configurações (OpenAPI etc.)
├── controller/    endpoints REST (só recebem/retornam DTOs)
├── dto/           objetos de entrada e saída (com Bean Validation)
├── entity/        entidades JPA
├── exception/     exceções e GlobalExceptionHandler (400, 404, 409, 500)
├── repository/    interfaces Spring Data JPA
└── service/       regras de negócio e transações (@Transactional)
src/main/resources/
├── application.properties
└── db/migration/  V1__criar_tabelas.sql, V2__dados_demonstracao.sql
```

## Convenções da equipe

- **Esquema do banco só muda por migração Flyway.** Nunca edite uma migração já commitada: crie `V3__...`, `V4__...`. O Hibernate está em `ddl-auto=validate`.
- Entidades usam `@GeneratedValue(strategy = GenerationType.IDENTITY)` e `@Enumerated(EnumType.STRING)` para as situações e unidades.
- Regras de negócio ficam no **Service**; controllers não acessam repositories.
- Erros de regra: lance `RegraNegocioException` (409). Registro ausente: `RecursoNaoEncontradoException` (404). Entrada inválida: Bean Validation nos DTOs (400).
- Listagens sempre paginadas (`Pageable`), no máximo 100 por página.
- Nunca commitar senhas, tokens ou o arquivo `.env`.

## Git

- Branch principal protegida por combinação da equipe; cada módulo em uma branch (`feature/material`, `feature/ponto-coleta`, `feature/solicitacao`...).
- Cada integrante commita com o **próprio** usuário (`git config user.name` / `user.email`), pois o histórico de participação é avaliado.
- Mensagens de commit curtas e no imperativo (ex.: `Adiciona cadastro de materiais`).

## Ordem de implementação sugerida

1. Material
2. Ponto de coleta + materiais aceitos (RF04 a RF06)
3. Morador, endereços e operador (RF01, RF02, RF07)
4. Solicitação: criação com itens e histórico (RF08, RN01, RN02, RN04, RN06)
5. Agendar, recusar, cancelar, iniciar, concluir e reagendar (RF10 a RF14)
6. Resumo gerencial (RF15)
7. Testes automatizados e documentação final
