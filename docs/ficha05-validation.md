# Ficha 05 — registo de validação

## Current reconciliation — 2026-11-16

**Partial delivery; local execution evidence passed.** Latest isolated verification after the later description/comment Unicode and raw-decimal fixes: **244 passes = 199 backend + 45 frontend**, zero failures/errors/skips, production build passed. Earlier full-regression baseline after responsible-ID/title-name fixes: **252 distinct tests = 167 backend + 37 PostgreSQL + 5 browser + 43 frontend**, zero failures/errors/skips, all five command exits **0**, production build passed. PostgreSQL/browser suites were not rerun after the later fixes; no complete all-layer regression after them is claimed. Exact recorded commands/results/evidence: [verification](verification.md). Overlapping runs and repeated backend cases under Maven `verify` are not additional unique coverage. This documentation update ran no suites, builds, Docker commands or secret checks.

Historical helper scripts and generated configurations under ignored `backend/target/` are local evidence references, not supplied reproduction dependencies. Use the [README](../README.md) for fresh setup and the checked-in test sources/profiles for verification. Preserve the dated runtime records below; never attach raw resolved configurations, credential-bearing logs or private session data to the submission.

Existing isolated Compose startup, communication, task persistence after all-container recreation and log checks passed on 2026-11-14; healthy/unhealthy backend startup gating passed on 2026-11-15 (records below). These are prior runtime evidence, not post-fix Docker rebuild/revalidation. Current config has **all three healthchecks**, backend non-root `USER matriz`, and frontend `service_healthy` gating. The removed stray test-source `y`, absent-backend-healthcheck and pending-integration/E2E statements in historical sections are **not current blockers**. Frontend HTML probe is not itself an API/DB probe; separate backend probe supplies startup readiness.

Configuration inspection confirms `.github/workflows/ci.yml` triggers push/PR, `contents: read`, separate integration/E2E jobs, frontend `npm ci/test/build`, and Docker builds gated on **all three** test jobs. Backend image uses `-DskipTests package`: image build is not test execution. Commands in README match Maven profiles, npm scripts, Docker build contexts and Compose service names. No remote Actions run/commit ID or controlled-failure/recovery evidence exists in the inspected records: **mandatory Ficha 05 Ex. 04/06 evidence remains missing**. Final secret-reviewed file selection, identified GitHub delivery snapshot/clean checkout and human candidate acceptance remain open. Production release/public deployment, scanners and registry publication are not mandatory coursework deliverables.

## Historical status — 2026-11-14

**Estado atualizado em 2026-11-14:** validação Compose isolada aprovada e executada: ambas as imagens construídas, stack iniciada, frontend/API/DB e persistência após recriação dos três containers verificados com sucesso. Stack existente não acedida nem alterada nesta execução aprovada. As secções de inspeção/aprovação e matriz histórica abaixo preservam o registo anterior; não representam o estado atual.

## Backend healthcheck and frontend startup gate — runtime evidence (2026-11-15)

**PASS for healthy startup and controlled unhealthy-backend startup gating.** Executed **2026-11-15T22:46:41.881122Z–22:50:57.006586Z** using Docker Compose **v2.27.0-desktop.2** on the available Docker Desktop Linux engine. This section supersedes the runtime-pending statement in the configuration/build record below, without rewriting historical evidence. No production source, Dockerfile or canonical Compose configuration changed.

### Isolation and exact execution

Read-only inventories established **6 containers, 25 volumes and 5 networks** before execution. A local ignored Python helper (`backend/target/health-runtime-check.py`) generated unique project **`matriz-hc-20261115224641-eddc0e`**, random disposable DB credentials, an empty env file and resolved JSON Compose files under `backend/target/matriz-hc-20261115224641-eddc0e/`. It removed inherited Compose/DB/demo/session/server/Spring variables, set `COMPOSE_DISABLE_ENV_FILE=1`, supplied only the generated DB password and disabled demo seeding. The repository `.env`, backups and existing databases were not read.

