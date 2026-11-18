# Matriz

## Current working-tree overview — 2026-11-16

Matriz implements session login/logout, project/task CRUD, state/priority, multiple eligible assignees, comments, literal project-name search, task filters and a dashboard. Latest isolated post-fix verification: **244 passes (199 backend + 45 frontend)**, zero failures/errors/skips; production build passed. Earlier full-regression baseline: **252 distinct tests (167 backend + 37 PostgreSQL integration + 5 Java Playwright + 43 frontend)**. PostgreSQL/browser suites were not rerun after the later description/comment Unicode and raw-decimal fixes; no complete all-layer regression after those fixes is claimed. Do not add overlapping runs or Maven `verify` repetitions to unique coverage. Exact recorded commands, results and evidence are in [verification](docs/verification.md). No suites were rerun for this documentation audit.

**Delivery is partial, not approved/published:** remote Actions success and controlled-failure evidence, final secret-reviewed Git snapshot/clean-checkout verification, release identity and human acceptance remain open. See the [assignment checklist](docs/final-review.md).

Matriz contains a Spring Boot backend, React frontend, PostgreSQL migration and optional demo seed, automated test sources, and Docker/CI configuration. The older P1 and Assignments 01/02 descriptions below are preserved as historical records; statements that no backend exists do not describe the current working tree. Existing code is not evidence that all requirements are met or that a clean checkout is reproducible.

Start with the public [project requirements](docs/requirements.md), [implementation decisions](docs/implementation-plan.md) and [verification evidence](docs/verification.md). Third-party course materials, including PDFs and Markdown transcriptions, are retained unchanged locally and are not distributed in this public repository. The [documentation normalization report](docs/documentation-normalization-report.md) preserves historical local provenance; its course-material references are not public delivery dependencies.

Matriz v1.3 is the academic project label, not a unified application release. The backend (`0.1.0-SNAPSHOT`), frontend (`0.0.0`) and OpenAPI contract (`0.4.0`) versions are separate component identifiers. Git publication remains pending.

### Optional Microsoft personal-account connection (backend foundation)

This is a **disabled-by-default delegated connection**, not Microsoft login to Matriz. Existing Matriz sessions, session expiry and CSRF remain authoritative. No UI, Graph requests, mail sending, refresh workflow, database changes or persistent token cache are implemented.

Backend environment variables (do not put real credentials in source control):

| Variable | Default / purpose |
| --- | --- |
| `MICROSOFT_CONNECTION_ENABLED` | `false`; disabled needs no credentials and makes no Microsoft requests |
| `MICROSOFT_CLIENT_ID` | Empty; required only when enabled |
| `MICROSOFT_CLIENT_SECRET` | Empty; confidential web application secret, required only when enabled |
| `MICROSOFT_REDIRECT_URI` | Empty; exact registered absolute callback URL ending `/api/v1/connections/microsoft/callback`; HTTPS required except `http://localhost` development |

