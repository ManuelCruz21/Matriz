# Ficha 04 — Plano de testes (Matriz v1.3)

## Current executed matrix — 2026-11-16

Latest isolated verification after the later description/comment Unicode and raw-decimal fixes: **244 passes = 199 backend + 45 frontend**, all passed with zero failures/errors/skips; production build passed. Earlier full-regression baseline: **252 distinct tests = 167 backend + 37 PostgreSQL integration + 5 Java Playwright + 43 frontend**, all passed, production build passed. PostgreSQL/browser suites were not rerun after the later fixes; no complete all-layer regression after them is claimed. Exact recorded commands/results/evidence are in [verification](verification.md). No tests executed in this documentation update. The matrix combines dated coverage; historical smaller totals/pending rows below remain unchanged and do not override the opening reconciliation.

| Requirement / case | Expected result | Current associated test/evidence |
| --- | --- | --- |
| U01/U02 title rule | Strip, code-point length 3–150 inclusive; invalid values 422/no writes | `TaskTitleRuleTest`: 20 cases; task create/update supplementary boundaries 2/3/150/151 plus original ASCII/null/blank creation cases |
| Project-name boundary | Strip, code-point length 3–100; invalid values 422/no writes | `ProjectControllerTest.unicodeNameBoundariesOnCreateAndPatch`: 4 cases, each POST/PATCH |
| I01 create/read/persistence | 201 then 200, normalized fields and committed DB data | `TaskPostgresIT.managerCreatesRetrievesAndCommitsTaskWithEligibleAssignment` |
| I02 invalid/nonexistent | Invalid title 422/no writes; unknown resource 404 | `invalidTitlesAreRejectedWithoutWrites`, `nonexistentTaskReturnsNotFound` |
| A01/A02/A03 two-user authorization | Hidden resources 404; prohibited accessible mutations 403; unchanged DB | `memberCannotCreateReadOrModifyPrivateProjectTask`, `unassignedMemberCanReadButCannotMutateSharedTask`, `crossProjectIdentifierSwappingIsHiddenAndCannotChangeCommittedState` |
| Responsible IDs | Reject fractional/out-of-range/nonpositive values without writes; preserve exact valid integers | Earlier 14 focused `TaskControllerTest` cases plus later raw-JSON decimal regressions included in the 199-backend isolated run; scope in verification |
| E01/E02 browser creation | Login → project → create → same-project list, reload | `TaskCreationE2E.loginOpenProjectCreateTaskAndRemainInItsTaskList` |
| R01 mutations/counters | Exact persistence/filter/counter results after state/priority edits | `TaskPostgresIT.authorizedTaskMutationsCommitAndRefreshExactFiltersAndCounters`; browser collaboration method |
| UI permission/lifecycle/errors | Authorized operations, denied operations, draft retention, no false success, busy/deletion/dependencies | All 5 methods of `TaskCreationE2E`, listed in verification |
| Description/comment Unicode | Preserve limits, stripping, nullable/PATCH semantics and code-point counting | Later focused backend/frontend cases included in the latest 199-backend/45-frontend isolated run; not post-fix browser/database verification |
| Frontend helpers/build | Error propagation, project/task flow assertions and code-point checks; build assets | All `frontend/src/*.test.js`: 45 in the latest isolated run (43 in the earlier baseline); `npm run build`: 18 modules |

Commands: from repository root `mvn -B -ntp -f backend/pom.xml test`, `mvn -B -ntp -f backend/pom.xml -Pintegration verify`; separately from `backend/` `mvn -B -ntp -Pe2e verify` (optional Node path). Frontend: `npm --prefix frontend test`, `npm --prefix frontend run build`. README documents installation prerequisites. Keep profiles separate; each `verify` repeats the ordinary backend suite (167 cases in the earlier full baseline; latest isolated backend run: 199). Do not add repeated or overlapping runs as new unique coverage. These tests prove their fixtures, not exhaustive role/input/browser coverage. Ficha 04 minimum unit/integration/E2E/two-account checks have executed evidence; final candidate/human review and publication remain open.

## Historical U02 — isolated business-rule unit evidence (2026-11-15)

**Closed for the task-title rule.** `backend/src/test/java/pt/matriz/tasks/TaskTitleRuleTest.java` reuses the JDBC identity/membership mocking pattern from `TaskControllerTest`, but calls the public `TaskController.create` method directly: plain JUnit 5/Mockito, no Spring test context, MockMvc, HTTP, PostgreSQL or Docker. No validator extraction or production changes. This is an isolated controller-unit test of its embedded business rule, not a standalone validator or persistence test.

| Inputs | Expected and observed |
| --- | --- |
| Ordinary valid title; exactly 3 and 150 characters; whitespace-padded 3 and 150 | 201, correct Location, returned normalized title and exact INSERT arguments |
| Exactly 2 and 151 characters; padded 2 and 151 | 422 title validation error; only identity/membership reads, no additional JDBC interactions |
| Empty, whitespace-only, explicit null | Same 422/no-write result |