Before any build/start, assertions rejected external resources, fixed container names, alternate network modes, links, configs/secrets and unexpected mounts; verified that only DB mounts the new named volume, neither DB nor backend publishes host ports, and frontend depends on backend `service_healthy`. The generated config retained the canonical healthcheck command and timing unchanged. Isolation-only changes were unique backend/frontend image tags, project-scoped network/volume names and frontend **`127.0.0.1:0:80`** ephemeral publication. Images were built from the current sources; existing tags were not overwritten. No shared Docker socket or host-data mount was given to any test container.

Executed launcher from repository root: `python backend/target/health-runtime-check.py` (**exit 0**). The exact underlying command arguments/exit codes/durations are saved in private `summary.json`. Equivalent repository-relative command transcript below uses shell variables solely to avoid repeating the same literal arguments; absolute paths were used by the helper:

```bash
P=matriz-hc-20261115224641-eddc0e
D=backend/target/matriz-hc-20261115224641-eddc0e
# Pre/post read-only resource inventories:
docker ps -aq --no-trunc
docker volume ls -q
docker network ls -q --no-trunc
# Environment isolation and generated password are supplied by the helper, not .env:
docker compose --env-file "$D/empty.env" -p "$P" -f compose.yaml config --format json
# Helper writes the isolated resolved config and controlled-failure variant, then:
docker compose --env-file "$D/empty.env" -p "$P" -f "$D/compose.json" config -q
docker compose --env-file "$D/empty.env" -p "$P" -f "$D/compose-unhealthy.json" config -q
docker compose --env-file "$D/empty.env" -p "$P" -f "$D/compose.json" build
docker compose --env-file "$D/empty.env" -p "$P" -f "$D/compose.json" up -d --no-build --wait --wait-timeout 240
docker compose --env-file "$D/empty.env" -p "$P" -f "$D/compose.json" ps -aq
docker compose --env-file "$D/empty.env" -p "$P" -f "$D/compose.json" port frontend 80
docker events --since 1794782835 --until 1794782869 --filter type=container --filter "label=com.docker.compose.project=$P" --format '{{json .}}'
# Remove ONLY this newly created project's app containers to retest initial startup:
docker compose --env-file "$D/empty.env" -p "$P" -f "$D/compose.json" rm -s -f frontend backend
docker compose --env-file "$D/empty.env" -p "$P" -f "$D/compose.json" ps -aq
docker compose --env-file "$D/empty.env" -p "$P" -f "$D/compose-unhealthy.json" up -d --no-build --wait --wait-timeout 240
docker compose --env-file "$D/empty.env" -p "$P" -f "$D/compose-unhealthy.json" ps -aq
docker exec 81b1dcec782ec587e2787664f951535f25728f2fdaaca5a5fb5a9540dd273c56 curl --silent --show-error --noproxy '*' --max-time 5 --write-out '\nHTTP_STATUS=%{http_code}\n' http://127.0.0.1:8080/api/health
# Verify labels on all test-owned containers, then dispose ONLY this project's resources:
docker compose --env-file "$D/empty.env" -p "$P" -f "$D/compose.json" ps -aq
docker compose --env-file "$D/empty.env" -p "$P" -f "$D/compose.json" down --volumes --timeout 15
```

The helper also ran `docker inspect <IDs returned by this project's ps -aq>` to assert project-label ownership and collect status/health logs/restart counts; it did not inspect existing containers' environment. Python `urllib.request.urlopen` requested the test frontend's `/` and `/api/health` at **127.0.0.1:32816**, with 10-second timeouts. Commands above are a record, **not instructions to rerun against the already-disposed resources**. For repetition create a fresh project/directory/credentials and recheck isolation. Generated resolved config and source-config logs contain disposable credentials: do not print, commit or publish these private files.

### Exact outcomes