A future, separately authorized live setup must register an application supporting **personal Microsoft accounts** and the exact **Web** callback URI. The fixed Microsoft identity v2 `consumers` authority excludes organizational account login; delegated scopes are only `https://graph.microsoft.com/Mail.Send` and `offline_access` (no application permission, `.default`, profile or mailbox read). Spring Security OAuth client, version managed by the existing Spring Boot BOM, builds authorization requests and exchanges codes. It is used directly, without `oauth2Login`, discovery or replacement of Matriz authentication. Microsoft references: [personal-account authority](https://learn.microsoft.com/en-us/entra/identity-platform/v2-protocols#endpoints), [authorization code flow and PKCE](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow), [delegated Mail.Send](https://learn.microsoft.com/en-us/graph/permissions-reference#mail-send). These links are configuration references, not evidence of a live consent test.

All routes require the existing authenticated Matriz session:

- `GET /api/v1/connections/microsoft`: exactly `{ "enabled": boolean, "connected": boolean }`; no tokens/account details.
- `POST /api/v1/connections/microsoft/connect`: requires `X-CSRF-TOKEN`; returns `{ "authorizationUrl": "..." }`. The caller may explicitly navigate to this fixed-authority URL. No caller-provided redirect or user ID is used. Disabled returns 503.
- `GET /api/v1/connections/microsoft/callback`: requires the original Matriz user and session, exact single-use state (five minutes) and library-generated S256 PKCE verifier. State is consumed before exchange, including denial and exchange failure. Success is 204, not a redirect; errors are sanitized problem responses. No automatic login, arbitrary destination or provider error reflection.
- `DELETE` or `POST /api/v1/connections/microsoft/disconnect`: requires CSRF; clears only this Matriz user's tokens and pending authorization, returning 204. Disabled returns 503. This is local deletion, **not** Microsoft consent revocation.

Pending requests and access/refresh tokens live only in bounded process memory (up to 1,000 entries each), keyed by server-resolved Matriz user ID. They are never placed in HTTP sessions, serialized to storage, returned or intentionally logged. Expired entries are evicted on subsequent connection operations; all state disappears on backend restart. Access-token expiry makes status disconnected and discards the associated refresh token; refresh is intentionally not implemented. Logout/session expiry prevents callback completion but does not revoke an already connected user's grant; another valid Matriz session for that same user can inspect status/disconnect. Different users cannot access it. An already connected account cannot be silently replaced (409): explicitly disconnect first. This foundation does not fetch/verify/display a Microsoft account identity. Re-initiation replaces that user's previous pending state.

**Operational limits:** single-node only; an in-flight exchange currently serializes connection operations and has 5-second connect/10-second read timeouts. No durable cache, multi-node coordination, automatic refresh, remote revocation, scheduled cleanup or account-identity linkage. Keep HTTP/OAuth wire debug logging and proxy query-string/access logging disabled/redacted for the callback (codes/state) and token endpoint (credentials/tokens). Published environments require HTTPS and secure Matriz cookies. Do not enable or navigate the authorization URL without separate approval for credentials/consent. The implementation was verified only with mocked HTTP/provider responses; no real credentials, consent, Microsoft network requests or emails were used.

Verification on 2026-11-17: `mvn.cmd -B -ntp -f backend/pom.xml test` with JDK 21 passed **238** tests (204 existing + 34 new Microsoft cases), zero failures/errors/skips; `npm.cmd --prefix frontend test` passed **185** tests; `npm.cmd --prefix frontend run build` passed. Java compilation is included in Maven tests; the frontend has no separate typecheck script. PostgreSQL integration/browser tests and live Microsoft behavior were not run. These results supersede neither historical full-stack evidence nor release approval.

### Configuration and execution references

The inspected manifests specify Java 21, Spring Boot 3.5.16, React/react-dom 19.3.1, Vite 8.3.8 and the PostgreSQL Docker image `postgres:17.11-bookworm`. Dockerfiles build the backend and frontend separately; Compose binds the nginx frontend with `127.0.0.1:8080:80`, and nginx proxies API requests to the backend. These are configuration facts, not successful runtime verification or support-lifecycle certification.

The current Compose file requires `DB_PASSWORD`, which supplies the database container's `POSTGRES_PASSWORD` and the backend's `DB_PASSWORD`. The optional seed is disabled by default; enabling it also requires `DEMO_MANAGER_PASSWORD` and `DEMO_MEMBER_PASSWORD`. Keep real values in a private local environment, never in committed documents. The sanitized `.env.example` is included in the 67-file academic export; Git publication and clean-checkout verification remain pending.

All teaching materials under `docs/aulas/`, including Markdown reading copies and original PDFs, are preserved locally but ignored by Git. Historical course-source references are local-only provenance, not clean-checkout dependencies; use the public project requirements, implementation decisions and verification linked above.

### Installation, schema, demo accounts and local startup

Prerequisites: Linux-container Docker Engine/Desktop with Compose; for host tests, Java 21/Maven and Node 24/npm with platform-correct dependencies (`npm --prefix frontend ci`, using the existing lockfile). Do not overwrite an existing `.env`. On a fresh approved checkout only, copy the sanitized `.env.example` to a private `.env`, supply a strong local `DB_PASSWORD`, and keep it untracked. Flyway automatically applies V1/V2 at backend startup; no manual SQL import/reset is needed.

For a fresh demo environment set `DEMO_SEED_ENABLED=true`, and supply `DEMO_MANAGER_PASSWORD` and `DEMO_MEMBER_PASSWORD` (each at least 12 characters). Synthetic logins are `gestor@example.org` and `membro@example.org`; passwords are supplied locally, never documented defaults. Seed creates private/shared demo projects and memberships but **does not reset existing accounts or projects**. With seed disabled there is no automatic login account. Demo seeding is one-time initialization: if either demo email already exists, backend startup refuses to proceed rather than reusing or resetting that account. After successful initialization, set `DEMO_SEED_ENABLED=false` in your private local configuration and run `docker compose up -d --force-recreate backend` to recreate the backend with the updated environment; a container restart alone does not reload Compose environment settings. Existing demo accounts, passwords and data remain available. Leaving seeding enabled causes subsequent backend startups to fail. Do not enable/reset seed against shared data, delete accounts or remove volumes as a troubleshooting shortcut.

Reference commands from the repository root, **configuration-checked, not executed in this documentation audit**. They affect the selected Compose project: obtain approval before using an existing stack; historical isolated runtime evidence is in [Ficha 05 validation](docs/ficha05-validation.md).

```text
docker compose config -q
docker compose build
docker compose up -d
docker compose ps
docker compose logs --tail=50
```

Open `http://127.0.0.1:8080`. All three services have Compose healthchecks; frontend initially waits for healthy backend, backend for healthy DB. nginx serves built assets and proxies `/api/`; DB/backend publish no host ports. `postgres_data` preserves data across normal container recreation. Never delete volumes to fix startup. Logs can contain private data: inspect locally, do not publish raw output.

With Java 21/Maven available, backend test sources are invoked with `mvn -f backend/pom.xml test`. With frontend dependencies already installed, `npm --prefix frontend test` runs all `src/*.test.js` Node tests (45 passes in the latest isolated run, including API, projects and task-flow helpers); `npm --prefix frontend run build` builds the UI. Runtime images do not contain the Maven/npm development toolchains, so older instructions to run those tools inside the final Compose services are obsolete. These commands are not a claim of integration coverage or successful execution.

The demo seed is optional and does not reset existing accounts. Do not delete volumes to fix setup problems. See [delivery validation status](docs/ficha05-validation.md), [final delivery checklist](docs/final-review.md) and [the QA plan](docs/test-plan.md); pending checks and known contract/security defects remain unresolved. No application tests, builds, migrations, Docker startup or CI runs were performed in this documentation task.

## Backend tests — Ficha 04 PostgreSQL integration (2026-11-14)

From the repository root:

```text
mvn -f backend/pom.xml test
mvn -f backend/pom.xml -Pintegration verify
```

Prerequisites: Java 21 and Maven; integration additionally requires a running Docker engine using Linux containers, Docker API access, and network/cache access to Maven dependencies and `postgres:17.11-bookworm` (plus Testcontainers helper images). Spring Boot 3.5.16 manages Testcontainers 1.21.4 and Failsafe 3.5.6. On Windows, use `mvn.cmd` or its full installed path if necessary.

`test` runs the ordinary backend suite (199 passes in the latest isolated run) without starting Docker containers. The earlier full-regression baseline below used 167 backend cases. `-Pintegration verify` also runs `TaskPostgresIT` through Failsafe: 37 cases exercising real login/CSRF, task creation/retrieval, committed PostgreSQL persistence, validation and cross-user authorization. Docker absence is an error, not a silently skipped integration suite.

The integration class overrides datasource and Flyway connection properties with a disposable Testcontainers database, uses a random mapped port, disables demo seeding and Flyway clean, and runs the existing V1/V2 migrations. It does not use Compose, the existing Matriz database, `.env` credentials, bind mounts or Compose volumes. Every test invocation creates its own synthetic fixtures; no truncation/reset is needed. Testcontainers automatically disposes only its test resources. Do not use Compose volume deletion or a shared database to prepare these tests.

Executed on 2026-11-14: normal suite passed before and after adding the setup (129 tests, 0 failures/errors/skips). After correcting one test-only JSON numeric-node assertion, two consecutive full integration-profile runs each passed 129 normal + 15 integration tests (144 total, 0 failures/errors/skips). Reports: `backend/target/surefire-reports/` and `backend/target/failsafe-reports/`. These results do not establish browser E2E, network/proxy behavior, restart durability or completion of the entire Ficha 04 review. See [test plan](docs/test-plan.md) and [security evidence](docs/quality-security-review.md).

## Browser E2E — Ficha 04

The `e2e` Maven profile uses Playwright Java 1.58.0 and headless Chromium. It starts the real Spring Boot HTTP server, a private Vite server/proxy, and a disposable PostgreSQL Testcontainers instance. No running application or Compose database is needed. Frontend package manifests are unchanged.

Prerequisites: Java 21, Maven, Node compatible with the installed Vite (tested with Node 24.14.0), installed platform-correct `frontend/node_modules`, accessible Linux Docker, and network/cache access for Maven, PostgreSQL and Playwright browser downloads. Install frontend dependencies using the existing lockfile if missing; do not copy Linux `node_modules` to Windows. Chromium installation below only populates Playwright's browser cache.

From `backend/`:

```text
mvn -Pe2e test-compile exec:java
mvn -Pe2e verify
```

The first command installs the pinned Chromium browser (needed once per environment/version). The second runs the ordinary backend suite plus all 5 browser E2E methods (the earlier full-regression baseline recorded 167 backend + 5 E2E passes; no browser rerun after the later fixes is claimed): authentication/permissions, project lifecycle/search, task creation/navigation, collaboration/filters/assignments/comments, validation/drafts/busy/deletion. Run `mvn -Pintegration verify` separately for the 37 PostgreSQL API cases; do not combine the profiles, since their Failsafe include rules select different suites. If Node is not on PATH, pass `-De2e.node=<absolute-path-to-node>` to the E2E run.

The browser logs in with a unique synthetic account, opens its unique project, selects filters hiding TODO/MEDIUM tasks, creates a task, and verifies the same project still displays its new task with filters reset. It also verifies the database row and reopens the project after a page reload. There are no mocked API responses. Both datasource and Flyway connections use only the generated container URL/credentials; demo seed and Flyway clean are disabled. Ports are ephemeral and loopback-only. Test-owned processes/containers are disposed automatically; existing data, containers and Docker volumes are not used or deleted.

Reports: `backend/target/failsafe-reports/pt.matriz.tasks.TaskCreationE2E.txt`; private Vite log: `backend/target/e2e-artifacts/vite.log`. On browser-flow failure a screenshot and Playwright trace are saved in that artifacts directory. All artifacts are under the already-ignored `target/`. A trace left from a previous failure is not evidence of the latest run; consult the current Failsafe report.

Executed 2026-11-14: two consecutive successful E2E-profile runs, each **129 normal tests + 1 browser test, 0 failures/errors/skips**. Frontend verification: **43 passed, 0 failures/skips**, production build successful. This is a Chromium development-server E2E, not production proxy/HTTPS coverage or a full browser compatibility matrix.

## Historical record — P1 documentation

P1 documental formalmente aprovada e concluída pelo utilizador em 2026-11-11, com as limitações do relatório final aceites. D1–D10 e pontos 1–7 mantidos. Este estado prevalece sobre referências anteriores a fecho pendente. Não foi implementado backend/React/PostgreSQL nesta fase. O protótipo HTML existente continua local e não comprova API, autenticação ou persistência de servidor.

**MVP aprovado:** âmbito completo da Ficha 03: sessões, CRUD de projetos e tarefas, prioridades, zero ou mais responsáveis elegíveis, comentários, pesquisa, filtros e painel. **Stack aprovada:** Java 21 LTS, Spring Boot 3.5.x, React 19.x e PostgreSQL 17.x, backend monólito modular. Papéis gestor/membro por projeto, participação direta e seed; sessão Spring Security com HttpOnly/CSRF. Matriz D3 aprovada: gestor gere projeto/responsáveis e qualquer tarefa; membro cria tarefas e só edita/altera estado das atribuídas a si; ambos consultam/comentam. Criador é gestor automaticamente, criação transacional e projeto privado sem outros membros. D4: inatividade máxima 30 min, duração absoluta 8 h, HttpOnly, SameSite=Lax, Secure em HTTPS, CSRF inclusive login/logout, rotação no login e invalidação no logout. Ver matriz precisa no plano; isto não declara implementação.

- [Plano, decisões D1–D10 e tarefas](docs/implementation-plan.md)
- [Verificações e revisões reais](docs/verification.md)
- [Requisitos vigentes e contexto histórico](docs/requirements.md)
- [Histórias atuais](docs/user-stories.md)
- [Contrato canónico](openapi.yaml), cópia sincronizada em [docs](docs/matriz-openapi.yaml)
- [Estado e checklist de entrega](docs/final-review.md) e [âmbito e limitações da verificação](docs/verification.md)

**Limites:** só documentação nos ficheiros autorizados; sem código, instalações, migrations, alterações de dados/configurações executáveis, commits, merges, push ou deploy. P2 não autorizada. Não há comandos de arranque validados para a stack futura; não apresentar instruções hipotéticas como executadas.

## Complementos aprovados — pontos 1–7

Verificar suporte e compatibilidade antes da implementação e fixar as versões exatas nessa fase futura; qualquer substituição requer aprovação humana. Suporte e compatibilidade não foram verificados nesta atualização documental.

Frontend e API na mesma origem, com HTTPS no ambiente publicado e proxy em desenvolvimento; não permitir origens arbitrárias com credenciais. Bootstrap CSRF e convenções `JSESSIONID` (cookie de sessão) e `X-CSRF-TOKEN` (header) aprovados, condicionados à coerência com D3/D4: CSRF inclusive login/logout, HttpOnly no cookie de sessão, SameSite=Lax, Secure em HTTPS, rotação no login e invalidação no logout. Não constituem configuração executável nem prova de implementação.

Listas completas, **sem paginação no MVP**. Projetos: `id DESC`; tarefas: `createdAt DESC, id DESC`; comentários e histórico: data UTC `ASC, id ASC`; responsáveis elegíveis: nome sem distinguir maiúsculas/minúsculas `ASC, id ASC`. Pesquisa por substring literal no nome, com trim, sem distinguir maiúsculas/minúsculas (case-insensitive), mas distinguindo acentos (accent-sensitive). Valor omitido ou vazio após trim não aplica filtro; `%` e `_` são caracteres literais, não wildcards.

Descrições de projetos e tarefas: na criação, campo omitido ou `null` significa ausência; em PATCH, omitido mantém o valor e `null` remove-o. Texto é sujeito a trim e rejeitado se ficar vazio, respeitando os limites D8. A API devolve `null` quando a descrição está ausente.

Em CreateTask, membro pode omitir responsáveis ou enviar `[]`; uma lista não vazia devolve 403 sem criação parcial, incluindo tentativa de autoatribuição. Gestor pode indicar responsáveis elegíveis por participação direta no projeto.

Estes complementos não fecham P1 nem autorizam P2; ver registo dos pontos 1–7 no plano.

## Contexto histórico — apresentação das Fichas 01/02

O texto abaixo preserva a proposta anterior. Onde divergir (responsável único, exclusão de edição/eliminação, stack/autenticação pendentes), prevalecem D1–D10 no plano acima.

## Sobre o projeto

A **Matriz** é uma aplicação proposta para ajudar equipas a organizar projetos, gerir tarefas e acompanhar o progresso do trabalho.

Este repositório reúne o **Specification Pack da Ficha 01** e o **Architecture Pack da Ficha 02**. Não existe backend implementado; o protótipo HTML usa dados simulados e `localStorage` para demonstrar alguns fluxos.

## Objetivo das fichas 01 e 02

- Identificar as necessidades gerais apresentadas pelo cliente.
- Propor um conjunto inicial de funcionalidades e delimitar um MVP.
- Escrever histórias de utilizador e critérios de aceitação.
- Registar pressupostos, ambiguidades e questões por validar.
- Rever criticamente as propostas antes de tomar decisões de implementação.

## MVP proposto

A primeira versão deverá permitir criar e consultar projetos e, dentro deles, criar, consultar, editar, atribuir e atualizar o estado das tarefas. **Cada tarefa pertence obrigatoriamente a um projeto e tem um único responsável obrigatório desde a criação**, segundo a proposta de desenho.

A edição e eliminação de projetos ficam fora do MVP. A autenticação, as permissões e a gestão de membros ainda precisam de clarificação. As funcionalidades e regras aqui descritas **são propostas de trabalho, não requisitos aprovados pelo cliente**.

## Documentação

| Ficheiro | Conteúdo |
| --- | --- |
| [`docs/requirements.md`](docs/requirements.md) | Visão, requisitos funcionais e não funcionais, MVP e questões em aberto. |
| [`docs/user-stories.md`](docs/user-stories.md) | Histórias de utilizador, critérios de aceitação e casos excecionais. |
| [`docs/constraints.md`](docs/constraints.md) | Restrições, pressupostos e decisões pendentes. |
| [`docs/review.md`](docs/review.md) | Revisão crítica das Fichas 01 e 02 e recomendações a validar. |
| [`docs/architecture.md`](docs/architecture.md) | Arquitetura proposta e respetivos trade-offs. |
| [`docs/database.md`](docs/database.md) | Modelo de dados e diagrama ER Mermaid. |
| [`openapi.yaml`](openapi.yaml) | Contrato REST OpenAPI 3.1 do MVP. |
| [`docs/final-review.md`](docs/final-review.md) | Checklist de aceitação, limitações e pendências da entrega. |

**Ordem de leitura sugerida:** requirements → user-stories → constraints → architecture → database → openapi → review → final-review.

## Próximos passos

1. Concluir a revisão crítica e verificar a coerência entre documentos.
2. Validar com o cliente as funcionalidades, os pressupostos e as decisões em aberto.
3. Validar a arquitetura proposta, bem como a stack e a base de dados a adotar.
4. Só depois iniciar a implementação.

## Transparência sobre o processo

A documentação foi elaborada com apoio de **inteligência artificial**, através de discussão e revisão humana. As sugestões geradas por IA não substituem a validação do cliente nem constituem decisões técnicas definitivas.