Executed once from `backend/`: `mvn -B -ntp -Dtest=TaskTitleRuleTest test` using installed Maven 3.9.5 and OpenJDK 21.0.2. **12 cases, 0 failures/errors/skips/flakes; BUILD SUCCESS, exit 0**, finished **2026-11-15T22:36:16Z**, Maven **19.194 s**, Surefire class **4.500 s**. Log: `backend/target/u02-title-rule-unit.log`; XML: `backend/target/surefire-reports/TEST-pt.matriz.tasks.TaskTitleRuleTest.xml:2`. Only this class executed; compilation of other test sources is not execution. No profiles, full suites, integration/E2E or builds were run. Historical totals below were not rerun. Mockito dynamic-agent/compiler warnings did not fail this run. Evidence is local ignored output, not a published artifact.

This section supersedes the historical U02 pending entry below and the earlier need to extract a validator. Existing integration boundary and browser evidence remain distinct. It does not establish all title shapes, every mutation path, description/comment limits, authorization completeness or release approval.

## Executed PostgreSQL integration — 2026-11-14

This section supersedes the older pending integration status below. Implemented in `backend/src/test/java/pt/matriz/tasks/TaskPostgresIT.java`; no production code or migration changes.

| Coverage | Integration method | Expected and observed result |
| --- | --- | --- |
| I01: create, retrieve and persist | `managerCreatesRetrievesAndCommitsTaskWithEligibleAssignment` | 201 + Location, then 200; normalized fields, TODO/MEDIUM, UTC timestamp, assignment, committed row visible on a fresh JDBC connection; both Flyway versions successful |
| Member creation | `memberCreatesWithoutAssignments` (2 cases) | 201/200 with omitted or empty assignments; omitted/null description returns and persists null |
| I02: invalid title | `invalidTitlesAreRejectedWithoutWrites` (4 cases) | Missing/blank/2/151-character titles: **422** with title error; no task or assignment created |
| Title boundaries | `trimmedTitleBoundariesAreAccepted` (2 cases) | 3/150 characters after trim accepted and persisted (integration, not a pure unit test) |
| Invalid input shape | `unsupportedCreationFieldIsBadRequestWithoutWrites` | Client-supplied status rejected with 400; no writes |
| I02: nonexistent resource | `nonexistentTaskReturnsNotFound` | 404; no writes |
| A01/A02: cross-user private resource | `memberCannotCreateReadOrModifyPrivateProjectTask` | Manager creates; authenticated member without membership gets 404 on create/read/edit; original data unchanged |
| Initial assignment authorization | `memberCannotSelfAssignDuringCreation` | Valid login/CSRF but 403 on self-assignment; no partial task/assignment |
| A03: shared resource permissions | `unassignedMemberCanReadButCannotMutateSharedTask` | 200 read; 403 title/status/responsibles changes; fields, assignments and history unchanged |
| Anonymous access | `anonymousRequestsAreUnauthorizedWithoutWrites` | 401 for read/create; persisted task unchanged and no anonymous creation |

Commands from the repository root: `mvn -f backend/pom.xml test`, then `mvn -f backend/pom.xml -Pintegration verify`. Java 21/Maven required; integration requires accessible Linux Docker and image/dependency downloads. Boot manages Testcontainers 1.21.4 and Failsafe 3.5.6. Failsafe runs `*IT`; Surefire retains the existing 129 tests.

Isolation: class-scoped disposable `postgres:17.11-bookworm`, generated credentials and random mapped port, explicit container-only datasource/Flyway properties, demo seed disabled, Flyway clean disabled, production V1/V2 migrations. Unique users/projects per test; no test-level transaction, destructive reset, shared database or Compose volume access. Teardown is Testcontainers-owned disposal. MockMvc uses real Spring Security authentication, session-bound CSRF and post-login token refresh, not mocked JDBC/authentication.

Actual runs: baseline and post-implementation normal suites passed (129 tests, 0 failures/errors/skips). First integration attempt: normal 129 passed; integration 15 run, 1 failure, 0 errors/skips due to comparing Jackson LongNode/IntNode arrays. Corrected the test to compare assignment count and numeric ID, with no production changes. The following two consecutive `-Pintegration verify` runs each passed normal 129 + integration 15 = **144 tests, 0 failures, 0 errors, 0 skipped**. Latest integration-class elapsed time: 18.69 s (previous successful run: 19.39 s). Reports are in `backend/target/surefire-reports/` and `backend/target/failsafe-reports/`.