| Check / private log | Observed outcome |
| --- | --- |
| Source config and both generated `config -q` checks | All **exit 0**; isolation/dependency/probe assertions passed before starting resources |
| `build` / `build.log` | **Exit 0**, **30.713 s**; unique tags `matriz-hc-20261115224641-eddc0e-backend:validation` and `matriz-hc-20261115224641-eddc0e-frontend:validation` |
| Healthy `up` / `healthy-up.log` | **Exit 0**, **30.538 s**; DB/backend/frontend all `running` and `healthy`, restart counts **0** |
| Backend Docker probe | Real unchanged curl probe succeeded, exit **0**; first recorded success ended **22:47:34.665948780Z** |
| Initial startup ordering / `healthy-events.log` | Backend `health_status: healthy` at **timeNano 1794782854666877346**; frontend `start` at **1794782855642230301**, approximately **0.975 s later**. Frontend StartedAt **22:47:35.627822544Z**. This verifies the actual dependency gate, not only final healthy state |
| Test nginx HTTP | `/` **200**, HTML 426 characters; `/api/health` **200**, `{"status":"UP"}` through nginx → backend → PostgreSQL |
| Test-only app removal / `remove-test-app-containers.log` | **Exit 0**, **2.042 s**; removed this project's backend/frontend only; its DB remained running/healthy with the same ID/start time and disposable volume |
| Controlled unhealthy setup | Only generated failure config changed backend `DB_URL` to **`jdbc:postgresql://db:1/matriz`** (unused port on its own isolated DB service) and `SPRING_FLYWAY_ENABLED=false`. Disabling migrations for this phase allows HTTP startup while actual DB-backed health fails; no real DB was stopped/corrupted and no fake health command was used |
| Unhealthy `up` / `unhealthy-up.log` | **Expected exit 1**, **173.337 s**, before 240-second wait timeout: `dependency failed to start: container matriz-hc-20261115224641-eddc0e-backend-1 is unhealthy` |
| Unhealthy backend | Container **running**, health **unhealthy**, failing streak **10**, restartCount **0**. Retained probe log shows real HTTP **503**, curl **exit 22**; final captured failure ended **22:50:44.941985557Z** |
| Direct failure endpoint / `unhealthy-direct-http.log` | Curl command **exit 0** because this diagnostic intentionally omitted `--fail`; body **`{"status":"DOWN"}`**, **HTTP_STATUS=503**, **3.431 s**. The actual Docker probe retained `--fail` and failed correctly |
| Frontend gate under failure | New frontend container remained **`created`**, StartedAt **`0001-01-01T00:00:00Z`**, health absent, restartCount **0**: it was created but **never started** while backend was unhealthy. DB remained healthy |
| Project disposal / `cleanup-test-project.log` | **Exit 0**, **5.508 s**; only this project's containers, disposable PostgreSQL volume and network removed. No pruning/image removal/shared-resource cleanup |
| Preservation | Every pre-existing container ID, volume name and network ID remained in post-run inventory; **6/25/5 baseline resources preserved**, no remaining test-project containers/volumes/networks. Separate final container listing retained the same six running IDs/names. Unique validation images and private local evidence were intentionally retained |

### Scope and limitations

This closes runtime evidence for the configured backend probe and frontend **initial startup** `service_healthy` gate in the tested Compose/Docker environment. It does not prove automatic shutdown/restart of an already-running frontend after later backend degradation: Compose dependency gating is not continuous supervision. The controlled failure used a test-only unreachable DB port plus disabled Flyway, not a live database outage or a healthy→unhealthy transition within the same backend container. Recovery after the failed startup was not tested. Default probe timing was retained; no arbitrary timing reduction was used. No browser E2E, application-data flows, HTTPS, remote CI, multi-platform compatibility or full test suite ran. Dockerfile builds use `-DskipTests package`/frontend build and are not test-suite evidence. No production defect was observed or fixed. Only this document and ignored local runtime artifacts/helper were changed; existing uncommitted work was preserved. No commits or push.

## Backend healthcheck — approved configuration/build validation (2026-11-15)

This update supersedes the historical statements below that the backend has no Docker healthcheck. Only `backend/Dockerfile`, `compose.yaml` and related documentation changed; no existing containers, databases or volumes were accessed or modified.

- Compose backend now probes unauthenticated `GET http://127.0.0.1:8080/api/health` with exec-form curl: `--fail --silent --show-error --noproxy '*' --connect-timeout 2 --max-time 5 --output /dev/null`. HTTP 503/connection failure/timeouts fail the probe. `HealthController` executes `SELECT 1`; `SecurityConfig` explicitly permits anonymous GET on this endpoint. No credentials/session/CSRF are needed.
- Timing: interval 10 s, Docker timeout 6 s (longer than curl's 5 s limit), start period 40 s, 10 retries. This gives startup grace without an unlimited probe.
- Frontend now depends on backend `service_healthy`, rather than mere container startup, because nginx forwards API requests to it. This gates initial Compose startup only; it does not automatically stop/restart nginx if backend later becomes unhealthy. The existing frontend HTML healthcheck is unchanged.
- Runtime base `eclipse-temurin:21-jre` was inspected in a temporary network-disabled container: `/usr/bin/curl` 8.18.0 exists. Dockerfile checks `command -v curl` during build, so a future mutable-base change removing curl fails clearly rather than creating a broken probe. No package was added. Healthcheck is Compose configuration, not image-level HEALTHCHECK.

Actual validation:

| Check | Result |
| --- | --- |
| `docker compose --env-file <empty-temporary-file> -p matriz-healthcheck-config-validation -f <absolute-compose.yaml> config -q` | **Exit 0**. Used a filtered process environment, `COMPOSE_DISABLE_ENV_FILE=1`, fixed validation-only DB placeholder and seed disabled; existing `.env` never loaded or printed. |
| Same isolated invocation with `config --format json`, captured in memory | **Passed assertions**: backend probe URL correct and frontend dependency is `service_healthy`; printed only the non-secret healthcheck block. |
| `docker build -t matriz-healthcheck-backend:20261115-review-01 ./backend` | **Exit 0**; unique image tag, build-time `/usr/bin/curl` check passed. Log: ignored `backend/target/healthcheck-backend-build.log`. Existing image tags not replaced. Dockerfile uses `-DskipTests package`; this is a build, not a test-suite execution. |
| `docker run --rm --network none --entrypoint /bin/sh matriz-healthcheck-backend:20261115-review-01 -c 'id -un; command -v curl; curl --version'` | **Exit 0**; runtime user `matriz`, `/usr/bin/curl`, version **8.18.0**. Temporary container only, no network or volume mounts; application not started. |
| `git diff --check -- backend/Dockerfile compose.yaml` | Passed. |

Remaining verification: the new Compose probe's actual healthy/unhealthy transitions and frontend readiness gate have **not** been exercised against a running stack. No `compose up/down`, restart/recreation, DB access, volume operations, full suites or browser E2E were run. A future runtime check requires an explicitly approved isolated environment; existing running containers retain their previous configuration. No commits/pushes; individual tool approvals retained.

## CI/CD — revisão e validação local em 2026-11-14

Alterações limitadas a `.github/workflows/ci.yml` e a este registo. Sem push/commit, alterações de aplicação/POM/package.json, acesso a dados existentes ou operações em volumes.

### Diagnóstico e alterações mínimas

- Antes: `mvn verify` sem perfil executava apenas os 129 testes normais. O serviço PostgreSQL configurado no workflow não era usado por estes testes; os 15 casos `TaskPostgresIT` e o browser `TaskCreationE2E` não estavam selecionados.
- Job backend agora usa `mvn -B -ntp -Pintegration verify`, com timeout de 15 minutos. Removido serviço PostgreSQL/credenciais estáticas desnecessários: Testcontainers cria a própria base isolada e executa Flyway.
- Adicionado job `e2e` independente (20 minutos), Java 21/Node 24, `npm ci`, instalação Chromium/dependências Linux com `mvn -B -ntp -Pe2e test-compile exec:java -Dexec.args="install --with-deps chromium"`, depois `mvn -B -ntp -Pe2e verify`. Não combinar perfis: as regras Failsafe diferem. Este comando também repete os 129 testes normais por simplicidade, sem alterar o POM.
- Frontend mantém instalação por lockfile, 43 testes e production build. Job Docker mantém builds dos Dockerfiles, mas depende agora de backend, frontend **e e2e** passarem. Nenhum push de imagens/deploy foi adicionado.
- Mantidos triggers push/pull_request e permissões mínimas `contents: read`. Não são necessários secrets de projeto para estes testes; PostgreSQL é descartável e credenciais dos testes são geradas.

Decisão: incluir ambos os perfis em cada push/PR. São apenas 15 casos de integração e um cenário Chromium; histórico local anterior registou cerca de 19 s e 32 s por classe, excluindo preparação/downloads. Integração deteta SQL/migrations/commit real; browser protege a regressão de navegação que testes Node não renderizados não cobrem. Jobs separados correm em paralelo e timeouts limitam hangs. Downloads Docker/Chromium/Maven e apt Linux podem dominar primeira execução; fiabilidade/tempo reais no runner ainda exigem medição. Não adicionar retries que ocultem falhas, browsers adicionais, deploy ou cache de browser neste ajuste mínimo.

### Comandos e resultados locais reais

| Comando/check | Resultado desta execução |
| --- | --- |
| `docker run --rm --network none --mount type=bind,source=<workflow>,target=/work/ci.yml,readonly rhysd/actionlint:1.7.7 -shellcheck= -pyflakes= /work/ci.yml` | **Exit 0**, sem diagnósticos: YAML/estrutura Actions/expressões/job dependencies validados. Shellcheck/Pyflakes explicitamente desligados; não é execução do workflow. Container temporário sem rede runtime ou volumes; só workflow montado read-only. |
| `python -c "import yaml"` | PyYAML não instalado; usada ferramenta especializada actionlint em vez de instalar pacotes Python. |
| `mvn -B -ntp -Pintegration verify` (backend) | **BUILD FAILURE**, compilação de testes, 4.878 s; **nenhum teste executado**. |
| `mvn -B -ntp -Pe2e test-compile exec:java -Dexec.args="install chromium"` (equivalente Windows sem apt Linux) | **BUILD FAILURE**, mesma compilação, 3.994 s; browser installer não chegou a executar. `-Pe2e verify` condicionado a sucesso não foi iniciado. |
| `npm.cmd test` (frontend) | **43 passed, 0 failed, 0 cancelled, 0 skipped, 0 todo**, 308.3071 ms. |
| `npm.cmd run build` | **Sucesso**, 18 módulos, 357 ms; Vite 8.3.8. |
| `docker build -t matriz-ci-check-backend:20261114-review ./backend` | **Falha**, Docker build process exit 1 na etapa `mvn -DskipTests package`, mesma compilação. `skipTests` não elimina testCompile. |
| `docker build -t matriz-ci-check-frontend:20261114-review ./frontend` | **Sucesso**, imagem criada; executa `npm ci` e build em Linux. Tags próprias, nenhuma imagem da stack existente substituída. |

**Bloqueador de código existente, não corrigido nesta tarefa CI:** `backend/src/test/java/pt/matriz/tasks/TaskControllerTest.java:349` contém um `y` solto entre métodos. O compilador reporta `illegal start of type` em 351 e `<identifier> expected` em 352. O mesmo problema foi confirmado no Docker Linux. Preservado o ficheiro/trabalho não commitado, sem mascarar o problema com flags de skip. Requer aprovação/correção separada, depois repetir integração, E2E e build backend. Os relatórios XML/TXT antigos com 144/130 testes passados são históricos e **não** resultados destas tentativas.

Logs locais desta etapa em `backend/target/ci-integration-check.log`, `ci-browser-install-check.log`, `ci-docker-backend-check.log`, `ci-docker-frontend-check.log`. A saída redirecionada do shell não propaga de forma fiável exit codes à ferramenta externa; os resultados acima foram confirmados pelo conteúdo dos logs, não apenas por ausência de saída. Não existe `ci-e2e-check.log` desta tentativa, pois o comando anterior falhou.

### O que ainda exige GitHub Actions real

Nenhum workflow remoto foi disparado: não houve push, PR, commit nem acesso a secrets. Falta confirmar checkout do conjunto completo de ficheiros atualmente não commitados, resolução de actions/caches, Docker/Testcontainers no runner Ubuntu, instalação Chromium com `--with-deps`, aprovisionamento de permissões apt, duração dos jobs/cold downloads, nomes/checks obrigatórios na proteção de branch, bloqueio real do job Docker quando testes falham e evidência remota de sucesso. CI falha controlada continua pendente; não inserir um teste falhado nem alterar branch sem autorização. Backend local está atualmente bloqueado pela sintaxe acima. A pipeline só valida/testa/constrói: não é CD/deploy nem publicação de imagens.

## Resultado executado — ambiente isolado aprovado

Recursos exclusivos desta validação:

- Projeto: `matriz-f05-20261114230744-c540f4`.
- Imagens próprias: `matriz-f05-20261114230744-c540f4-backend:validation` e `matriz-f05-20261114230744-c540f4-frontend:validation`.
- Rede: `matriz-f05-20261114230744-c540f4_default`.
- Volume novo e **retido**: `matriz-f05-20261114230744-c540f4_postgres_data`.
- Publicação exclusiva: `127.0.0.1:61629` → nginx:80; sem portas host de backend/PostgreSQL.
- Credenciais geradas apenas para testes, nunca impressas; seed demo ativado exclusivamente nesta base nova para criar contas/projetos sintéticos. Sem leitura de `.env`.

### Comandos e isolamento

Foi usado o helper local não versionado `backend/target/ficha05-validate.py`, em diretório já ignorado. Antes de qualquer build/start, o helper resolveu a configuração real com `docker compose --env-file <ficheiro-vazio> -p <projeto-único> -f compose.yaml config --format json`, em ambiente filtrado e com valores de teste gerados. O JSON foi capturado sem impressão; depois foram substituídos nome do projeto, tags, rede, volume e porta. Foram confirmados: três serviços esperados, ausência de bind mounts/env_file/container_name, apenas rede própria e volume nomeado não externo; inexistência prévia dos recursos; `config -q` válido. Antes de recriar foram novamente verificadas labels de propriedade e mounts de todos os containers-alvo.

Sequência efetivamente executada da raiz (Python 3.13):

```text
python backend/target/ficha05-validate.py prepare
python backend/target/ficha05-validate.py build
python backend/target/ficha05-validate.py start
python backend/target/ficha05-validate.py data
python backend/target/ficha05-validate.py data       # após corrigir email do helper
python backend/target/ficha05-validate.py data       # após corrigir envelope items
python backend/target/ficha05-validate.py recreate
python backend/target/ficha05-validate.py logs
# nova leitura após 12 segundos para aguardar healthcheck nginx
python backend/target/ficha05-validate.py logs
git diff --check
```

Todos os comandos Compose mutáveis usaram explicitamente os mesmos argumentos de isolamento:

```text
docker compose --env-file backend/target/ficha05-isolated/empty.env -p matriz-f05-20261114230744-c540f4 -f backend/target/ficha05-isolated/compose.json config -q
docker compose --env-file backend/target/ficha05-isolated/empty.env -p matriz-f05-20261114230744-c540f4 -f backend/target/ficha05-isolated/compose.json build
docker compose --env-file backend/target/ficha05-isolated/empty.env -p matriz-f05-20261114230744-c540f4 -f backend/target/ficha05-isolated/compose.json up -d --no-build
docker compose --env-file backend/target/ficha05-isolated/empty.env -p matriz-f05-20261114230744-c540f4 -f backend/target/ficha05-isolated/compose.json up -d --no-build --force-recreate
```

O helper também executou `ps -a -q`, inspeção seletiva dos containers próprios e `logs --no-color --tail 150` com estes mesmos argumentos. Configuração gerada, estado com passwords, build.log e runtime-private.log ficam **apenas localmente** em `backend/target/ficha05-isolated/`; não publicar/copiar o seu conteúdo, pois pode conter secrets de teste. O helper captura logs, não os imprime. Não executar `prepare` de novo no mesmo diretório: recusa sobrescrever o estado. Para repetir uma validação independente, gerar novos nomes/credenciais/porta e nova pasta privada; não apontar o helper para a stack existente. Os ficheiros locais não são uma nova ferramenta de produção nem fazem parte da entrega versionada.

### Resultados observados

| Verificação | Resultado real |
| --- | --- |
| Isolamento pré-operação | Verificado; novo projeto/rede/volume/tags; `.env` não carregado |
| Build backend e frontend | `build` terminou com **código 0**; Dockerfiles originais, sem alterações |
| Arranque | `up -d --no-build` concluído; backend e DB arrancaram; frontend inicialmente `starting`, depois `healthy` |
| HTML frontend pelo nginx | **HTTP 200**, documento contém root React |
| `/api/health` pelo nginx | **HTTP 200**, `{"status":"UP"}`; prova nginx→backend→consulta PostgreSQL |
| Login com CSRF/sessão e token renovado | **HTTP 200**, usando conta sintética do seed exclusivo |
| Listagem projetos | **HTTP 200**, envelope `items`; projeto sintético selecionado ID **2** |
| Criar tarefa | **HTTP 201**, ID **1**, título exclusivo `Ficha05 persistence matriz-f05-20261114230744-c540f4` |
| Consultar tarefa | **HTTP 200**, resposta igual à criação |
| Recriação segura | `up -d --no-build --force-recreate`; IDs dos **três** containers mudaram; mount continua no mesmo volume de teste |
| Persistência | Novo login **200** após recriação; tarefa **200** com todos os campos iguais à resposta original; listagem do projeto contém exatamente uma ocorrência do ID |
| Estado final | DB `healthy`, frontend `healthy`, backend `running` sem healthcheck configurado; todos com restartCount **0** |
| Logs finais | **76 linhas** recentes; **0** linhas com marcador ` ERROR `, **0** com ` WARN `; marcadores PostgreSQL ready, Spring Boot started, nginx workers e Flyway schema up to date presentes. Não é uma análise exaustiva de todos os formatos/severidades. |
| Whitespace Git | `git diff --check` passou |

IDs finais (apenas stack de teste): backend `439e95549bae`, DB `60e8ec94bb73`, frontend `1896653c590d`.

### Falhas encontradas e correções locais

1. Primeiro login retornou **401**: helper usava email errado `gestor@matriz.local`. Inspeção de `DemoSeed.java` confirmou `gestor@example.org`; corrigido apenas o helper. Não houve alteração da aplicação nem das credenciais existentes.
2. Após login corrigido, helper falhou com **KeyError: 0** ao tratar listagem de projetos como array. Controller devolve envelope `items`; corrigido parsing de projetos e tarefas no helper. Nenhuma tarefa foi criada nas duas tentativas falhadas. A tentativa seguinte criou/consultou a tarefa e completou a persistência.
3. Não ocorreram falhas de build ou runtime Compose nesta execução.

### Preservação e lacunas restantes

Nenhum comando desta execução aprovada inspecionou/consultou containers ou dados da stack existente. Verificações globais anteriores a operações limitaram-se à inexistência dos nomes exclusivos de recursos. Não se executou `down`, `down -v`, remoção de volumes, reset de DB ou limpeza destrutiva. A recriação removeu/substituiu **apenas containers do projeto de validação**, conforme aprovação, preservando o seu volume. Os três containers de validação ficam **em execução** e o volume permanece retido; qualquer paragem/limpeza futura deve indicar explicitamente o projeto correto, e remoção de volume exige nova aprovação. Secrets existentes e trabalho não commitado preservados. Alteração de entrega limitada a este documento; auxiliares gerados apenas em `backend/target/`. Sem commits.

Lacunas: backend não tem healthcheck Docker/readiness gate para frontend; healthcheck nginx cobre HTML, não API. Não foram modificados esses comportamentos. HTTPS/cookies Secure, falhas induzidas de DB, auditoria de dependências/imagens, CI remoto e falha controlada não validados aqui. O browser E2E anterior usa Vite/Testcontainers, não a imagem nginx: nesta validação Compose a navegação/login/criação foi verificada por HTTP/API, não por browser. Suites backend/frontend não foram repetidas nesta etapa Docker; os builds não equivalem a nova execução dos testes. Sem bloqueador para os cenários Compose acima.

## Execução atual — inspeção segura e ponto de aprovação

Comandos efetivamente executados na raiz:

| Comando | Resultado observado |
| --- | --- |
| `docker compose version` | Docker Compose v2.27.0-desktop.2 |
| `docker ps --format '{{.Names}}\t{{.Status}}\t{{.Ports}}'` | Stack existente: `matriz_backup-frontend-1` Up 9 hours (healthy), `127.0.0.1:8080->80/tcp`; `matriz_backup-backend-1` Up 9 hours, porta interna 8080; `matriz_backup-db-1` Up 9 hours (healthy), porta interna 5432. Isto é estado Docker, não prova de resposta HTTP ou prontidão do backend. |
| `docker compose config -q` | Sucesso, código 0; configuração resolvida sem imprimir valores de ambiente. |
| `git ls-files --error-unmatch .env` com saída suprimida | `.env` não versionado. |
| `git check-ignore -q .env` | `.env` ignorado pelo Git. |
| `git status --short` | Trabalho não commitado existente identificado e preservado. |

Ficheiros inspecionados: `compose.yaml`, ambos os Dockerfiles e `.dockerignore`, `frontend/nginx.conf`, `.env.example`, propriedades Spring e implementação de `/api/health`. O ficheiro `.env` não foi lido nem os seus valores expostos.

Constatações estáticas:

- PostgreSQL `17.11-bookworm`, volume persistente `postgres_data`; healthcheck `pg_isready`. Isto não comprova sozinho que as migrations ou consultas da aplicação funcionam.
- Backend multi-stage Maven/Java 21, utilizador runtime não-root, sem publicação de porta host. Não existe healthcheck Docker do backend; `depends_on` aguarda apenas a base de dados saudável.
- Frontend multi-stage Node 24.16.0 e nginx 1.27-alpine; publicação loopback 8080. Healthcheck apenas do HTML `/`; não comprova API/DB. Nginx encaminha `/api/` para `backend:8080`.
- Endpoint `/api/health` executa `SELECT 1`, devolvendo UP/200 ou DOWN/503. Ainda não consultado nesta validação Compose.
- Credenciais via variáveis, demo seed desligado por omissão; cookies Secure desligados por omissão para HTTP local, não validação de produção HTTPS.

**Ponto de segurança:** não executar `docker compose build/up` com o projeto predefinido: reutiliza tags de imagens e pode recriar containers da stack existente. Nenhum build, arranque, stop/recreate, alteração de dados, consulta de logs da aplicação ou operação em volumes foi realizado nesta etapa. Não foi executado `down`, `down -v`, limpeza de volumes ou reset de base de dados. Nenhuma falha de build/runtime foi observada porque essas etapas ainda não foram tentadas.

Próximo passo proposto, pendente de aprovação: projeto Compose com nome único, tags de imagem próprias, rede e volume novos, password gerada não impressa, porta loopback diferente/livre e configuração isolada que não carregue `.env`. Validar build, startup, logs com cuidado para não expor secrets, HTTP HTML/API, comunicação nginx→backend→PostgreSQL; criar apenas dados sintéticos nesse ambiente, recriar apenas os seus containers mantendo o volume e verificar a persistência. Nunca remover volumes; não parar/recriar a stack existente. Se for necessária alteração dos ficheiros de infraestrutura, pedir aprovação antes de implementar.

Lacunas atuais: build de ambas as imagens, startup da stack de validação, respostas HTTP/health, logs sanitizados, login/tarefa através do proxy e persistência após recriação ainda pendentes; CI remoto, falha controlada e revisão HTTPS/dependências também não executados nesta etapa. O E2E Playwright anterior foi realizado num ambiente Testcontainers/Vite separado e não substitui validação Compose (ver `docs/test-plan.md`).

## Matriz histórica de preparação

| Verificação | Comando / procedimento | Estado |
| --- | --- | --- |
| Configuração Compose | `docker compose config -q` | Pendente |
| Build imagens | `docker compose build` | Pendente |
| Arranque | `docker compose up -d` | Pendente |
| Serviços e logs | `docker compose ps`; `docker compose logs --tail=50` | Pendente |
| Frontend + API | Abrir `http://localhost:8080` e testar login, projetos, tarefas | Pendente |
| Persistência | Criar tarefa, `docker compose down`, `docker compose up -d`, consultar novamente | Pendente |
| Testes backend | `mvn test` na pasta backend | Reportado pelo utilizador como BUILD SUCCESS, antes deste pacote |
| Build frontend | `npm.cmd run build` na pasta frontend | Reportado pelo utilizador como concluído, antes deste pacote |
| CI | Ver separador Actions após push e pull request | Pendente |
| CI falha controlada | Introduzir temporariamente teste falhado numa branch, reverter e repetir | Pendente |
| Segurança | Revisão de permissões/IDOR, secrets, cookies HTTPS e dependências | Pendente |

## Registo de problemas e decisões
- [ ] Adicionar logs, evidências, correções e commit/branch efetivos.
- [ ] Verificar que `.env` não está versionado e que `compose.yaml` não inclui credenciais reais.
- [ ] Rever e corrigir possíveis discrepâncias com OpenAPI e requisitos antes da release.