Correction to the historical matrix: semantic title validation is **422**, not 400 (`ApiProblems.validation` and OpenAPI's UnprocessableEntity response); unsupported creation fields remain 400. E2E, pure unit validation extraction, restart durability and full independent security review remain outside this implementation. No environment blocker occurred.

## Executed browser E2E — 2026-11-14

E01/E02 now have an automated happy-path implementation: `backend/src/e2e/java/pt/matriz/tasks/TaskCreationE2E.java`, launched through the `e2e` Maven profile. Playwright Java 1.58.0 controls headless Chromium against the real React UI and Spring Boot HTTP application. `frontend/e2e/server.mjs` provides a private Vite server with an API proxy to the randomly assigned isolated backend port; it ignores project Vite config/environment files and never targets the existing application.

The test creates a unique BCrypt account/project/membership in disposable PostgreSQL, then performs browser login → projects → open the seeded project → select DONE/HIGH filters → create a uniquely titled task → assert success feedback, same project heading, cleared filters and exactly one TODO/MEDIUM task card with its open button. A JDBC check confirms the committed task belongs to that project. Reloading and reopening the project verifies the task remains listed. No application response or authentication is mocked.

Datasource and Flyway credentials/URLs are explicitly container-only; existing V1/V2 migrations run, demo seed and Flyway clean are disabled, and no shared data or Compose volumes are accessed/reset. Private backend/Vite ports are ephemeral. Cleanup affects only test-owned processes/containers. Browser failure artifacts and server logs are under ignored `backend/target/e2e-artifacts/`.

Reproduction prerequisites and commands are in README's Browser E2E section: Java 21, Maven, compatible Node/platform-correct frontend dependencies, Linux Docker and download access; from `backend/`, `mvn -Pe2e test-compile exec:java` installs Chromium, then `mvn -Pe2e verify` executes the test. Missing prerequisites are errors, not silent skips. Run PostgreSQL API tests with the separate `integration` profile.

Actual evidence: initial browser attempt reached the project but errored on an overly strict exact-label selector for a select whose accessible name includes option text (1 browser test, 0 failures, 1 error, 0 skipped). Fixed only the test to use combobox role and name prefix. Two subsequent complete E2E-profile executions each passed **129 normal tests + 1 browser test, 0 failures/errors/skips**. First successful browser class: 32.35 s. Frontend rerun passed **43 tests, 0 failures/cancelled/skipped/todo**, 390.1877 ms; production build passed, 18 modules, 446 ms. No environment blocker remains. This does not cover production HTTPS/proxy, other browsers, negative browser scenarios or a complete independent security review; older pending-E2E notes below are historical.

## Historical planning matrix — superseded by current executed matrix

The original Portuguese rows below preserve planning history, including incorrect 400 expectations and pending labels. Current semantic validation is **422**, and current execution status is the matrix at the top; these rows are not outstanding tasks.

### Referências e âmbito

Baseado na Ficha Prática Laboratorial 04, `AGENTS.md`, `docs/requirements.md`, `docs/matriz-openapi.yaml` e na implementação atual de tarefas. Estes são **cenários planeados**, não resultados de execução.

| ID | Nível | Cenário / entrada | Resultado esperado | Evidência |
|---|---|---|---|---|
| U01 | Controller/validação | Criar tarefa com título apenas espaços | HTTP 400; não criar registo | `TaskControllerTest.invalidTaskTitleIsRejected` (por executar) |
| U02 | Isolated unit | Task title: valid, invalid and trimmed 3/150 boundaries | 3/150 accepted; 2/151 and empty/blank/null rejected without writes | **Executed: 12 cases passed**, `TaskTitleRuleTest`; exact evidence above |
| I01 | Integração | Gestor cria tarefa e consulta pelo ID | HTTP 201, depois 200; campos e BD coerentes | Pendente: BD de teste isolada |
| I02 | Integração | Título inválido e recurso inexistente | 400 e 404, sem escrita | Pendente |
| A01 | Autorização | Utilizador sem membership tenta criar tarefa | 404; sem escrita | `TaskControllerTest.userWithoutProjectMembershipCannotCreateTask` (por executar) |
| A02 | Autorização | Utilizador sem membership tenta consultar tarefa | 404; sem fuga de dados | `TaskControllerTest.userWithoutTaskMembershipCannotReadTask` (por executar) |
| A03 | Autorização | Membro não gestor tenta alterar responsáveis | 403; sem escrita | Pendente: teste com duas contas e BD isolada |
| E01 | E2E | Login → projeto → criar tarefa → ver na lista | Tarefa aparece na interface após criação | Pendente: Playwright com seletores reais |
| R01 | Regressão | Alterar estado de tarefa | Histórico e contagens atualizados | Pendente |

## Decisões humanas e limites

- Não assumir que estar autenticado implica ter autorização para um projeto/tarefa.
- Para a Ficha 04, o teste de integração **tem de usar BD de teste isolada** e verificar persistência real; mocks não chegam.
- Os testes `@WebMvcTest` são testes da camada HTTP/controlador, não substituem o teste unitário puro, a integração nem o E2E pedidos na ficha.
- Não usar dados de produção, credenciais reais ou uma base de dados partilhada.
- A Ficha 04 permite documentar impedimentos técnicos para E2E, mas apenas com evidência.
- A confirmação de resultados e a aprovação de alterações pertencem à revisão humana.
