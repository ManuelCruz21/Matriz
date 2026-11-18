# Matriz — Verificação e aceitação

## Public documentation provenance — 2026-11-16

Third-party course materials under `docs/aulas/`, including PDFs and Markdown transcriptions, remain unchanged locally and are excluded from public delivery. Historical course paths, scenario-numbering sources and line citations below are local-only provenance, not public navigation or packaging dependencies. The acceptance scenarios, results, technical evidence and historical corrections remain recorded here; public requirements are in [requirements](requirements.md). This notice does not alter test counts, rerun checks or certify delivery.

## Addendum — complete post-fix isolated regression — 2026-11-16

**Documentation-only reconciliation: no tests/builds executed for this edit.** This records the later completed isolated run after both validation fixes. Earlier execution records are preserved; statements that only focused post-fix evidence exists are superseded within this isolated scope.

| Check | Actual result | Scope / evidence |
| --- | --- | --- |
| Complete backend unit/MVC suite | **199 passed, 0 failures/errors/skips; BUILD SUCCESS** | All eight ordinary test classes, corroborated by their generated Surefire XML reports under `backend/target/surefire-reports/`. |
| All frontend tests | **45 passed, 0 failures/cancelled/skipped** | Existing Node test suite, including code-point checks, source wiring guards and mocked HTTP/flow tests. |
| Frontend production build | **Passed; 18 modules; 534 ms** | Vite production compilation; no runtime/database startup. |

### Execution safety and coverage

Before execution, all selected backend classes were inspected: six MVC slices replace JDBC with Mockito, `TaskTitleRuleTest` uses direct JDBC mocks, and `AbsoluteSessionExpiryFilterTest` has no database access. The explicit class selection included every ordinary unit/MVC class and excluded `TaskPostgresIT` and E2E sources. Maven ran **offline**, with no integration/E2E profile or dependency installation, using:

```text
backend/: mvn -o -B -ntp -Dtest=AbsoluteSessionExpiryFilterTest,AuthControllerTest,AuthenticationFlowTest,SecurityIntegrationTest,HealthControllerTest,ProjectControllerTest,TaskControllerTest,TaskTitleRuleTest -Dspring.flyway.enabled=false -Dmatriz.demo-seed.enabled=false -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration test
frontend/: npm test
frontend/: npm run build
```

Commands above describe the completed execution, not commands run by this documentation update. Existing Java 21/Maven/Node installations and dependencies were reused. No live-database requirement was encountered. No Docker, PostgreSQL, persistent data, schema, secrets or Git operations occurred; archived evidence was preserved and normal test/build outputs updated. Non-failing warnings concerned Mockito/JVM agent loading and development-security auto-configuration in a test slice. Frontend tests do not render browser forms; a successful build is not browser acceptance. No separate immutable archive, exact UTC timing or candidate commit is newly asserted for this run.

### Distinction from historical and focused evidence

- **Latest post-fix isolated result: 244 test passes = 199 backend + 45 frontend**, plus successful frontend production build.
- **Earlier full-regression baseline: 252 distinct tests = 167 backend + 37 PostgreSQL integration + 5 browser E2E + 43 frontend**, executed before these fixes. The **37 PostgreSQL and 5 browser E2E** results remain historical: neither suite was rerun after the fixes in this isolated execution.
- Earlier Unicode **18 backend + 2 frontend** and decimal **32 selected backend** passes remain focused implementation evidence. They overlap broader coverage and must not be added to 244 or 252 as a new unique total.
- Saved login checks and owner-reported authenticated responsive checks remain separately scoped evidence, not a post-fix browser rerun. **No complete post-fix integration/E2E regression is claimed.**

### Current acceptance and delivery status

The two validation limitations are implemented and supported by focused plus complete isolated regression evidence. The bounded Ex. 08 mapping remains supported across historical and later evidence; this is not exhaustive conformance or a single all-layer post-fix acceptance run. Human acceptance of results, candidate/version identity and review/AI-use/no-refactor decisions remains open. Decide separately whether additional disposable post-fix PostgreSQL/browser verification is needed; no database/browser execution is authorized here.

A proposed allowlisted delivery manifest and redacted candidate review now exist, but final file selection/publication approval, any remaining redaction, sanitized evidence packaging, identified delivery commit/GitHub submission and clean-checkout execution are still outstanding. No final secret-free or Git-history certification is claimed. Personal absolute machine paths in these two documents have been removed in favor of neutral tool prerequisites; historical versions, commands, timing and relative evidence locations are preserved. Actual remote GitHub Actions evidence for the candidate remains missing: all four job outcomes, controlled failing-test/Docker-gate proof and restored green recovery.

**Next safe step:** obtain candidate-manifest and bounded acceptance/verification-scope approval, then separately authorize remaining publication remediation and clean-checkout/remote-CI work. Production deployment/release is not mandatory by default. Only this file and `docs/final-review.md` were changed; no application code, configuration, Docker, databases, secrets or Git were modified.

## Addendum — completed Unicode and exact-decimal fixes — 2026-11-16

**Documentation-only reconciliation; no tests/builds run for this update.** This records completed focused executions, separate from the earlier 252-test baseline. Earlier findings and execution records remain preserved as history; their unresolved-description/comment and raw-decimal statements are superseded within the scope below.

### Unicode descriptions/comments and frontend parity

- Project description validation now counts Unicode code points after existing stripping on create and PATCH: **1–1000**, or null.
- Task descriptions and comments use code-point counting with the existing **1–2000** limits. Nullable descriptions, required comments, blank rejection, stored stripping and PATCH omission/null-clearing behavior are preserved.
- The three frontend textareas use code-point-aware custom validity rather than native UTF-16 maxlength. Existing title/name controls are unchanged. The counting function matches Java stripping for length checks without altering submitted text.
- **Focused backend result: 18 passed, 0 failures/errors/skips**, finished **2026-11-16 13:17:31 UTC**: 10 selected project-controller cases (8 new Unicode/null/blank boundary cases plus 2 existing PATCH-preservation checks) and 8 new direct task-controller cases exercising task create/update and comment creation.
- **Focused frontend result: 2 passed**: counting/boundaries and structural textarea-wiring guards. The counting test passed initially; the wiring guard initially failed because its pattern expected non-self-closing JSX. The pattern was corrected, only the failed case was rerun, and it passed. **Frontend production build passed** (18 modules; 444 ms). These Node tests do not render a browser; no post-fix browser interaction or PostgreSQL integration was executed.
- Implementation/test files: `backend/src/main/java/pt/matriz/projects/ProjectController.java`, `backend/src/main/java/pt/matriz/tasks/TaskController.java`, `backend/src/test/java/pt/matriz/projects/ProjectControllerTest.java`, `backend/src/test/java/pt/matriz/tasks/TaskTitleRuleTest.java`, `frontend/src/main.jsx`, `frontend/src/task-flows.js`, `frontend/src/task-flows.test.js`.

### Exact JSON decimal deserialization

- Existing application configuration now enables `spring.jackson.deserialization.use-big-decimal-for-floats=true`. The shared responsible-ID validator, authorization and controller error handling are unchanged.
- Application-mapper precision is asserted using the injected mapper. Raw-JSON MockMvc tests cover **both task creation and responsible replacement**: high-precision fractions, exact integral decimal/exponent forms (`2.0`, `2.000`, `2e0`), exact large values including `Long.MAX_VALUE`, out-of-range values and wrong types. Mixed arrays with a valid first ID are rejected atomically with existing field-specific **422 Problem Details**, without eligibility checks or task/assignment/history writes.
- **Focused backend result: 32 passed, 0 failures/errors/skips; BUILD SUCCESS**, finished **2026-11-16 13:34:59 UTC**. This consists of 16 new parameterized/test invocations (some exercise both endpoints) and 16 selected existing numeric-validation/authorization invocations, not 32 newly added distinct tests.
- Changed files for this fix: `backend/src/main/resources/application.properties` and `backend/src/test/java/pt/matriz/tasks/TaskControllerTest.java`. No frontend or ID-parser change was needed.

### Database safety and evidence boundaries

Before both backend executions, the selected tests were inspected: direct controller tests use Mockito JDBC; MVC slices use `@WebMvcTest` and `@MockitoBean JdbcTemplate`. Runs explicitly disabled Flyway/demo seeding and excluded datasource/Flyway auto-configuration. Neither run used the integration/E2E profiles, Docker or an existing database. Frontend checks exercised JavaScript/source and compiled assets only. Standard generated reports are under `backend/target/surefire-reports/` (project/task-title reports for Unicode; task-controller report for decimals); these are mutable local outputs, not an immutable packaged delivery artifact.

The complete **252 distinct tests = 167 backend + 37 PostgreSQL + 5 Java Playwright + 43 frontend** execution at **07:22–07:27 UTC** occurred **before these two fixes**. It remains the earlier full-regression baseline. **No full regression, integration or E2E run after these fixes is claimed.** Do not sum 18 + 32 + 2 into that baseline: the selected runs contain existing checks and differing scopes, and no new full-suite total was established. Owner-reported responsive checks and the saved automated login checks remain separately labeled evidence, not post-fix browser verification.

### Updated acceptance and remaining delivery blockers

- **Ex. 08 case 04:** the two recorded limitations are addressed with focused evidence. Status is **bounded focused PASS**; the combined mapping is now **9/9 cases supported across historical and focused evidence**, not a single post-fix acceptance/full-regression run or exhaustive input certification. Earlier 8 pass / 1 partial statements are retained as historical snapshots. No fix-versus-deferral decision remains for these two implemented issues.
- **Human acceptance remains:** approve the scoped fixes, combined evidence, AI-use/review and no-refactor decisions; resolve candidate identity and differing manifest versions. Agree any additional final-candidate verification scope separately. Malformed Unicode, normalization/graphemes and parser resource-limit behavior were not newly certified; no new mandatory defect is asserted for them.
- **Final delivery snapshot remains incomplete:** explicit candidate file list, separately authorized secret/publication review, sanitized evidence, identified delivery commit/GitHub submission and clean-checkout execution. No refreshed Git inventory or secret-free certification was performed here.
- **Remote CI remains missing:** commit/run identity, all four Actions job outcomes, controlled failing-test/Docker-gate proof and restored green recovery. Earlier local regression and these focused runs do not replace that evidence.

**Next safe step:** obtain candidate/human acceptance and verification-scope decisions, then separately authorize the narrowly scoped publication review and clean-checkout/remote-CI work. No tests/builds are authorized merely by this documentation reconciliation. Production deployment/release, scanners and registry publication are not mandatory by default. Only `docs/final-review.md` and this file were changed; application code, tests, configuration, Docker, databases, secrets and Git were not modified.

## Addendum — responsive evidence and remaining delivery gates — 2026-11-16

**Documentation-only record; no new test execution.** Owner-reported manual checks are distinguished below from saved automated evidence. Earlier audit statements are retained as dated history; their absence-of-responsive-evidence statements no longer describe the full evidence now reported.

### Owner-reported manual responsive checks

The owner reports inspecting the authenticated application in **Google Chrome**, with **no visible layout defects**:

| Authenticated screen / area | 375px | 768px | 1440px | Evidence type |
| --- | --- | --- | --- | --- |
| Dashboard | Manual PASS | Manual PASS | Manual PASS | Owner report |
| Projects | Manual PASS | Manual PASS | Manual PASS | Owner report |
| Project tasks | Manual PASS | Manual PASS | Manual PASS | Owner report |
| Task details | Manual PASS | Manual PASS | Manual PASS | Owner report |
| Task footer, additional focused check | Manual PASS | Not separately reported | Not separately reported | Owner report |

Recorded on 2026-11-16; exact execution time, Chrome version, viewport heights and delivered commit were not supplied. These are manual visual checks, not an automated run or an independent screenshot inspection by the assistant. They address the prior authenticated multi-width visual coverage gap within the listed scope, without certifying all roles, content sizes, interactions, browsers or accessibility behavior.

### Separate saved automated evidence — reused, not rerun

Evidence: `backend/target/responsive-ui-20261116T075729Z/login-check.log`, the three accompanying `login-*.png` files and `backend/target/ResponsiveLoginCheck.java`.

| Login viewport | HTTP | Document / body width | Measured overflow | Login controls visible |
| --- | --- | --- | --- | --- |
| 375 × 900 | 200 | 375 / 375 | None | Yes |
| 768 × 900 | 200 | 768 / 768 | None | Yes |
| 1440 × 900 | 200 | 1440 / 1440 | None | Yes |

Prior inspection verified the saved log and PNG dimensions/readability, not screenshot appearance. The checker waits for login controls, records layout metrics for selected elements and saves screenshots; it does not authenticate or capture console/page errors. Its protected-screen result is `BLOCKED_NO_AUTH`. These three login checks are separate from the owner's authenticated manual checks and do not increase the **252 distinct tests** in the saved regression. No completed tests were repeated for this addendum.

**Console observation:** the owner's reported DevTools **401 on `/api/v1/users/me`** is consistent with startup requesting the current user before authentication (`frontend/src/main.jsx`, initialization) and the API layer handling 401 by returning to the unauthenticated state (`frontend/src/api.js`). It does not establish a layout defect or mandatory delivery blocker. This is a source-consistent interpretation of the reported request, not a captured console trace or certification that the session had no other errors.

### Remaining mandatory delivery blockers and validation decisions

- **Actual remote CI:** identified tested commit/run, all four GitHub Actions job outcomes, controlled failing-test evidence proving the Docker gate is blocked/skipped and restored green recovery. Local green results and configuration checks are not remote execution evidence.
- **Complete final delivery:** explicit candidate file list, separately authorized final secret/publication review, sanitized evidence, identified Git commit/GitHub submission and clean-checkout execution. No final secret-free certification or refreshed Git-status check was performed here; prior working-tree observations remain historical.
- **Unresolved validation disposition:** description/comment UTF-16 versus database character-count semantics and raw high-precision numeric JSON/deserialization limitations. The owner/reviewer must explicitly require fixes or accept documented limitations; this does not convert partial validation into full conformance. The existing Ex. 08 result remains **8 pass / 1 partial**.
- **Human approval and candidate identity:** acceptance of fixture-bounded results, AI-use/review and no-refactor decisions, plus a decision on `v1.3`, proposed `0.1.0-rc.1` and differing manifest versions. No manifest alignment or final release approval is implied.

**Next safe step:** collect those human decisions, then separately authorize a narrowly scoped candidate/publication review before clean-checkout validation and remote submission/CI. No repeat local regression or responsive run is needed just to update this record. Production release/deployment, scanners and registry publication are not mandatory by default. Only this file and `docs/final-review.md` were edited; no application code, Docker, databases, secrets or Git were accessed or changed and no tests/builds were executed for this update.

## Assignment audit and documentation reconciliation — 2026-11-16

**Historical documentation-only audit; no suites/builds, Docker operations, databases or secret files accessed.** Local audit sources were `docs/aulas/ficha03/enunciado.md`, `ficha04/enunciado.md`, `ficha05/enunciado.md` (not example prompts); public requirements are in [requirements](requirements.md). The regression baseline at that audit was **252 distinct tests = 167 backend + 37 PostgreSQL + 5 Java Playwright + 43 frontend**, zero failures/errors/skips, all five command exits 0, production build passed. The exact execution record below remains evidence after the responsible-ID and title/name fixes, before the later description/comment Unicode and raw-decimal fixes; this audit did not rerun it. The latest isolated result is **244 passes = 199 backend + 45 frontend**, plus production build, as recorded in the opening addendum. PostgreSQL/browser suites were not rerun after the later fixes; no complete all-layer regression after them is claimed.

### Exact Ficha 03 Ex. 08 acceptance reconciliation

The retained older table below predates the later browser increments and overstates outstanding gaps. Current statuses within representative fixtures:

| Canonical case | Expected / observed evidence | Status |
| --- | --- | --- |
| 01 access | Valid login; invalid-login recovery and unauthenticated rejection: `authenticationAndPermissionBoundaries`, real-invalid-password IT | Pass |
| 02 project management | Manager UI create/search/edit/delete, committed state; member denials: `projectLifecycleValidationSearchAndDeletion`, permission E2E and project ITs | Pass |
| 03 persistence | Immediate same-project task visibility and reload: creation E2E; complementary prior isolated Compose all-container recreation retained the task, Ficha 05 validation | Pass (complementary dated fixtures, not post-fix Docker rerun) |
| 04 validation | Empty/blank/invalid task/project inputs, no writes and UI drafts: validation E2E and ITs; Unicode title/name boundary units/controller tests now included | Partial: description/comment code-point inconsistency remains; raw high-precision numeric JSON handling not certified |
| 05 state/priority | Exact API/DB filters/counts and representative browser edits/reload: mutation IT and collaboration E2E | Pass |
| 06 assignees | Add/multiple/repeat/remove/clear, duplicate/ineligible rejection and atomicity: lifecycle IT and collaboration E2E | Pass |
| 07 comments | Content, session author/date, committed data and browser reload: comment IT and collaboration E2E | Pass |
| 08 authorization | Swapped identifiers 404, same-project prohibited changes 403, anonymous 401, unchanged committed data: 12-case IT and permission E2E | Pass (representative matrix, not exhaustive audit) |
| 09 deletion/errors | Task/project cancellation/confirmation, dependency cleanup, retained drafts/no false success on rejected submission: validation/deletion and project E2E, deletion IT | Pass |

Current functional evidence is **8 pass / 1 partial** at this bounded acceptance level, not full-input/browser/security certification. The later execution records below explain the actual assertions. Missing final clean-checkout/Git/remote-CI evidence is separate from functional tests. Old claims that standalone task deletion, draft retention or browser collaboration are wholly unverified are superseded.

### AI-use examples and refactoring decision (Ficha 03 Ex. 09)

These are concise records of observable requests/constraints and resulting verification, not invented private human sign-offs:

| Request objective | Recorded programmer/user decision or constraint | Verification |
| --- | --- | --- |
| Fix responsible-ID numeric validation | User authorized the bounded fix, not unrelated policy changes; reinspection corrected the AI review's claim that ordinary fractional IDs were unconditionally truncated: an existing guard already rejected 1.9. Validate parsed integral/range values exactly; retain deserialization limitation. | 14 focused cases passed; included in the 167-case backend regression; exact scope/evidence in quality-security review F01 |
| Resume Unicode task/project title validation | User required inspection before edits and no redo of completed work; resumption found the fix and 24-case focused run already complete and changed only the review record. | Saved log matched XML/text reports; subsequent full 252-distinct-test regression passed |
| Run complete post-fix regression | User required separate Maven profiles and preservation of pre-existing resources/uncommitted work; reused the safe isolated runner, no production changes. | Five commands exit 0, archived reports, pre/post resource-preservation inventory in regression record below |

**Refactoring decision recorded now, not a claim of past approval:** controllers combine HTTP/business/JDBC responsibilities (architecture design opportunity); extracting service/repository layers could improve separation but is not warranted in this documentation-only reconciliation and would expand the validated diff. No refactor applied. Any later extraction needs explicit approval and behavior-preserving regression. Human acceptance of this decision and final submission remains pending.

### Commands checked against configuration, not executed

`backend/pom.xml` declares Java 21, Surefire normal lifecycle, separate integration Failsafe and `e2e` source/profile/includes plus Playwright CLI install. README/AGENTS now document `test`, separate `-Pintegration verify` and `-Pe2e verify`, optional Node path and Chromium installation. `frontend/package.json` maps `test` to **all** `node --test src/*.test.js`, `build` to Vite; `npm ci` uses the existing lockfile. Compose build contexts/services/ports/health dependencies agree with README; Flyway V1/V2 and optional env-controlled seed match configuration/source. CI uses integration, separate E2E, npm tests/build, then Docker gated on three jobs with `contents: read`. Runtime images lack Maven/npm; host test commands must not be run inside final services. No executable configuration changed.

Documentation integrity checks in this audit: `git --no-pager diff --check` passed (exit 0); a read-only Python comparison confirmed `openapi.yaml` and `docs/matriz-openapi.yaml` byte-identical and re-read the saved regression summary (all five exits 0, backend 167 / PostgreSQL 37 / browser 5). This is report/configuration inspection, not test execution or complete OpenAPI conformance validation. No clean checkout or responsive multi-width browser check was performed in this audit. A read-only requirements worker corroborated the main gaps but its output was truncated by the local CLI's Windows cp1252 encoding error; no project failure or extra test run resulted, and canonical statements were inspected directly.

**Remaining mandatory gates:** actual GitHub Actions green plus controlled-failure/recovery evidence (Ficha 05 Ex. 04/06); secret-reviewed complete GitHub snapshot/identified commit and clean-checkout execution (Ficha 03 Ex. 09, Ficha 04 deliverables, Ficha 05 delivery); human acceptance/disposition of remaining validation limitations and candidate identity. Final secret-free verification was not performed. Production release/public deployment and scanners/registry publication are not mandatory. Current branch is `revisao-final-fichas-03-04-05`, not an identified final delivered commit. See [final checklist](final-review.md).

## Historical full regression after responsible-ID and title/name fixes — Matriz v1.3 (2026-11-16)

**PASS: all five requested commands, first attempt, after the responsible-ID and Unicode title/name fixes.** Executed sequentially **2026-11-16T07:22:32Z–07:27:48Z** against the existing uncommitted working tree. No test or production defects observed in this execution; no source/test fixes, retries, dependency changes or installations. This supersedes the earlier regression for execution after these fixes, not for release/security approval.

### Exact commands, counts and exit codes

Reused the existing ignored runner, launched from the repository root as `python backend/target/final-local-regression.py` (**exit 0**). It invoked installed Maven **3.9.5** (`mvn.cmd`), OpenJDK **21.0.2** selected through `JAVA_HOME`, and the installed Node.js `npm.cmd`. These are neutral tool prerequisites; personal installation paths are omitted. Existing Node dependencies and Chromium were reused. The runner filters inherited `SPRING_`, `DB_`, `DEMO_`, `SERVER_`, `SESSION_COOKIE_` and `COMPOSE_` environment variables; no `.env` or secret files were read. Profiles remained **separate**, with no selection/skip flags. Exact executable argument arrays, working directories, UTC times and process return codes are retained in the summary below.

| Directory / exact command (installed executable above) | Executed result | Failures / errors / skipped | Exit | Tool duration; process wall time | Process finished UTC |
| --- | --- | --- | --- | --- | --- |
| `backend/`: `mvn -B -ntp test` | **167 Surefire** tests; BUILD SUCCESS | **0 / 0 / 0** | **0** | Maven **42.704 s**; wall **51.249 s** | **07:23:24.876436Z** |
| `backend/`: `mvn -B -ntp -Pintegration verify` | **167 Surefire + 37 PostgreSQL Failsafe = 204**; BUILD SUCCESS | **0 / 0 / 0**, both layers | **0** | Maven **01:30 min**; wall **94.950 s**; IT class **45.160 s** | **07:25:00.084792Z** |
| `backend/`: `mvn -B -ntp -Pe2e "-De2e.node=C:/Program Files/nodejs/node.exe" verify` | **167 Surefire + all 5 Java Playwright Failsafe = 172**; BUILD SUCCESS | **0 / 0 / 0**, both layers | **0** | Maven **02:37 min**; wall **161.667 s**; E2E class **109.438 s** | **07:27:42.022510Z** |
| `frontend/`: `npm test` | **43 passed**, 0 failed/cancelled/skipped/todo | **0 failed; 0 skipped** | **0** | Node runner **329.0067 ms**; wall **3.103 s** | **07:27:45.512018Z** |
| `frontend/`: `npm run build` | Vite **8.3.8**, **18 modules**, production build successful | Not a test suite | **0** | Vite **648 ms**; wall **2.356 s** | **07:27:47.870081Z** |

Surefire per-class counts, identical across all three Maven commands: AbsoluteSessionExpiryFilterTest **4**, AuthControllerTest **5**, AuthenticationFlowTest **2**, SecurityIntegrationTest **39**, HealthControllerTest **2**, ProjectControllerTest **35**, TaskControllerTest **60**, TaskTitleRuleTest **20** = **167**. The increase from the previous 141 is **14 responsible-ID cases + 8 task Unicode cases + 4 project Unicode cases**. Standard `verify` repeats Surefire: these repetitions are not additional unique coverage. No flakes reported in the XML.

All current Java Playwright methods ran under the separate E2E profile (`TaskCreationE2E`):

| Method | Testcase seconds |
| --- | --- |
| `taskValidationDraftsBusyAndDeletion` | **37.982** |
| `loginOpenProjectCreateTaskAndRemainInItsTaskList` | **9.308** |
| `projectLifecycleValidationSearchAndDeletion` | **15.111** |
| `authenticationAndPermissionBoundaries` | **13.546** |
| `taskCollaborationFiltersAssignmentsCommentsAndReload` | **14.206** |

### Evidence and preservation

Evidence root: **`backend/target/final-local-regression-20261116T072232Z/`** (ignored local output). Contains `summary.json`, `baseline.json`, `backend-unit.log`, `backend-integration.log`, `backend-e2e.log`, `frontend-test.log`, `frontend-build.log` and per-command copied Surefire/Failsafe reports. Each command's reports were copied before the next command. Failsafe directories may retain older XML for the other profile; counts above use only **TaskPostgresIT** for integration and **TaskCreationE2E** for E2E, corroborated by the fresh logs. Do not publish unsanitized logs/session/config values.

Rechecked and reused the existing safe isolation: PostgreSQL **17.11-bookworm** containers with generated credentials and random ports; explicit container-only datasource **and Flyway** bindings; demo seeding disabled; Flyway clean disabled. E2E uses private loopback Spring Boot/Vite and headless Chromium. Tests do not use existing Compose databases, mounted volumes or application credentials. Automatic Testcontainers lifecycle cleanup only; no Compose/prune/manual stop/remove commands or shared database operations.

Runner inventories (`docker ps -aq --no-trunc`, `docker volume ls -q`, `docker network ls -q --no-trunc`) confirmed **all 6 baseline container IDs, 25 volume names and 5 network IDs preserved**. The immediate post-run summary saw **7** containers while asynchronous cleanup was settling. A subsequent read-only `docker ps -a --no-trunc --format '{{.ID}} {{.Image}} {{.Status}} {{.Names}}'`, plus the same volume/network listings (**exit 0**), showed exactly the original **6** containers, all still running, **25** volumes and **5** networks; no manual cleanup was performed.

Only this document was edited in this increment, alongside ignored generated reports/logs/backend build/frontend production output. Existing uncommitted source/configuration/document changes and secrets were retained; no staging, commit or push. `git diff --check` passed (exit **0**) after documentation was recorded. Existing Mockito agent/JVM warnings were non-fatal. Passing tests establish their asserted fixtures, not absence of all production defects, exhaustive Unicode/security behavior, HTTPS/proxy correctness, remote CI or release approval. Remaining known security-review limitations are unchanged.

## Final local regression — Matriz v1.3 (2026-11-15)

**PASS: all requested local regression commands, no retries or fixes.** Executed sequentially **2026-11-15T22:54:47Z–22:59:59Z** against the current working tree. This is working-tree evidence, not an identified committed/released snapshot or remote CI result. Previous records below remain historical; their smaller suite counts do not describe this regression.

### Exact commands and outcomes

Commands below ran in the indicated directories. Windows used installed Maven **3.9.5** `mvn.cmd` and OpenJDK **21.0.2** via JAVA_HOME, plus `C:/Program Files/nodejs/npm.cmd` and existing frontend dependencies. A local ignored Python subprocess helper (`backend/target/final-local-regression.py`, launcher `python backend/target/final-local-regression.py`, **exit 0**) captured UTF-8 stdout/stderr and return codes. Inherited Spring/DB/demo/server/session/Compose environment variables were filtered out; no `.env` or credentials were read. Integration and E2E profiles were **never combined**; no test selection filters or skip flags were used.

| Directory / exact command | Executed tests / result | Failures / errors / skipped | Exit | Maven/tool duration; process wall time | Finished UTC |
| --- | --- | --- | --- | --- | --- |
| `backend/`: `mvn -B -ntp test` | **141** Surefire unit/controller/security cases; BUILD SUCCESS | **0 / 0 / 0** | **0** | Maven **26.701 s**; wall **31.158 s** | **22:55:19Z** |
| `backend/`: `mvn -B -ntp -Pintegration verify` | **141 Surefire + 37 PostgreSQL Failsafe = 178**; BUILD SUCCESS | **0 / 0 / 0** in both layers | **0** | Maven **01:21 min**; wall **85.343 s**; `TaskPostgresIT` **46.119 s** | **22:56:45Z** |
| `backend/`: `mvn -B -ntp -Pe2e "-De2e.node=C:/Program Files/nodejs/node.exe" verify` | **141 Surefire + all 5 Java Playwright Failsafe = 146**; BUILD SUCCESS | **0 / 0 / 0** in both layers | **0** | Maven **02:57 min**; wall **181.842 s**; `TaskCreationE2E` **135.681 s** | **22:59:47Z** |
| `frontend/`: `npm test` | **43 passed**, 0 failed/cancelled/skipped/todo | **0 failed; 0 skipped** | **0** | Node runner **421.8698 ms**; wall **8.374 s** | **22:59:56Z** |
| `frontend/`: `npm run build` | Vite **8.3.8**, **18 modules**, production build successful | Not a test suite | **0** | Vite **750 ms**; wall **3.106 s** | **22:59:59Z** |

The 141 Surefire cases comprise AbsoluteSessionExpiryFilterTest **4**, AuthControllerTest **5**, AuthenticationFlowTest **2**, SecurityIntegrationTest **39**, HealthControllerTest **2**, ProjectControllerTest **31**, TaskControllerTest **46** and TaskTitleRuleTest **12**. They executed three times because standard `verify` includes Surefire; do not count these repetitions as new unique coverage. PostgreSQL Failsafe executed **37** cases, not the older historical 15. No flakes were reported in the captured XML.

All current Java Playwright tests executed in the same E2E class/container lifecycle:

| Method | Testcase seconds |
| --- | --- |
| `taskValidationDraftsBusyAndDeletion` | **48.577** |
| `loginOpenProjectCreateTaskAndRemainInItsTaskList` | **11.382** |
| `projectLifecycleValidationSearchAndDeletion` | **18.895** |
| `authenticationAndPermissionBoundaries` | **14.291** |
| `taskCollaborationFiltersAssignmentsCommentsAndReload` | **23.002** |

### Durable local evidence and preservation

Evidence root: **`backend/target/final-local-regression-20261115T225447Z/`** (already ignored). It contains `summary.json` with exact executable paths/arguments, working directories, exit codes, finish times, measured wall durations and per-class XML counts; `backend-unit.log`, `backend-integration.log`, `backend-e2e.log`, `frontend-test.log`, `frontend-build.log`; and per-command copies of Surefire/Failsafe reports under `backend-unit/`, `backend-integration/`, `backend-e2e/`. Reports were copied immediately after each command so later runs cannot overwrite this regression's evidence. Standard `target/failsafe-reports` can contain older XML from another profile: the regression summary uses `TaskPostgresIT` for integration and `TaskCreationE2E` for E2E, confirmed by each fresh command log. Local output is not a portable published artifact; do not publish unsanitized traces/config/session values.

Reused existing isolated Testcontainers PostgreSQL **17.11**, generated credentials/random mapped ports, explicit container-only datasource **and Flyway** bindings, demo seed disabled and Flyway clean disabled. Browser tests used private loopback Spring Boot/Vite and headless Chromium; not an existing Compose stack. Only Testcontainers-owned test resources/processes were automatically disposed. No Compose operations, Docker prune, shared database queries/migrations, external volume mounts, secret/backup access or browser installation were performed.

Pre/post read-only inventories (`docker ps -aq --no-trunc`, `docker volume ls -q`, `docker network ls -q --no-trunc`) confirmed every baseline resource remained: **6 existing container IDs, 25 volume names and 5 network IDs preserved**. No existing containers/volumes/networks/databases were stopped, recreated, deleted or used by tests. Existing uncommitted work retained. Only this document and ignored generated test/build/log/report/helper output changed in this increment; **no test defects or production defects observed, no source fixes**, staging, commits or push. `git diff --check` passed after recording this section.

**Limitations:** passing tests establish their asserted fixtures, not absence of all defects, exhaustive authorization/security review, production nginx/HTTPS/browser compatibility, remote Actions success, restart durability or release approval. Frontend Node tests are not rendered-browser tests; the Java Playwright suite supplies the recorded browser coverage. The independent runtime Compose healthcheck gate evidence remains in `docs/ficha05-validation.md` and was not rerun here.

## Ficha 04 U02 — focused isolated title-rule execution (2026-11-15)

Closed the U02 business-rule unit gap with `backend/src/test/java/pt/matriz/tasks/TaskTitleRuleTest.java:21`. Reused `TaskControllerTest`'s JDBC user/membership stubbing pattern and existing JUnit 5/Mockito dependencies. Direct public `TaskController.create` invocation, **no Spring context/MockMvc/HTTP/real database/Docker**, no reflection into private validation, copied rule implementation or production extraction/change. Five accepted cases verify 201/Location, returned normalized title and exact INSERT arguments: ordinary title, 3/150 characters and padded 3/150. Seven rejected cases verify title-specific 422 and only two access-read interactions (no INSERT/update/dependent writes): 2/151, padded 2/151, empty, whitespace-only and explicit null.

**Executed once from `backend/`:**

```text
mvn -B -ntp -Dtest=TaskTitleRuleTest test
```

Installed Maven 3.9.5 `mvn.cmd`, OpenJDK 21.0.2 via JAVA_HOME; Python subprocess captured UTF-8 stdout/stderr. **BUILD SUCCESS, captured exit 0**, finished **2026-11-15T22:36:16Z**. **12 tests, 0 failures/errors/skips/flakes**; Maven **19.194 s**, Surefire class **4.500 s**. Log: `backend/target/u02-title-rule-unit.log`; report: `backend/target/surefire-reports/TEST-pt.matriz.tasks.TaskTitleRuleTest.xml:2`. Only this class executed; test-source compilation did not execute other tests. Existing Mockito agent/deprecation/unchecked warnings were non-fatal. No integration/E2E profiles, frontend tests, full suites, production builds or security scanners ran. Ignored reports are local evidence, not portable delivery artifacts; historical suite totals were not rerun.

Updated U02 mapping in `docs/test-plan.md` and checked its gate in `docs/final-review.md`. Remaining security-review decisions are explicitly inventoried in `docs/quality-security-review.md`: candidate/review scope, targeted evidence gaps, deployment HTTPS/proxy/session verification, local limiter scope, conditional static tooling and final secret/publication approval. Approved D3/D4 policies are not reopened. This inventory is not a completed security review or finding-free certification; no new confirmed production defect established or fixed.

Only the new test and these four directly relevant documents changed in this increment. No shared data, Docker resources, credentials, backups, existing uncommitted edits, staging, commits or push touched. U02 is an isolated controller-unit test of the embedded title rule, not a standalone validator, all-input validation proof, persistence/security certification or release approval.

## Ficha 03 — acceptance evidence audit (2026-11-15)

This section records the current implementation/evidence assessment and supersedes P1-only status claims **for this acceptance scope**. Historical records below are unchanged. Sources: Ficha 03 Ex. 08 (nine scenarios), `docs/requirements.md`, `openapi.yaml`, `docs/final-review.md` and the specific source/tests below. The original read-only audit ran no tests, browser flows, builds, database queries or Docker operations; subsequent focused integration executions are recorded separately below.

**Classification:** verified = the scenario's outcome has direct recorded execution evidence, within the stated fixture; partially verified = only some outcomes/layers have evidence; not verified = implementation exists but no directly relevant executed positive outcome was found. Passing suites do not establish unasserted behavior. These are not nine new test results and do not certify every input, role or environment.

Execution baseline reused: latest integration profile **129 + 15, 0 failures/errors/skips** (`backend/target/blocker-fix-integration.log`); E2E profile **129 + 1, 0 failures/errors/skips** (`backend/target/blocker-fix-e2e.log`); frontend **43 passed** (CI local record in `docs/ficha05-validation.md`). Controller tests mock JDBC; frontend Node tests mock requests or inspect source, not a rendered browser. Integration tests use real PostgreSQL; the browser case uses actual HTTP, React and disposable PostgreSQL, with project/account fixtures inserted directly rather than created through the UI. Ignored local reports are not a portable published delivery artifact.

### Canonical Ficha 03 Ex. 08 — acceptance status

The canonical numbering and Portuguese scenario names below follow `docs/aulas/ficha03/enunciado.md:182–199`. The earlier topic-based audit incorrectly presented its own numbering as Ex. 08; its complete evidence is retained in the non-canonical topic index below. That index and its former summary must not be used as canonical scenario completion counts. No new execution is implied by this correction.

| # / canonical scenario | Exact acceptance outcome (English translation) | Coverage and remaining gap |
| --- | --- | --- |
| **01 Acesso à aplicação** | Valid credentials allow entry; invalid credentials and unauthenticated requests are handled. | **Partially verified.** Real successful login in PostgreSQL/browser fixtures; `AuthControllerTest.rejectsInvalidLogin` mocks authentication; `SecurityIntegrationTest.anonymousWritesReturn401WithOrWithoutCsrf` and anonymous-read tests verify rejection. The new `TaskPostgresIT.realInvalidPasswordDoesNotAuthenticateOrChangeAccount` passed against the real authentication provider: wrong password 401, same-session protected requests 401, unchanged password hash/project state, then correct-password login/profile 200. The focused P0 browser test below now verifies invalid-login feedback, unauthenticated rejection and successful recovery after re-entering both credentials. This closes the recorded browser failure-feedback gap within that fixture, not session-expiry/logout/HTTPS coverage. |
| **02 Gestão de projetos** | Create, search, edit and delete work according to permissions. | **Partially verified — focused API/DB project-management paths verified (2026-11-15).** Five new PostgreSQL cases prove authenticated creation with omitted/null/text descriptions, committed creator/sole-manager membership and list/detail visibility; actual case-insensitive/accent-sensitive/literal-wildcard search results; committed authorized edits and project deletion with dependent cleanup and unrelated-project preservation. Existing authorization evidence is reused, not rerun. The focused P1 browser execution below now verifies manager UI create/search/edit/delete, reload, validation feedback and confirmations within a representative fixture; real same-project member PATCH/DELETE 403 with unchanged committed data is now verified by the focused P0 browser-context execution below. See focused scenario 02 execution below. |
| **03 Persistência de tarefas** | The task appears without reloading and remains after restarting the backend. | **Verified within the recorded task-creation fixtures.** `TaskCreationE2E.loginOpenProjectCreateTaskAndRemainInItsTaskList` proves immediate visibility and page reload; separate isolated restart evidence in `docs/ficha05-validation.md:103–117` proves unchanged task after backend/container recreation. These are complementary executions, not one continuous browser/restart test or coverage of every mutation. |
| **04 Validação de dados** | Required fields that are empty or whitespace-only, and invalid values, are rejected. | **Partially verified.** `TaskPostgresIT.invalidTitlesAreRejectedWithoutWrites` covers missing/whitespace/short/long titles and DB non-mutation; `trimmedTitleBoundariesAreAccepted` covers valid limits. Mocked project PATCH, unknown-field and dashboard-priority tests add coverage. The focused P1 project browser execution below additionally verifies empty required-name blocking and whitespace-only name rejection with retained drafts/no writes. Empty/null task title, description/comment limits, invalid mutation/filter enums and assignment inputs still need explicit assertions. |
| **05 Estado e prioridade** | Editing updates the task, filters and counts coherently. | **Partially verified — focused API/DB paths verified (2026-11-15).** `TaskPostgresIT.authorizedTaskMutationsCommitAndRefreshExactFiltersAndCounters` passed for manager and assigned member: authorized status/priority changes, independent committed persistence and subsequent detail, all 16 filter combinations with exact IDs/totals including empty results, and project/dashboard counts before and after each mutation. Counts ignore status and respect priority; dashboards retain accessible empty projects and exclude inaccessible projects. The focused P1 task-collaboration browser execution below now verifies manager/assigned-member edits, representative individual/combined/empty filters, project/dashboard counts and reload within its fixture; exhaustive browser combinations remain unverified. See focused scenario 05 execution below. |
| **06 Responsáveis** | Association and removal work; the same association is not duplicated. | **Partially verified — focused API/DB assignment lifecycle verified (2026-11-15).** `TaskPostgresIT.managerAssignmentLifecycleCommitsAndRejectsInvalidSetsWithoutPartialWrites` passed: manager add/multiple/reordered-repeat/remove/replace/clear/repeat-clear, exact committed sets and subsequent detail, duplicate/ineligible IDs rejected with 422 without partial replacement, and unchanged unrelated data. The focused P1 task-collaboration browser execution below now verifies checkbox add/multiple/repeat/remove/clear, exact committed sets, visible names and reload within its fixture. See focused scenario 06 evidence below. |
| **07 Comentários** | Content, authenticated author and date are stored and visible. | **Partially verified — API/DB happy path verified.** The focused comment test below proves committed content/author/UTC date and subsequent API visibility; the focused P1 task-collaboration browser execution below now verifies member submission, rendered content/authenticated author/localized date, committed persistence and reload within its single-comment fixture. |
| **08 Autorização na API** | Another user cannot access or change resources without permission, even by changing request identifiers. | **Partially verified.** Real cross-project task creation/read/edit and shared-project member edit/status/assignment denials preserve data; mocked project and anonymous/expired-session matrices add coverage. The new 12-case `TaskPostgresIT.crossProjectIdentifierSwappingIsHiddenAndCannotChangeCommittedState` passed for project GET/PATCH/DELETE, eligible users/task listing, task DELETE/status/priority/responsibles, comment GET/POST and history GET: 404, unchanged committed project/dependent snapshots and authorized controls. Full role/endpoint/list/dashboard coverage is not established; see focused execution below. |
| **09 Eliminação e erros** | Deletion respects dependencies; API errors produce neither success messages nor loss of form data. | **Partially verified.** Scenario 02's new project-delete integration test proves committed membership/task/assignment/comment/history cleanup and preservation of an unrelated populated project. Standalone task-delete cleanup remains unverified. Node tests verify error propagation/no navigation, not rendered feedback or retained drafts; the focused P1 project browser execution below now verifies project deletion confirmation/cancellation and whitespace-rejection draft retention/no false creation success. Standalone task confirmation/cancellation and broader API-failure feedback/draft retention remain missing. |

**Canonical summary: 1 verified within explicit fixtures (03), 8 partially verified.** Missing evidence is not proof of a defect. Browser loading/empty/busy/error requirements also retain the limitations noted below.

### Retained implementation-topic evidence index — not canonical scenario numbering

Paths are relative to the repository; backend test names are exact method names. The following rows preserve the earlier detailed evidence and gaps; their topics are not the canonical Ex. 08 cases. **Historical gap descriptions in this retained index are superseded where explicitly closed by the focused executions below:** scenario 02 now has API/DB creation, matching search, project-edit persistence and project-deletion cleanup evidence; scenario 05 now has authorized status/priority persistence, real filter results and project/dashboard counter evidence; scenario 06 now has manager assignment lifecycle, exact committed sets, idempotence and atomic duplicate/ineligible rejection evidence. Browser gaps and standalone task-deletion cleanup remain. Consult the canonical table for current status.

| Implementation topic | Classification | Specific implementation and existing evidence | Exact gap / next targeted check |
| --- | --- | --- | --- |
| **Create project**, required name, optional description; persist/list/creator | **Partially verified** | `frontend/src/main.jsx:89–95,115–116` creates then opens project; `backend/src/main/java/pt/matriz/projects/ProjectController.java:83–104` takes creator from session and inserts manager membership transactionally. `ProjectControllerTest.createsProjectWithSessionCreatorAndMembership` asserts 201, normalized name, null description, creator and membership write; `listsProjectsWithExactEnvelopeAndSchema` asserts mocked list. | No executed real-DB/browser project creation→list→reload/creator-membership assertion. Browser E2E seeds its project directly. On an approved isolated fixture create projects with/without description via UI; assert list/detail after reload, session creator, manager membership and no other membership; also check required-name rejection. |
| **Search projects by partial name**, case-insensitive | **Partially verified** | `main.jsx:45,89–91`; `ProjectController.java:55–80` trims/escapes literal substring for `ILIKE`. `ProjectControllerTest.searchUsesTrimmedLiteralSubstring`, `emptySearchDoesNotFilter`, `searchLimitAppliesAfterTrimAndBeforeEscaping`, `searchLimitCountsUnicodeCharactersRatherThanUtf16Units` check bound patterns/limits, not PostgreSQL matching. | Seed mixed-case/accents/literal `%` and `_` names in isolated DB; search via UI and verify matching/nonmatching records, case-insensitivity, accent distinction, empty search and persistence. Do not claim SQL mocked assertions prove database collation/UI results. |
| **Create task inside project**, optional description, new list item, TODO/MEDIUM | **Verified (tested creation paths)** | `main.jsx:47–56,99–101,117–118`, `frontend/src/task-flows.js:8–16`; `TaskController.java:109–129`. `TaskPostgresIT.managerCreatesRetrievesAndCommitsTaskWithEligibleAssignment` verifies trimmed title/description, project, defaults, timestamp and committed row; `memberCreatesWithoutAssignments` covers omitted/null description. `TaskCreationE2E.loginOpenProjectCreateTaskAndRemainInItsTaskList` (`backend/src/e2e/java/pt/matriz/tasks/TaskCreationE2E.java:109–131`) asserts same project, cleared hiding filters, single task card, defaults and reload/reopen. `task-flows.test.js:17–24,48–55` protects navigation wiring. | No mandatory rerun for this demonstrated happy path. Browser description entry/negative forms and other roles are not covered by this single browser case; API negative cases exist. This does not imply all task acceptance scenarios pass. |
| **Change status and priority**, list/detail/counters updated | **Partially verified** | `main.jsx:59,98,104–105`; `TaskController.java:150–166,230–239`. `TaskControllerTest.taskListUsesOnlySuppliedFiltersAndBoundValues`, `dashboardFiltersAllCountsByPriorityWithoutRemovingProjects`, `historyMatchesContract` test mocked filters/counts/history; `TaskPostgresIT.unassignedMemberCanReadButCannotMutateSharedTask` proves forbidden mutations leave DB unchanged. | No positive real-DB/browser mutation evidence for authorized status/priority changes or counter refresh. As manager and assigned member, change each, assert detail/project/dashboard counts against fixture totals and after reload. Defaults/history fixtures are not evidence of a successful mutation. |
| **Filter tasks by state and priority**, individual/combined/counters | **Partially verified** | `main.jsx:55–56,96–100`; `TaskController.java:79–106`. `TaskControllerTest.taskListUsesOnlySuppliedFiltersAndBoundValues` checks SQL arguments and mocked envelopes for combinations; dashboard tests check priority count query shape. Browser E2E selects DONE/HIGH then verifies clearing after creation, not filter result correctness. | Seed distinct states/priorities; assert individual/combined/empty matches in UI and PostgreSQL-backed API. Counts must ignore status filter but respect priority, as defined in `docs/requirements.md:16–17`; do not assert counts equal displayed rows when status is selected. |
| **Associate/remove responsible users**, multiple, no duplicates | **Partially verified** | `main.jsx:106–107,119`; `TaskController.java:177–191`, eligibility helper `:65–76`; V1 assignment primary key prevents duplicate pairs. `frontend/src/projects.test.js:36–40` checks nested user mapping; `TaskPostgresIT.managerCreatesRetrievesAndCommitsTaskWithEligibleAssignment` verifies **initial** eligible assignment. `memberCannotSelfAssignDuringCreation` and `unassignedMemberCanReadButCannotMutateSharedTask` prove relevant denials. | No positive executed manager PUT replacement/add/remove/multiple/idempotence flow or duplicate-submission rejection. On isolated fixtures assert all names shown, exact assignment set after reload, removal and duplicate policy (backend rejects duplicate IDs with validation, rather than storing duplicates). Initial assignment does not verify later management. |
| **Add comment**, author/date and persistence | **Partially verified — API/DB happy path verified (2026-11-15)** | `main.jsx:108–109,120` posts and displays author ID/date; `TaskController.java:193–210` uses session author/DB timestamp. New `TaskPostgresIT.memberCreatesCommentWithAuthorTimestampAndCommittedPersistence` passed: authenticated member POST 201/Location, normalized content, exact task/member author, UTC timestamp within request interval, matching committed row through an independent JDBC connection, then manager GET 200 returns exactly the same comment. See focused execution below. | Browser comment submission/rendering and display after reload remain unverified; no new assertions for multiple-author ordering, empty-comment rejection or draft retention. UI identifies author as `Autor #id`; a name is not explicitly required. API/DB success does not prove browser acceptance. |
| **Edit/delete task or project**, persist/remove/no orphan children | **Partially verified** | `main.jsx:94–95,103,112` includes confirms and refresh; `ProjectController.java:106–138`; `TaskController.java:131–148`; V1 FKs cascade project/task children. `ProjectControllerTest.nameOnlyPatchPreservesDescriptionAndOriginalCreator`, `descriptionOnlyPatchPreservesName`, `explicitNullClearsDescription`, `invalidPatchDoesNotWrite`, `memberCannotPatchOrDelete` cover mocked edits/denials. `frontend/src/task-flows.test.js:27–45` verifies edit-detail navigation/error propagation; no real successful deletion proof. | In isolated synthetic data, verify successful manager edits/reload, confirm cancel performs no request, confirm deletes remove list/detail and only owned synthetic children; assert no orphan task/comment/history/assignment rows. Verify member denial. FK declarations do not prove full UI/deletion acceptance; no existing project data may be used. |
| **Reload after mutation**, persistence | **Verified for task creation only** | `TaskCreationE2E...:125–131` checks committed project/title row, reloads page, reopens same project and sees one task. `TaskPostgresIT.managerCreatesRetrievesAndCommitsTaskWithEligibleAssignment` uses a fresh JDBC connection outside request transactions. Independent isolated Compose evidence confirms unchanged task after all three containers recreated (`docs/ficha05-validation.md`). | Historical topic assessment only: reload after one mutation is demonstrated, but this is not canonical case 09 (deletion/errors); canonical case 03 additionally requires backend restart, evidenced separately above. Project edits, assignments, comments and deletions remain unverified for their own scenarios; do not generalize this result to every resource or restart/HTTPS/browser environment. |

**Superseded topic-index summary (not canonical acceptance counts): 2 verified topics (task creation and reload, with explicit scope), 7 partially verified topics, 0 wholly unverified topics.** No failure is inferred merely from missing evidence. Additional UI requirements from Ex. 07 (loading, emptiness, feedback, error drafts and busy guards) have source/Node evidence but not complete rendered-browser acceptance; include them in targeted checks above rather than asserting completion from source.

### P1 — focused task-collaboration browser execution (2026-11-15)

Added only `TaskCreationE2E.taskCollaborationFiltersAssignmentsCommentsAndReload` and two focused task-open/mutation helpers. Reused private Vite/Chromium lifecycle, UI login/project navigation, isolated PostgreSQL 17.11/Flyway and independent manager/member browser contexts. Synthetic fixture: one shared project, manager/member, target TODO/MEDIUM, DONE/HIGH and TODO/MEDIUM sentinels. No production/configuration changes or existing data/resources touched.

Executed from `backend`, once, without retries:

```text
mvn -B -ntp -Pe2e "-Dit.test=TaskCreationE2E#taskCollaborationFiltersAssignmentsCommentsAndReload" test-compile failsafe:integration-test failsafe:verify
```

**BUILD SUCCESS, captured exit 0**, finished **2026-11-15T18:38:48Z**, Maven **01:23 min**. Log: `backend/target/p1-task-collaboration-e2e.log`. Failsafe XML `backend/target/failsafe-reports/TEST-pt.matriz.tasks.TaskCreationE2E.xml:2,73`: **1 test, 0 failures/errors/skips/flakes**, suite **66.779 s**, sole method `taskCollaborationFiltersAssignmentsCommentsAndReload` **50.362 s**. Only this method ran; no Surefire/full suites or prior E2E methods. The XML overwrites earlier focused reports; prior results remain historical. Artifact namespace: `backend/target/e2e-artifacts/task-collaboration/` (Vite log; failure screenshots/traces only on failure). Ignored local evidence is not portable delivery proof.

Observed: manager status TODO→DONE and priority MEDIUM→HIGH; later assigned member DONE→IN_PROGRESS and HIGH→LOW, all UI requests 200 with confirmed refresh and exact committed fields. Manager checkbox flow adds both users, repeats without duplicates, removes one, clears all, then assigns member; every step asserts exact committed IDs and displayed names. Reload confirms final checkbox selection/status/priority. Member posts one comment (201): exact committed content/member author, DB timestamp inside request interval, rendered author ID/date matching browser-local `pt-PT` formatting, cleared successful draft; reload/reopen preserves comment, assignment and task changes.

Five representative filter states assert exact ordered cards and counts: unfiltered, status only, combined empty, priority only, combined match. Status filtering does not narrow counts; priority does. Empty state is visible. Dashboard after reload shows TODO=1, IN_PROGRESS=1, DONE=1 both globally and for the project. Awaited responses, detail headings and enabled controls replace arbitrary sleeps.

Scope/gaps: scenarios 05/06/07 browser happy paths now have direct fixture evidence, superseding earlier missing-browser claims for these outcomes. Conservative canonical partial labels remain: not exhaustive state/priority combinations, roles, multi-author ordering, negative comment/assignment inputs, error-draft/busy/network behavior, standalone task deletion or backend restart. Existing exhaustive API/DB matrices were neither duplicated nor rerun. Only E2E source and this document were edited; no commits/pushes.

### P1 — final focused validation, drafts, busy state and task deletion (2026-11-15)

Added only `TaskCreationE2E.taskValidationDraftsBusyAndDeletion` (`backend/src/e2e/java/pt/matriz/tasks/TaskCreationE2E.java:467`) and this evidence section. Reused existing Playwright/browser/login/project helpers, dependent seeding and seven-table committed snapshots. Real Spring Boot, private Vite proxy and isolated PostgreSQL 17.11 Testcontainers use ephemeral ports and generated credentials; demo seed and Flyway clean remain disabled. **No production defect observed; no production code/configuration/dependency/migration changes or fixes.**

**Executed once, backend directory; only the new method:**

```text
mvn -B -ntp -Pe2e "-Dit.test=TaskCreationE2E#taskValidationDraftsBusyAndDeletion" "-De2e.node=C:/Program Files/nodejs/node.exe" test-compile failsafe:integration-test failsafe:verify
```

Windows execution used installed Maven 3.9.5 `mvn.cmd`, JAVA_HOME pointing to the existing OpenJDK 21.0.2 and Node on PATH; a Python subprocess captured UTF-8 output without printing secrets. **BUILD SUCCESS, captured exit 0**, finished **2026-11-15T22:29:28Z**, Maven **01:55 min**. Log: `backend/target/p1-task-validation-deletion-e2e.log`. Failsafe XML `backend/target/failsafe-reports/TEST-pt.matriz.tasks.TaskCreationE2E.xml:2,74`: **1 test, 0 failures/errors/skips/flakes**, suite **68.449 s**, sole testcase `taskValidationDraftsBusyAndDeletion` **48.9 s**. No Surefire/unit suites, previous browser methods, exhaustive PostgreSQL integration suites, production builds or Compose commands ran. The XML replaces the previous focused report; earlier results remain historical, not rerun.

**Observed assertions:**

- Empty required task title is blocked by native `valueMissing`, sends no mutation and leaves the full committed snapshot unchanged. Three spaces reach the real API and return **422** with visible title-specific alert, retained title/description drafts, no success status and unchanged DB. Invalid edit similarly returns real **422**, retains both edit drafts and leaves the snapshot unchanged. Corrected edit returns **200**, clears the alert, shows success and persists the description.
- The creation POST is held by a test-local route and then forwarded unchanged to the real backend; no synthetic success response. Two synchronous `requestSubmit()` calls plus a further submit event while pending produce exactly **one POST**. Save/title/description/logout controls are disabled, the visible status is `A processar…`, and the committed DB remains unchanged until release. The real response is **201**; exactly one matching row exists, success is visible, controls recover and the prior alert is gone.
- A **deliberately induced 503** on comment POST verifies visible service-error text, retained comment draft, no success status, recovered submit control and unchanged committed snapshot. This is a frontend failure-path assertion, not evidence of an actual backend outage/defect. Removing the route and retrying reaches the real backend (**201**), clears draft/error and stores exactly one comment. Real UI assignment (**200**) supplies an assignment and history entry; their existence and the comment are asserted before deletion.
- Native task-deletion confirmation text/type are checked. Cancel sends no DELETE and preserves the entire snapshot. Confirm returns **204**, sends exactly one DELETE, displays success and returns to the project with only its sentinel task. Exact equality of all seven domain tables to the pre-creation baseline proves target task/assignment/comment/history cleanup and preservation of users, memberships, both projects and populated same-project/unrelated-project sentinels. Subsequent task GET returns **404**.
- Reload shows exactly **2 accessible projects**, global **TODO=2 / IN_PROGRESS=0 / DONE=0**. Reopening the target project shows exactly its sentinel and **TODO=1 / IN_PROGRESS=0 / DONE=0**, with the same committed baseline. Synchronization uses responses, route conditions and Playwright assertions; no arbitrary sleeps.

**Preservation:** pre/post read-only Docker inventories show the same six pre-existing running container IDs/names and the same 25 volume names. No existing Docker resource was stopped, recreated or removed; only automatically managed test resources were disposed. No shared database/`.env`/backup access, resets, truncation, staging, commits or push. Existing uncommitted work retained. `git diff --check` passed. Artifact namespace: `backend/target/e2e-artifacts/task-validation-deletion/` (Vite log; screenshots/traces only on failure). Ignored local reports are not portable published delivery evidence; do not publish unsanitized traces.

**Scope:** closes the final requested representative P1 browser gaps for invalid task create/edit submissions, visible errors, retained drafts, in-flight/double-submit prevention and standalone task deletion/reload. Supersedes earlier missing-evidence claims only for these exact outcomes; canonical partial labels and historical evidence remain conservative. Not exhaustive field/length/role validation, every form's busy state, negative comment validation, network disconnection, production nginx/HTTPS, browser compatibility, restart durability or release approval. Earlier tests were reused as source patterns, not rerun.

### P1 — focused project lifecycle browser execution (2026-11-15)

Added `TaskCreationE2E.projectLifecycleValidationSearchAndDeletion` and a synthetic dependent-fixture helper in the existing E2E file. Reused `withBrowser`, real UI login, disposable PostgreSQL 17.11/Flyway, private random-port Vite/backend and committed domain snapshots. Tightened the shared project-opening helper to await the level-2 detail heading (not a same-name list-card heading) and enabled logout control (busy operation completed). No production, migration, dependency, Docker configuration or existing database/volume/secret changes.

**Executed command, backend directory (only this new method):**

```text
mvn -B -ntp -Pe2e "-Dit.test=TaskCreationE2E#projectLifecycleValidationSearchAndDeletion" test-compile failsafe:integration-test failsafe:verify
```

No Surefire/unit tests, prior E2E methods, exhaustive PostgreSQL search/authorization suites, full builds or Compose commands were run. Each attempt used newly created disposable test resources only.

**Exact results:**

| Attempt / local log under `backend/target/` | Maven finish (UTC) | Result |
| --- | --- | --- |
| `p1-project-lifecycle-e2e.log` | 2026-11-15T18:16:15Z | Exit 1; 1 test, 1 failure, 0 errors/skips; 01:17 min. Exact description-label locator stopped matching after draft entry. Trace confirmed retained textarea value. |
| `p1-project-lifecycle-e2e-retry.log` | 2026-11-15T18:20:24Z | Exit 1; 1 test, 1 failure, 0 errors/skips; 01:15 min. Description text matched both rendered paragraph and textarea. Scoped display assertions to paragraph role; textarea uses textbox role/name prefix. |
| `p1-project-lifecycle-e2e-retry2.log` | 2026-11-15T18:25:56Z | Exit 1; 1 test, 0 failures, 1 error, 0 skips; 01:27 min. Detail-open helper accepted the still-visible list-card heading before navigation completed; trace showed the next navigation click while busy. Fixed helper with detail heading level and awaited idle condition, without sleeps. |
| `p1-project-lifecycle-e2e-retry3.log` | **2026-11-15T18:29:32Z** | **BUILD SUCCESS; 1 test, 0 failures/errors/skips; 01:16 min.** Python's subsequent console rendering failed on cp1252 Unicode output; the saved complete Maven log and XML establish success independently of that wrapper error. |

`backend/target/failsafe-reports/TEST-pt.matriz.tasks.TaskCreationE2E.xml:2` records **tests=1, failures=0, errors=0, skipped=0, flakes=0**, suite **59.766 s**; line 73 names **`projectLifecycleValidationSearchAndDeletion`**, testcase **42.119 s**. The report replaces the previous P0 XML at the same path; P0 remains historical evidence, not rerun. Local artifacts are under `backend/target/e2e-artifacts/project-lifecycle/`; retained failure screenshot/trace describe the last failed attempt, not the passing run. Do not publish unsanitized browser traces containing disposable session/CSRF/fixture values.

**Observed assertions:**

- UI creation POST **201**, visible success and detail description, correct committed session creator and sole manager membership; list/dashboard presence and detail survive reload/reopen.
- Three UI searches return exact ordered cards: trimmed uppercase `CAFÉ mixed%_` matches the mixed-case accented project only; `cafe mixed%_` matches the unaccented sentinel only; literal `%_` matches both literal-name projects but excludes the `XY` near-match. No exhaustive search matrix duplicated.
- Empty name is blocked by native `valueMissing` with no mutation request or DB change. Three-space name receives real **422**, visible alert, unchanged name/description drafts, no creation-success message and unchanged seven-table committed snapshot.
- Manager UI PATCH **200** updates name and description, displays success; independent committed reads and reload/reopen confirm persistence.
- Target plus both unrelated sentinel projects contain synthetic task/assignment/comment/history dependents. Cancelled native confirmation sends no DELETE and leaves the entire snapshot unchanged. Confirmed deletion returns **204**, displays success and refreshes the complete list to exactly the two sentinels.
- After deletion, all seven domain tables exactly equal the baseline before UI creation: target project/membership/task/assignment/comment/history are gone and both unrelated populated projects/users are unchanged. After page reload the dashboard shows exactly **2 projects, TODO=2, IN_PROGRESS=0, DONE=0**, followed by the exact two remaining project-list names.

**Remaining gaps:** representative manager/project browser lifecycle is now verified; this is not exhaustive field/search/role/environment coverage. P0 permission evidence is reused, not rerun. Optional-description variants/limits, edit-failure drafts, induced 5xx/network errors, standalone task deletion and collaboration/filter browser flows for scenarios 05/06/07 remain outside this run. Scenarios 02/04/09 retain conservative partial status outside the explicit fixture; this section supersedes their earlier missing-browser claims only for the assertions above. Existing broad counts and historical topic rows are not new execution totals. Ignored local reports are not portable delivery artifacts.

### P0 — focused browser authentication and permission boundaries (2026-11-15)

Added only `TaskCreationE2E.authenticationAndPermissionBoundaries`. Extracted the existing private Vite/Chromium lifecycle into `withBrowser` in the same file; the original creation test's assertions are preserved, but it was not rerun. Artifacts are now namespaced under `target/e2e-artifacts/task-creation/` and `target/e2e-artifacts/authentication-boundaries/`. No production code, dependency/configuration or existing application data changes.

**Executed command (backend directory):**

```text
mvn -B -ntp -Pe2e "-Dit.test=TaskCreationE2E#authenticationAndPermissionBoundaries" test-compile failsafe:integration-test failsafe:verify
```

Direct Failsafe goals after test compilation deliberately avoid Surefire/unit execution. Only this one method ran; no other E2E/integration/unit suites, build delivery or Compose operations were executed. Each attempt used a new Testcontainers PostgreSQL 17.11 instance, Flyway, disabled demo seed, random loopback backend/Vite ports and fresh browser contexts; no existing Docker resources were stopped, recreated or pruned.

**Results:** initial attempt finished **2026-11-15T17:59:31Z**, BUILD FAILURE, 1 test / 1 failure / 0 errors / 0 skips (suite 78.82 s). It successfully observed invalid-login feedback, then failed at recovery because the test supplied only the corrected password after the app had remounted/reset the form. The failure trace showed no second login POST; this was native required-username blocking, not evidence that valid authentication failed. Corrected only the test to re-enter both credentials. Initial log: `backend/target/p0-authentication-e2e.log` (PowerShell UTF-16); failure screenshot/trace remain under `backend/target/e2e-artifacts/authentication-boundaries/` and describe this failed attempt, not the passing run. Traces contain disposable session/CSRF/fixture values and must not be published unsanitized.

The corrected attempt finished **2026-11-15T18:03:55Z**, **BUILD SUCCESS, captured Maven exit code 0**, total Maven time **01:23 min**. `backend/target/p0-authentication-e2e-retry.log` records **1 test, 0 failures/errors/skips**. Exact XML evidence: `backend/target/failsafe-reports/TEST-pt.matriz.tasks.TaskCreationE2E.xml:2` has tests=1, failures=0, errors=0, skipped=0, flakes=0, suite **65.019 s**; line 73 names `authenticationAndPermissionBoundaries`, **45.148 s**. This replaces the older creation-only XML at the same path; its previous execution remains historical evidence, not a rerun here.

**Observed outcomes:**

- Wrong-password UI POST returns 401; Portuguese invalid-login alert is visible; login screen remains and dashboard is absent; same-context `/users/me` returns 401. Re-entering both correct credentials reaches the dashboard and clears the alert.
- Manager sees GESTOR/project-management, task creation/edit, assignment-save and task-delete controls. Same-project unassigned member sees MEMBRO, task-create/comment controls, but no project-management/delete, task-edit/status/priority, assignment-save or task-delete controls. Authenticated outsider sees no dashboard project cards or project-list cards, an explicit empty list, and no project-delete/task-open controls.
- Real UI logins establish independent cookie sessions. Member project/task GET controls return 200; same-project project PATCH and DELETE return **403**. Each mutation fetches a real same-session CSRF token and sends it as `X-CSRF-TOKEN` through the private Vite proxy.
- Representative outsider identifier swaps: project GET and task GET **404**, task-title PATCH with real CSRF **404**. Fresh anonymous context: project/task GET **401**, project DELETE with real anonymous-session CSRF **401**. Final manager project GET remains 200. No full authorization matrix was duplicated.
- Fresh JDBC snapshots of all seven domain tables (users, projects, memberships, tasks, assignments, comments, history) equal the pre-request committed state after failed login, each member denial, outsider checks and anonymous checks. Nonempty assignment/comment/history dependents make denied deletion checks meaningful.

**Remaining gaps:** P0 is verified only for these fixtures and representative endpoints. Assigned-member browser edits, full role/endpoint matrices, logout/session expiry, HTTPS/Secure cookies, throttling and browser error rendering after forced 403/404 remain outside this run. Scenario 01's recorded invalid-feedback gap is closed within this fixture; canonical status stays conservatively partial pending broader authentication acceptance. Scenarios 02/08 remain partial; project lifecycle and P1 browser flows for 04/05/06/07/09 are untouched. API-client assertions share browser cookies but do not claim that a forbidden operation was submitted through hidden UI controls. Reports/artifacts are ignored local evidence, not portable release artifacts.

### Scenario 06 — confirmed focused PostgreSQL assignment evidence (2026-11-15)

Read-only report inspection confirmed that the latest `backend/target/failsafe-reports/TEST-*.xml` is `TEST-pt.matriz.tasks.TaskPostgresIT.xml`, with filesystem modification time **2026-11-15 11:47:29 (local time)**. This is a file timestamp, not an XML testcase completion timestamp; the report has no explicit completion timestamp. The older `TEST-pt.matriz.tasks.TaskCreationE2E.xml` was modified on 2026-11-14 at 23:23:11 and is not this execution.

**Exact evidence:** `backend/target/failsafe-reports/TEST-pt.matriz.tasks.TaskPostgresIT.xml:2` records suite `pt.matriz.tasks.TaskPostgresIT`, **tests=1, failures=0, errors=0, skipped=0, flakes=0**, suite time **14.31 s**. Line 70 names the sole testcase **`managerAssignmentLifecycleCommitsAndRejectsInvalidSetsWithoutPartialWrites`**, time **3.376 s**. Embedded startup output at lines 116–123 is dated 2026-11-15 around 11:47 UTC. `failsafe-summary.xml:2–8` independently records completed=1 and zero errors/failures/skips/flakes, timeout=false; it does not identify the method or establish the original shell command/Maven exit status.

The matching source at `backend/src/test/java/pt/matriz/tasks/TaskPostgresIT.java:706–763` asserts real authenticated MockMvc requests against disposable PostgreSQL: add one responsible; add multiple; reorder the same set; remove one; replace; clear; repeat clear. Every successful PUT returns 200, has the exact committed assignment IDs without duplicates, and is confirmed by subsequent task GET. Repeated equivalent sets leave the complete snapshot/history unchanged; effective changes add one history entry. Duplicate IDs and an existing but ineligible user return 422 with `responsibleUserIds` field errors and preserve all committed rows. Project/membership/task/comment data and the unrelated private-project snapshot remain unchanged.

**Scope:** confirmed scenario 06 API/DB evidence, not a new execution or a browser test. No tests, database queries or Docker operations were run during this inspection. The original invocation is not recoverable from these XML reports and is not reconstructed as an executed command. Rendered assignment controls/names and browser reload remain unverified; canonical scenario 06 stays partially verified. Ignored local reports are not portable delivery artifacts.

### Scenario 05 — focused PostgreSQL status/priority execution (2026-11-15)

Added `TaskPostgresIT.authorizedTaskMutationsCommitAndRefreshExactFiltersAndCounters` with **2 parameterized cases: manager and assigned member**. Reused existing real login/CSRF, isolated Testcontainers database, independent `committedTask` and project-state snapshots. Existing creation/defaults, validation and authorization-denial cases were not duplicated or rerun.

The explicit synthetic fixture contains four shared-project tasks: TODO/MEDIUM (mutation target assigned to the member), DONE/HIGH, IN_PROGRESS/LOW and TODO/HIGH; a private-project DONE/HIGH sentinel; and an accessible empty project for the manager. Shared task timestamps are deliberately equal so expected descending-ID order is deterministic. Expected values come from fixed test records, not the API results or copies of production aggregate SQL.

**Observed assertions, before mutations and after each change:**

- Both authorized roles successfully change the target status TODO→DONE, then priority MEDIUM→HIGH (200). Independent JDBC reads prove committed rows equal the original row except the selected columns; subsequent authenticated detail equals the mutation response. Sentinel task status/priority and the full private/empty-project snapshots remain unchanged.
- For both manager and member readers, all **16 omitted/individual/combined status/priority filter combinations** return exact ordered task IDs, matching item status/priority and exact `totalItems`, including empty combinations. The target leaves its old filters and enters the new filters after each mutation.
- Exact project `counts` include every state under the selected priority, **ignoring the status filter**, even when `items` is empty. `totalItems` instead follows **both** supplied filters (RF06/RF07/D9, `docs/requirements.md:14–15,26`). Zero buckets and totals are asserted.
- Dashboard global/per-project counts are exact for no priority and LOW/MEDIUM/HIGH, both without and with `status=DONE`. Status does not alter dashboard responses or universe; priority changes counts but never removes accessible projects, including the empty project and projects with zero matching tasks. Manager sees shared/private/empty projects and their combined counts; member sees only the shared project and its counts, excluding the private sentinel. Dashboard order and `totalProjects` are asserted.

Executed from repository root with installed Maven 3.9.4 / Java 21:

```text
mvn.cmd -f backend/pom.xml -Pintegration -Dit.test=TaskPostgresIT#authorizedTaskMutationsCommitAndRefreshExactFiltersAndCounters -l backend/target/status-priority-acceptance-20261115.log test-compile failsafe:integration-test failsafe:verify
```

**Actual result: exit 0, BUILD SUCCESS; 2 tests, 0 failures, 0 errors, 0 skipped** (class 16.73 s; Maven total 26.239 s). Only these two new parameterized invocations ran: no normal suite, earlier integration methods or browser E2E. Log: `backend/target/status-priority-acceptance-20261115.log`; standard latest-run report: `backend/target/failsafe-reports/pt.matriz.tasks.TaskPostgresIT.txt` now describes this two-case execution. Earlier dedicated execution logs remain preserved. `git diff --check` passed.

**Remaining browser/acceptance gaps:** canonical scenario 05 stays partially verified overall. Rendered select interactions, task/list display, project/dashboard refresh/navigation and page reload after mutation have no new browser evidence. This run exercises two real transitions per role, not every transition, history/no-op semantics, invalid input, concurrency, backend restart or production HTTPS/proxy behavior. Prior validation/denial evidence remains separately scoped.

Only test source and this document changed in this increment. Existing datasource/Flyway isolation, generated container credentials, disabled seed/Flyway clean and automatic test-resource disposal are unchanged. No production code, migration or Docker configuration changes; existing application databases, volumes, secrets and uncommitted work were not used or modified. No staging, commit or push.

### Scenario 02 — focused PostgreSQL project-management execution (2026-11-15)

Added three focused test methods to `backend/src/test/java/pt/matriz/tasks/TaskPostgresIT.java`, reusing real login/CSRF, existing synthetic fixtures and the independent committed-state snapshot. The snapshot now accepts explicit project IDs through an overload; existing callers retain the same shared/private scope. Existing authorization matrices, request validation and mocked PATCH omission/null cases were not duplicated or rerun.

- `authenticatedProjectCreationCommitsCreatorMembershipAndListEntry` — **3 parameterized cases**, description omitted / explicit null / padded text. A user who is only a member of another project creates via authenticated API. Independent JDBC snapshots prove normalized committed name/description, exact authenticated creator ID and exactly one membership, that creator as `gestor`; subsequent detail and list return the created project, with the existing shared project retained and the inaccessible private project absent.
- `projectSearchMatchesCaseAndLiteralWildcardsInPostgres` — **1 case with multiple search assertions**, unique fixture names and near-match controls. Actual PostgreSQL results prove trimmed case-insensitive partial matching, accent distinction (`Café` versus `Cafe`), literal `%`, `_` and escape character `!`, unmatched search, empty/whitespace search, exact descending-ID results/totals and exclusion of nonparticipant projects. This closes the database-matching gap, not browser search interaction.
- `projectEditingAndDeletionCommitCleanupWithoutChangingUnrelatedProject` — **1 lifecycle case**. Both target and unrelated projects have tasks, assignments, comments and history before mutation. Authorized PATCH returns updated detail/search results; an independent snapshot proves normalized committed name/description, unchanged creator/other project columns and unchanged dependents. Authorized DELETE returns 204; committed snapshots prove project, memberships and tasks removed; direct queries by the captured deleted task ID prove no orphan assignments/comments/history (rather than relying solely on project joins). Detail/task return 404, listing retains only the unrelated project, and that project's complete populated snapshot is identical before/after editing/deletion.

Executed from repository root, installed Maven 3.9.4 / Java 21:

```text
mvn.cmd -f backend/pom.xml -Pintegration -Dit.test=TaskPostgresIT#authenticatedProjectCreationCommitsCreatorMembershipAndListEntry+projectSearchMatchesCaseAndLiteralWildcardsInPostgres+projectEditingAndDeletionCommitCleanupWithoutChangingUnrelatedProject -l backend/target/project-acceptance-20261115.log test-compile failsafe:integration-test failsafe:verify
```

**Actual result: exit 0, BUILD SUCCESS; 5 tests, 0 failures, 0 errors, 0 skipped** (class 14.17 s; Maven total 23.697 s). Only these new cases ran, not the normal suite, earlier integration methods or browser E2E. Log: `backend/target/project-acceptance-20261115.log`; latest-run report: `backend/target/failsafe-reports/pt.matriz.tasks.TaskPostgresIT.txt`. Earlier dedicated comment/authorization logs are preserved; the standard report now describes this five-case run.

**Remaining scope:** canonical scenario 02 stays partially verified overall. No browser project forms/search results/navigation/reload, delete confirmation/cancellation, success/error feedback or draft retention was exercised. Real same-project member 403 PATCH/DELETE checks remain a role-matrix gap; existing mocked denials and earlier real cross-project 404 tests are not promoted to new execution. Project PATCH omission/null persistence variants and standalone task-deletion cleanup are not asserted by this run. Scenario 09 gains project-cleanup evidence but remains partial for standalone task cleanup and UI error handling.

Only test source and this document were changed in this increment. The disposable PostgreSQL fixture overrides datasource and Flyway, disables demo seed/Flyway clean, and never selects existing application databases, secrets, Compose resources or volumes. No production/migration/Docker configuration edits, staging, commit or push. `git diff --check` passed.

### Scenarios 01/08 — focused PostgreSQL authorization execution (2026-11-15)

Added only test code to the existing isolated `TaskPostgresIT` fixture; no application, migration or Docker configuration changes. Each parameterized invocation creates its own accounts/projects, a private task with nonempty assignments/comments/history, and a shared-project sentinel task. The outsider is a valid member of the shared project but has no membership in the private project. Valid CSRF and valid payloads ensure the denial is authorization, not request validation. Member access to the shared project and manager access to the private task are positive setup controls.

**12 cross-project cases:** project detail GET, project PATCH/DELETE, eligible-users GET, project-tasks GET, task DELETE, task status/priority PATCH, responsible-users PUT, comments GET/POST, and history GET. Every swapped private identifier returns **404** with the generic problem response and no items/projectId/content. Before/after snapshots via independent JDBC connections compare every column of the invocation's projects, memberships, tasks, assignments, comments and history, including the unrelated shared sentinel. Every denied request leaves these rows identical. The same endpoint/payload then succeeds for the private project's manager (GET/PATCH/PUT 200, POST 201, DELETE 204). These positive controls establish permission-sensitive endpoint execution, **not** complete successful mutation/cascade correctness or new browser lifecycle coverage.

**One real invalid-password case:** existing synthetic account + actual password encoder/authentication provider, valid CSRF, incorrect password → 401; the same session still receives 401 on profile and an otherwise accessible shared-project API request, password hash and project/dependent snapshots stay unchanged, and a subsequent correct-password login/profile request succeeds. No password/hash/token is logged by the test assertions.

Executed from the repository root with installed Maven 3.9.4 / Java 21:

```text
mvn.cmd -f backend/pom.xml -Pintegration -Dit.test=TaskPostgresIT#crossProjectIdentifierSwappingIsHiddenAndCannotChangeCommittedState+realInvalidPasswordDoesNotAuthenticateOrChangeAccount -l backend/target/authorization-acceptance-20261115.log test-compile failsafe:integration-test failsafe:verify
```

**Actual result: exit 0, BUILD SUCCESS; 13 tests, 0 failures, 0 errors, 0 skipped** (test class 17.44 s; Maven total 26.856 s). Only the 12 new parameterized invocations and new login method ran: no normal suite, previous integration methods or browser E2E. Log: `backend/target/authorization-acceptance-20261115.log`; current report: `backend/target/failsafe-reports/pt.matriz.tasks.TaskPostgresIT.txt` (standard latest-run report, not a preserved full-suite total). Earlier dedicated execution logs remain in place.

Datasource/Flyway are both bound to disposable Testcontainers PostgreSQL with generated credentials; demo seed and Flyway clean stay disabled. Existing databases, volumes, Compose configuration and secrets are not used. **Canonical cases 01 and 08 remain partially verified:** browser invalid-login feedback, full 403 role matrix (including assigned-member positive controls), list/dashboard exclusion and exhaustive endpoint coverage remain outside this focused run. `git diff --check` passed.

### Scenario 07 — focused comment integration execution (2026-11-15)

Recovery inspection found the complete `TaskPostgresIT.memberCreatesCommentWithAuthorTimestampAndCommittedPersistence` test already present at `backend/src/test/java/pt/matriz/tasks/TaskPostgresIT.java:192–234`, the scenario 07 table update above, and an earlier one-test passing Failsafe report. The execution-details section referenced by the table was missing. No test or production-source edits were needed; existing working-tree changes were preserved.

Executed from the repository root using the installed Maven 3.9.4 `mvn.cmd` (Java 21):

```text
mvn.cmd -f backend/pom.xml -Pintegration -Dit.test=TaskPostgresIT#memberCreatesCommentWithAuthorTimestampAndCommittedPersistence -Dfailsafe.reportsDirectory=target/comment-acceptance-resumed-reports -l backend/target/comment-acceptance-resumed.log test-compile failsafe:integration-test failsafe:verify
```

**Actual result: exit 0, BUILD SUCCESS; 1 test, 0 failures, 0 errors, 0 skipped** (test 16.10 s; Maven total 21.675 s). Only the selected new comment method ran; the normal suite, other integration cases and browser E2E were not run. The log is `backend/target/comment-acceptance-resumed.log`. Despite the requested reports-directory property, Failsafe wrote the result to its standard `backend/target/failsafe-reports/pt.matriz.tasks.TaskPostgresIT.txt`; the earlier dedicated log was not overwritten.

The passing assertions cover authenticated member creation, 201/Location, trimmed content, session-derived author ID, UTC timestamp bounded by the request, matching committed data through an independent JDBC connection, and a manager's subsequent GET returning the identical comment. The existing fixture overrides both datasource and Flyway connections to a disposable PostgreSQL Testcontainer, disables demo seeding and Flyway clean, and does not use application data, secrets, Compose resources or existing volumes. Existing production code, migration files and Docker configuration were not modified. Scenario 07 remains **partially verified overall**: browser submission/rendering/reload and the additional cases listed in the table were not exercised.

### Legacy creator / OpenAPI mismatch — resolved by approved contract correction (2026-11-15)

**Resolution now applied:** both `openapi.yaml` and `docs/matriz-openapi.yaml` use `type: [integer, 'null']` for `Project.createdByUserId`, retaining the required property, `readOnly` and positive integer minimum. The description limits null to unknown legacy creators and explicitly guarantees non-null session-derived creator/automatic manager for new API-created projects. No Java, migration or data was changed. The proposal/status bullets below preserve the original diagnosis and are historical, superseded by this resolution.

**Exact focused validation:** executed `java --class-path <existing-local-snakeyaml-2.4.jar> backend/target/ProjectContractCheck.java .` from repository root. Final result **exit 0, CHECKS_PASSED=16**: both YAML documents parse; byte-identical copies; required/readOnly retained; legacy null and positive creator accepted; missing creator, zero, negative, string, boolean and fractional values rejected; actual JSON fixture extracted from `createsProjectWithSessionCreatorAndMembership` remains valid/non-null; shared references retained; dashboard reference chain `Dashboard → DashboardProject → Project` reaches the corrected schema. A harmless Java unchecked-generics warning was emitted. Initial helper attempt passed 14 checks then failed on an incorrect assumption that dashboard items were inline; after inspecting the actual `$ref` chain, only the helper was corrected and all 16 checks passed.

The helper is an ignored, local artifact at `backend/target/ProjectContractCheck.java`; it uses already-installed SnakeYAML with no dependency additions. These are explicit focused schema/fixture assertions, **not** a complete JSON Schema/OpenAPI validator, durable CI regression suite, or new execution of controller tests. Python `yaml`/`jsonschema` are unavailable; no packages were installed. The actual create method was inspected at `ProjectController.java:123–168`: rejects unknown identity, stores/returns authenticated user ID and inserts gestor membership transactionally. Existing creation/legacy tests were inspected, not rerun. V2's historical comment about a necessary backfill predates this nullable contract correction and was intentionally not edited; no backfill is required for this property under the corrected schema. `git diff --check` passed. Broader acceptance classifications above are unchanged.

#### Historical diagnosis/proposal (superseded)

- **Contract:** `openapi.yaml:959–989` requires `Project.createdByUserId` to be an integer ≥1 (non-null), including project list/detail/PATCH and nested dashboard uses of Project.
- **Implementation:** V1 did not record creator (`backend/src/main/resources/db/migration/V1__initial_schema.sql:9–14`). V2 adds a nullable FK and intentionally preserves unknown legacy creators (`V2__project_creator.sql:1–4`). `ProjectController.java:25–28,46–48` uses `Long`/`getObject`, so null is truthful; create (`:95–103`) writes authenticated creator and manager participation. PATCH does not change creator. Dashboard also maps nullable creator (`TaskController.java:234–235`).
- **Specific regression evidence:** `ProjectControllerTest.legacyCreatorRemainsNullRatherThanBeingInferredFromMembership` asserts JSON null (`:170–181`); `TaskControllerTest.dashboardUsesExactProjectSchemaIncludingNullDescriptionAndLegacyCreator` asserts the nested legacy form; `frontend/src/projects.test.js:18–34` confirms permissions derive from participation, not authorship. These tests validate intended legacy behavior, **not conformance to the current non-null OpenAPI schema**. No live database inspection was made; the number/existence of affected installed rows is unknown.
- **Smallest safe resolution, pending explicit contract approval:** change only `Project.createdByUserId` response type in **both** canonical `openapi.yaml` and mirror `docs/matriz-openapi.yaml` to `type: [integer, 'null']`; retain `required`, `readOnly` and `minimum: 1` (applies to integer values). Describe null narrowly as unknown historical creator; explicitly guarantee authenticated non-null creator/automatic manager for all newly API-created projects, and keep writes to creator forbidden. Clarify the exception in requirements/database documentation and add focused schema-response checks for legacy null, known positive ID, missing field, invalid non-null ID, new-project response and nested dashboard Project. Check both copies remain identical.
- **No historical-data rewrite:** never infer creator from current manager, smallest membership/user ID, timestamps or seed account; never hide/drop legacy projects, fabricate a creator, remove the response key, or edit V2 after application. Current managers retain their existing rights. If retaining non-null contract is mandatory, it requires reliable external authorship evidence and separately authorized data remediation; no such evidence is established, so it is not the minimal safe solution here.
- **Status:** confirmed contract defect, not a demonstrated authorization bypass. Contract change is **not implemented or approved by this audit**; full OpenAPI-conformance claims remain blocked until an approved resolution and targeted schema checks. Acceptance of newly created projects is separately evidenced at controller level, not invalidated by this legacy exception.

### Recommended next steps (no execution authorized by this record)

1. Nullable legacy response correction and 16 focused local assertions are complete (see resolution above). If durable automated contract coverage is desired, separately approve adding these checks to the existing test tooling; do not infer full OpenAPI validation or mutate historical data.
2. On a separately approved isolated fixture, close scenario 01/02 first, then authorized 04/05, assignment 06, comment 07 and edit/delete 08. Capture exact inputs/roles, expected vs observed UI/API/DB results, UTC timestamp and evidence path; deletion only of that fixture's synthetic records. Existing test passes need not be rerun solely to populate this matrix.
3. Add focused automation for any confirmed gap/defect and update this section with actual outcomes, not projected passes. Keep remote CI and final delivery tasks separate; none was performed here.

## Historical P1 documentary verification

## Fecho formal aprovado — 2026-11-11

O utilizador aprovou formalmente o fecho da P1 documental com as limitações do relatório final aceites. P1 concluída; D1–D10 e pontos 1–7 mantidos. Este registo prevalece sobre referências anteriores a fecho pendente abaixo e nos restantes artefactos. Sem implementação ou autorização P2. A preparação Git local foi autorizada separadamente: .gitignore, inspeção de segredos e staging explícito, sem commit, remoto ou push.

## Preparação Git local — autorização separada

Repositório inicializado apenas na pasta Matriz, sem commit ou remoto. Baseline preparada por lista explícita de 17 ficheiros: .gitignore; README.md; AGENTS.md; dois OpenAPI; dez Markdown em docs (incluindo docs/ui/README.md); duas skills locais matriz-orchestrator. Nenhum `git add .`.

Inspeção textual por padrões de chaves privadas, tokens de fornecedores, atribuições de credenciais, URLs com credenciais, emails e caminhos pessoais. O email dos contratos é exemplo no domínio reservado example.org. Um caminho absoluto com nome do perfil local no plano foi substituído pela referência portátil `~/.openharness/skills/matriz-orchestrator/SKILL.md`, sem alterar a autorização. Não foram identificados segredos reais nos candidatos revistos. Esta inspeção heurística não é garantia absoluta nem auditoria dos ficheiros excluídos.

.gitignore exclui .env e variantes, credenciais/chaves, dados/dumps, temporários, builds, estado local das ferramentas, materiais de aulas, protótipo executável e skills não revistas. As exclusões foram verificadas com git check-ignore. Estes materiais continuam no disco, mas não fazem parte da baseline; referências documentais a aulas/protótipo dependem desses materiais locais. As outras skills podem conter regras antigas e não foram certificadas nem preparadas.

Revisão de `git diff --cached` e verificação de whitespace: detetados dois espaços finais históricos em docs/user-stories.md:38, removidos sem alteração semântica antes da verificação final. Git avisou sobre conversão LF/CRLF pela configuração já existente; nenhuma configuração global alterada. Sem implementação, commit, remoto, push ou P2. O relatório final da operação identifica o resultado da revalidação da staging area.

## Relatório da entrega — D1–D10 e pontos 1–7

Fecho P1 **não aprovado**; P2 não autorizada. Esta secção prevalece sobre as revisões e hashes anteriores preservados como cronologia.

Atualização autorizada limitada aos 16 ficheiros do projeto e à cópia global específica de matriz-orchestrator. Dois workers reais com escrita delimitada: `a7d55f96d` (dois OpenAPI) e `a55562100` (dez Markdown funcionais/técnicos). Sessão principal: três skills, review.md e verification.md. Nenhum outro ficheiro autorizado por inferência.

### Decisões incorporadas

1. Baseline Java 21 LTS, Spring Boot 3.5.x, React 19.x, PostgreSQL 17.x; confirmar suporte e compatibilidade antes da implementação e fixar versões exatas. Suporte **não verificado nesta P1**; substituições pedem aprovação.
2. Mesma origem frontend/API, HTTPS publicado, proxy em desenvolvimento; sem CORS arbitrário com credenciais.
3. Projetos id DESC; tarefas createdAt DESC, id DESC; comentários e histórico data UTC ASC, id ASC; elegíveis nome sem distinguir maiúsculas ASC, id ASC.
4. Pesquisa substring literal após trim, sem distinguir maiúsculas e distinguindo acentos; omitida/vazia não filtra; % e _ literais.
5. Descrições: criação omitida/null ausente; PATCH omitida mantém e null remove; texto vazio após trim rejeitado; resposta null se ausente.
6. Membro cria com responsibleUserIds omitido ou []; não vazio resulta em 403 sem criação parcial; gestor pode atribuir elegíveis.
7. Listas completas sem paginação no MVP; bootstrap CSRF e convenções aprovadas condicionalmente à coerência com D1–D10/pontos 1–6. Esta aprovação não fecha P1.

### Comparativo desta entrega (não é git diff)

```diff
- Versões, topologia, ordenação e pesquisa concretas pendentes.
+ Baseline e mesma origem aprovadas; suporte a verificar antes da implementação.
+ Projetos id DESC; tarefas createdAt DESC, id DESC; restantes listas com desempate ID.
+ Pesquisa literal, case-insensitive e accent-sensitive, vazio sem filtro.
- null e [] explícito por membro por decidir.
+ null exprime ausência/remoção; [] permitido na criação sem atribuições.
- Listas/transportes contratuais apenas propostos.
+ Ponto 7 aprovado condicionalmente à consistência das decisões; fecho P1 pendente.
```

Sem baseline Git disponível na revisão anterior; não afirmar diff Git completo ou certificação de alterações prévias. Não executar aplicação, instalações, migrations, mutações Git ou testes dinâmicos.

### Revisões e verificações desta entrega

Workers anteriores registados: QA `ae89499e6`, Security `a3adcbd23`, Code Reviewer `ae722f37c`. Na retoma de 2026-11-11, `task_list` não apresentou tarefas recuperáveis; os respetivos relatórios não foram recuperados nem considerados aprovados. Foram executadas novas revisões independentes, só de leitura e com recortes essenciais separados, descritas abaixo. As evidências anteriores não certificam os novos snapshots.

Worker de contrato `a7d55f96d` reportou igualdade byte a byte, YAML sem duplicados, 22 operações, 154 referências locais, 30 esquemas JSON Schema e verificações de nulidade/campos/pesquisa vazia/UTC/CSRF com dependências existentes. Hash dos dois contratos: `f224a0837daab884894a29029874494d6f8a23c2c8c3b6822833b6e0d83d80a8`. Worker documental confirmou leitura dos dez Markdown e preservação textual das secções históricas. Nenhuma consulta web de suporte efetuada.

### Conclusão documental da retoma — 2026-11-11

Resultados novos efetivamente recebidos; não equivalem a aprovação humana de fecho P1:

| Revisão independente | Worker real | Resultado e evidências |
| --- | --- | --- |
| QA | `a7e507313` | Sem achados comprovados no recorte: D8/trim/limites, descrições null/PATCH, TODO/MEDIUM, CreateTask omitido/[]/403 atómico, pesquisa literal, ordenações, contagens e listas completas. Referências: openapi.yaml:59–66, 265–270, 916–979, 1035–1114, 1148–1149, 1305–1322, 1383–1436, 1536–1551. |
| Security | `ad9f7c51c` | Sem incoerências comprovadas: D3/D4, identidade da sessão, criação privada/transacional, CSRF bootstrap/login/logout, mesma origem e 401/403/404. Referências: openapi.yaml:89–91, 265–270, 341–344, 612–616, 680–798, 839–868, 1505–1533, 1556–1557; docs/database.md:46. |
| Code Reviewer | `aec69f3cd` | Sem divergências vigentes comprovadas: rastreabilidade D1–D10/pontos 1–7, limites de âmbito, distinção aprovado/implementado/fecho e coerência das skills locais. Referências: implementation-plan.md:13–42; AGENTS.md:5–17; review.md:5–7; secção vigente deste registo antes da consolidação. Não certifica cronologia ou alterações sem baseline. |

**Verificações estáticas executadas pela sessão principal**, com Python e dependências já existentes, sem instalação:

- Dois OpenAPI idênticos byte a byte; SHA-256 `f224a0837daab884894a29029874494d6f8a23c2c8c3b6822833b6e0d83d80a8`.
- YAML carregado com deteção de chaves duplicadas: sem duplicados. 154 referências locais resolvidas; 15 caminhos; 22 operationId únicos; 30 esquemas verificados com `Draft202012Validator.check_schema`; 15 ocorrências de parâmetros de caminho obrigatórios e coerentes com o caminho.
- Três cópias autorizadas de matriz-orchestrator idênticas byte a byte, incluindo a global; SHA-256 `be73246ea7d0dd61e03642b1e4508b4c38c3dfff054f0f294b44f004719d478c`. Loader `skill(matriz-orchestrator)` carregou nesta retoma o conteúdo com D1–D10/pontos 1–7 e portão P1/P2.
- Uma primeira chamada Python falhou por quoting do shell; a chamada corrigida expandiu indevidamente `$ref` e mostrou zero referências. Esse resultado não foi aceite: nova chamada com chave construída por `chr(36)` confirmou as 154 referências e os parâmetros. Não houve alteração de ficheiros nesses comandos.
- `git status --short` falhou: diretório sem repositório Git. Não foi inicializado Git; sem baseline/diff Git ou certificação de alterações anteriores.

**Alterações nesta retoma:** apenas `docs/review.md` e `docs/verification.md`, substituindo o estado de revisões pendentes por resultados reais e consolidando evidências/limitações. Nenhuma divergência funcional/contratual comprovada exigiu editar OpenAPI, skills ou outros documentos. Comparativo documental, não git diff: revisões anteriores indisponíveis → novas revisões essenciais recebidas; igualdade e estrutura reportadas anteriormente → verificações atuais executadas; fecho P1 pendente → mantém-se pendente.

**Limitações e portão:** sem validador OpenAPI integral disponível (`openapi_spec_validator` ausente); validação de esquemas isolados não equivale a validação integral do contrato ou de payloads. Sem testes unitários, integração, E2E, aplicação ou segurança dinâmica. Revisões novas limitadas aos recortes indicados; não repetem nem certificam toda a cronologia. Suporte/compatibilidade da stack e versões exatas continuam a verificar antes de implementação futura autorizada; não são decisões funcionais em falta. Sem código, instalações, migrations, dados, configurações executáveis, commits, integração ou P2.

**Resultado:** sem bloqueios documentais identificados nestas verificações essenciais. Consolidação documental pronta para apreciação humana; aguardar aprovação explícita para fechar P1. P2 não autorizada.

## Cronologia anterior — atualização D3/D4 e criação aprovadas

Esta secção prevalece sobre classificações de pendências e hashes da entrega anterior, preservadas abaixo como cronologia. O utilizador **não aprovou o fecho P1** e confirmou que a matriz D3 e política D4 já estavam aprovadas. A atualização agora autorizada não implementa nem testa os controlos.

- D3: gestor edita/elimina projeto, cria/edita/altera estado de qualquer tarefa do seu projeto, elimina tarefas e gere responsáveis; membro cria tarefas e só edita/altera estado das atribuídas a si; ambos consultam/comentam. 401 sem autenticação; 403 sem permissão; 404 recurso inexistente/inacessível.
- D4: sessão Spring Security; inatividade máxima 30 min e máximo absoluto 8 h; HttpOnly, SameSite=Lax, Secure em HTTPS; CSRF login/logout; rotação no login e invalidação no logout.
- Criação de projeto: criador automaticamente gestor na mesma transação; projeto privado sem outros membros.
- SEC-01 deixa de ser bloqueio por falta de matriz; SEC-02 deixa de ser pendência nos parâmetros aprovados. A referência anterior a decisões por aprovar foi incorreta; CORS/topologia permanecem fora desses parâmetros.
- Alterações limitadas aos ficheiros anteriormente autorizados. Hashes e revisões anteriores não certificam esta nova versão; ver resultados específicos a acrescentar abaixo.

### Verificações desta atualização — worker de contrato

Worker real `a609e3749` leu integralmente ambos os YAML e alterou apenas esses dois ficheiros. Reportou YAML sem duplicados, 154 referências resolvidas, 15 caminhos, 22 operações com identificadores únicos e 30 esquemas verificados com JSON Schema; CSRF nas operações mutáveis e sessão/401 nas protegidas. Cópias byte-idênticas, SHA-256 `9d3d742663bc3093061da08f74c19cba795fc2df3c61ff8171b81b73bedc5d62`. Sem validador OpenAPI dedicado disponível, instalações ou testes funcionais.

Contrato explicita que membro não pode usar CreateTask para atribuir responsáveis (nem a si próprio). O tratamento de um conjunto explicitamente vazio `responsibleUserIds: []` enviado por membro ficou como detalhe contratual pendente, sem inventar aceitação/rejeição e sem permitir novas atribuições.

Revisão independente de consistência concluída pelo worker `a1468e0db`, somente leitura, usando matriz-code-reviewer e matriz-security na mesma sessão de revisão (não duas revisões independentes entre si). Resultado: sem achados bloqueantes nem contradição vigente D3/D4; criação privada/transacional e prioridade como edição coerentes, membro sem autoatribuição via criação. Não foi executada nova revisão QA separada nesta atualização; as revisões QA anteriores mantêm a sua data lógica e âmbito anteriores.

Verificações finais reportadas: YAML sem duplicados, 154 referências locais resolvidas, 22 operationId únicos, 15 ocorrências de parâmetros de caminho obrigatórios, 30 esquemas JSON Schema 2020-12 e 7 casos estáticos aprovados. DTOs não aceitam identidade/papel no pedido de criação de projeto, estado/prioridade na criação de tarefa ou responsáveis na edição textual. Sessão/CSRF combinados em mutações protegidas; login exige CSRF. Loader `skill(matriz-orchestrator)` carregou o conteúdo atualizado com sucesso.

Dois contratos idênticos byte a byte, hash final `9d3d742663bc3093061da08f74c19cba795fc2df3c61ff8171b81b73bedc5d62`. Três skills autorizadas idênticas byte a byte, hash final `9da79a4621279d79cf40281e6f437585b4d0651d9a44159e71fd2bcdc1c3e86a`.

Limitações: sem baseline Git, CLAUDE.md ausente, sem validador OpenAPI integral ou testes da aplicação/segurança dinâmica. Recomendação do revisor: sem bloqueio documental identificado para apreciação humana, não aprovação automática. Pendências atuais: versões da stack, topologia/origens CORS, ordenação e pesquisa concretas, limpeza de descrições, aceitação de conjunto vazio explícito no DTO de criação por membro e aprovação final dos detalhes contratuais. Não reapresentar D3/D4 ou papel inicial do criador como decisões em falta. Fecho P1 aguarda aprovação explícita; P2 não autorizada.

### Comparativo desta atualização

```diff
- Matriz por operação pendente; papel do criador pendente.
+ Matriz D3 completa aprovada; criador gestor, projeto privado e criação transacional.
- Duração, SameSite, Secure e rotação de sessão pendentes.
+ Máximo inativo 30 min, absoluto 8 h; HttpOnly, SameSite=Lax, Secure em HTTPS.
+ CSRF inclusive login/logout, rotação no login, invalidação no logout.
```

Documentos afetados: README, AGENTS, requisitos, histórias, restrições, arquitetura, modelo, plano, workflow, revisão, este registo, README UI, ambos OpenAPI e três skills autorizadas. Sem novo ficheiro, instalação, migration ou código. P2 bloqueada.

## Entrega anterior — cronologia e evidências anteriores à atualização D3/D4

As secções seguintes até aos critérios de decisão preservam o relatório anterior. As classificações de pendência e os hashes aí descritos foram ultrapassados pela atualização no início deste documento; não representam o estado vigente.

### Estado da entrega anterior

Alterações documentais e revisões concluídas, com pendências explícitas; aprovação final humana P1 pendente. P2 não iniciada. Decisões aprovadas D1–D10 em `implementation-plan.md`. Nenhuma funcionalidade é declarada implementada por esta documentação.

## Execução real e limites

- Skill matriz-orchestrator ativada; skill-creator e matriz-documentation consultadas. A regra antiga de responsável único da skill Documentation não prevalece sobre D5; essa skill não foi alterada, por estar fora de âmbito.
- Sessão principal: documentos e três cópias autorizadas de matriz-orchestrator. Worker real separado: os dois contratos OpenAPI. Revisões independentes de QA/Security/Code Reviewer efetivamente solicitadas; resultados abaixo.
- Não foram executados testes unitários, integração, E2E, API, browser ou segurança dinâmica; não há evidência de implementação Java/React/PostgreSQL nesta P1.
- Sem instalações, migrations, alterações de dados/configurações executáveis, commits, merges, push ou deploy.

## Comandos e observações

| Verificação | Resultado observado | Limitação |
| --- | --- | --- |
| Leitura de fontes e comparação das skills antes da edição | Três cópias de matriz-orchestrator tinham o mesmo conteúdo, com regra antiga de responsável único | Comparação textual por leitura, não teste do loader |
| `git status --short && git branch --show-current` | Ferramenta pediu confirmação; não produziu estado Git | Não declarar comando executado com sucesso |
| `git diff --stat; git diff --check; git status --short` | Falhou: diretório não é repositório Git | Sem diff Git/baseline versionada nem verificação de whitespace pelo Git |
| `pwd; git rev-parse --show-toplevel` com cwd explícito | Confirmou diretório Matriz; Git respondeu `not a git repository` | Não inicializar Git nem criar commits para contornar |

## Diffs e rastreabilidade

Sem metadados Git neste diretório, o comparativo da entrega será apresentado por ficheiro com alterações antes/depois conhecidas pelas leituras e edições, sem o designar `git diff` completo. O texto histórico dos Markdown foi preservado abaixo de secções atuais com precedência explícita D1–D10. Novos documentos: implementation-plan.md e este verification.md. A skill teve substituição localizada das regras fundamentais. O contrato foi revisto pelo worker e por revisores independentes; as correções e a re-revisão estão registadas abaixo.

## Comparativo das alterações conhecidas (não é git diff)

| Ficheiro(s) | Antes | Depois P1 |
| --- | --- | --- |
| README.md | Apresentação Fichas 01/02, MVP anterior | Secção P1 com stack/MVP aprovados, links plano/verificação; original preservado como histórico |
| AGENTS.md | Dez instruções gerais | Portão documental P1, D1–D10, fonte canónica e revisão independente acrescentados |
| docs/requirements.md | RF01–RF06 núcleo; RF07 opcional | RF01–RF12 atuais, D8–D10, originais identificados como históricos |
| docs/user-stories.md | US01–US06, um responsável | US01–US12 atuais, zero/vários responsáveis, prioridades, sessão, CRUD, contagens e casos-limite |
| docs/constraints.md | Stack/âmbito/identidade pendentes | Decisões aprovadas separadas de matriz de permissões e detalhes ainda pendentes |
| docs/architecture.md | Proposta monólito e stack em aberto | Stack aprovada, sessão/CSRF e módulos conceptuais; propostas anteriores preservadas |
| docs/database.md | Tarefa com responsavel_id NOT NULL, equipas e regras team leader/developer | Modelo alvo com participação direta, atribuição N:M, prioridade e histórico limitado; diagrama antigo rotulado histórico |
| docs/review.md | Revisões Fichas 01/02 | Matriz de reconciliação D1–D10 e referências às revisões reais |
| docs/agent-workflow.md | Workflow geral proposto | Portão P1 documental e proibições explícitas; workflow futuro não é autorização |
| docs/ui/README.md | Instruções protótipo com responsável obrigatório | Explicita divergência D5 e ausência de integração; HTML/CSS/JS intactos pela intervenção |
| docs/implementation-plan.md | Ausente | Novo registo D1–D10, rastreabilidade, tarefas P1 e sequência futura bloqueada |
| docs/verification.md | Ausente | Novo registo de evidências, limitações, comparativos e revisões |
| openapi.yaml e docs/matriz-openapi.yaml | v0.3.0, Bearer, responsável singular, operações incompletas | v0.4.0, proposta sessão/CSRF, operações do MVP, limites D8, contagens D9, histórico D10; cópia sincronizada pelo worker |
| Duas skills locais + global autorizada | Responsável único; stack/âmbito pendentes | Substituição localizada da secção 4 por decisões e portão P1, mantendo restantes secções |

Diff conceptual comum às três cópias da skill:

```diff
- Cada tarefa tem um único responsável obrigatório desde a criação.
- Stack e funcionalidades do MVP continuam por validar.
+ Zero ou mais responsáveis elegíveis por participação direta no projeto.
+ D1–D10 aprovadas; consultar documentos vigentes; matriz de permissões ainda pendente.
+ P1 documental; revisões reais; diffs e aprovação final; sem P2.
```

Diff conceptual do contrato (não substitui revisão integral dos YAML):

```diff
- version: 0.3.0
- bearerAuth / JWT
- responsibleUserId obrigatório
+ version: 0.4.0 (proposta contratual para aprovação final P1)
+ sessionCookie e csrfToken
+ responsibleUserIds: conjunto com zero ou mais IDs elegíveis
+ autenticação, edição/eliminação, prioridades, elegíveis, painel e histórico D10
```

## Revisões independentes

Workers reais: contrato `a29e34658`; QA inicial `ac7a95684` falhou ao emitir relatório por UnicodeEncodeError (cp1252, seta U+2192), seguido de erro assíncrono do runtime. Não contar como revisão concluída. Nova revisão QA `a53f91b24` concluída, com saída ASCII, sem alterar configurações. Security `a95d39902` e Code Reviewer `aa8ade497` entregaram relatórios independentes em leitura.

### Resultados recebidos e tratamento

| Achado | Gravidade / evidência | Tratamento |
| --- | --- | --- |
| CR-01 | Média: CreateTask aceitava priority HIGH/LOW apesar de D8 exigir MEDIUM inicial (versão revista inicialmente, linhas 1001–1006) | Correção solicitada ao worker: prioridade inferida MEDIUM, sem escolha no DTO de criação |
| CR-02 | Média: participação inicial do criador afirmada em database/US01 mas pendente no contrato (linhas 86–87/1410–1411 da versão inicial) | Correção solicitada: associação à identidade autenticada conforme Ficha 03, sem inferir papel/matriz |
| CR-03 | Baixa: resposta de histórico dizia paginado e esquema dizia sem paginação | Correção solicitada: uniformizar descrição sem paginação |
| SEC-01 | Bloqueante de decisão: matriz gestor/membro por operação e política de criação não aprovadas | Mantido explícito como pendência humana; não impede registar decisões documentais mas impede fechar autorização detalhada |
| SEC-02 | Alta, risco de desenho: duração, Secure/SameSite e origens ainda pendentes | Mantido como pendência operacional; não inventar configuração aprovada nem afirmar vulnerabilidade demonstrada |
| SEC-03 | Média: bootstrap CSRF podia criar sessão anónima sem Set-Cookie/ligação formal no contrato | Correção solicitada como proposta de transporte: sessão anónima vinculada ao token, cookie e no-store documentados |
| QA-01 | Alta: criação aceitava prioridade alternativa; mesmo problema CR-01 | Correção solicitada conforme D8, sem alterar a decisão |
| QA-02 | Média: histórico referido como paginado e sem parâmetros de navegação; estado do esquema mudou durante a correção | Uniformizar proposta sem paginação e lista completa; confirmar revisão final |
| QA-03 | Baixa: consulta GET de tarefa sem resposta 400 para taskId inválido | Acrescentar resposta Problem 400 proposta e sincronizar cópia |

QA confirmou coerência descritiva das contagens/filtros D9 e do histórico limitado D10, com ressalvas QA-01–03. Comparou contratos byte a byte, em leitura; sem validação automatizada OpenAPI nem testes funcionais. Code Reviewer recomenda aprovação após correções documentais, não aprovação incondicional. Security conclui revisão estática sem vulnerabilidades em execução confirmadas, com bloqueio de política de autorização. Não houve testes dinâmicos nem implementação.

### Evidências estáticas reportadas pelos revisores

- Code Reviewer: dois OpenAPI idênticos (48 177 bytes; SHA-256 inicial `1fa8137391cfc83860e93c991beb843e4457ef0b46828e7b89c63b1cb191f14f`); três skills idênticas (6 430 bytes; SHA-256 `2955103de4404f8be74e1039296385562552cc5fb598738e7f29be0633f1ff65`). Hash do contrato refere-se à versão anterior às correções CR/SEC e deve ser atualizado após confirmação final.
- Code Reviewer: YAML sem chaves duplicadas, 154 referências locais resolvidas, esquemas compatíveis com metaschema JSON Schema 2020-12, operationId únicos, parâmetros de caminho obrigatórios e CSRF em operações mutáveis; links Markdown explícitos sem destinos inexistentes.
- Security confirmou igualdade byte a byte dos contratos e ausência de CLAUDE.md. Fez leitura/pesquisa estática de sessão, autorização, elegibilidade, eliminações e histórico; não validou comportamento runtime.
- Worker de contrato inicial reportou 30 esquemas, 15 casos de validação estática e 22 operações. São validações de documentos/esquemas, não testes funcionais da aplicação.
- Sem validador OpenAPI completo disponível; nenhuma dependência instalada. Verificações parciais não equivalem a validação integral OpenAPI.
- A tentativa da sessão principal de executar Python para igualdade recebeu pedido de confirmação, sem resultado: não conta como evidência própria de execução.

Os relatórios não certificam diff completo nem ausência de alterações prévias fora do âmbito, pois não há baseline Git. A intervenção registada usou apenas os ficheiros autorizados. Outras skills ainda contêm propostas antigas: não foram alteradas; decisões aprovadas têm precedência.

## Correções aplicadas após as revisões

Worker confirmou: CR-01/QA-01 corrigidos removendo `priority` da criação (sempre TODO/MEDIUM); CR-02 corrigido distinguindo autoria/participação inicial de papel/permissão pendentes; CR-03/QA-02 corrigidos como histórico completo sem paginação nem `page` obrigatório; SEC-03 corrigido com proposta de sessão anónima vinculada ao token, `Set-Cookie` HttpOnly e `Cache-Control: no-store`; QA-03 corrigido com `400`/BadRequest no GET de tarefa. Sessão principal removeu das decisões D6 uma exclusão de endpoints de gestão de membros não explicitamente aprovada: ausência de tais rotas nesta proposta não equivale a decisão de âmbito nova.

Validação reportada pelo worker após CR/SEC: 153 referências locais, 22 operationId únicos, 30 esquemas JSON Schema 2020-12, sem chaves YAML duplicadas; CreateTask rejeita prioridade/estado explícitos. Após QA-03, confirmou parsing, referências e igualdade das cópias. Hashes intermédios 720ee061... e db4cef57... foram ultrapassados pela limpeza final de D6; não são o hash da entrega final.

Re-revisão independente final do Code Reviewer concluída sobre o estado estabilizado: CR-01/02/03, SEC-03 e QA-03 confirmados corrigidos; histórico completo sem page confirmado. YAML sem chaves duplicadas; 154 referências locais resolvidas (a resposta 400 acrescentou uma referência), 30 esquemas JSON Schema 2020-12, 22 operationId únicos e CSRF nas operações mutáveis. Cinco casos de validação estática CreateTask passaram após correção em memória do contexto do resolvedor usado pelo revisor; a falha inicial era do script de verificação, não do contrato. Nenhum script foi gravado no projeto.

**Hash final dos dois contratos idênticos, 51 340 bytes:** `0662d7a64f958048a5ce11a1151b85c062e858bc088b6a8fe97affa5aca19d70`.

**Hash final das três skills idênticas, 6 430 bytes:** `2955103de4404f8be74e1039296385562552cc5fb598738e7f29be0633f1ff65`.

Code Reviewer recomenda aprovar as correções contratuais, condicionando fecho documental à consolidação das evidências e decisão humana sobre pendências. O revisor não conseguiu consultar o worker de contrato pelo runtime; esta sessão principal confirmou separadamente a conclusão no task_get/task_output. Isso não afeta a leitura independente do snapshot final. Este registo consolida o achado baixo sobre evidências ainda descritas como solicitadas: a tabela anterior preserva a cronologia; os resultados finais são os desta secção.

Não declarar SEC-01/SEC-02 resolvidos: continuam decisões humanas pendentes, não vulnerabilidades confirmadas em execução. QA e Security fizeram revisão inicial independente; a confirmação final das correções foi do Code Reviewer, não uma segunda revisão de todos os especialistas.

## Critérios para decisão humana

- Confirmar coerência D1–D10 entre requisitos, histórias, arquitetura, modelo, contrato e skills.
- Confirmar identidade das duas cópias OpenAPI e das três cópias da skill após alterações.
- Rever detalhes contratuais propostos não abrangidos pelas decisões; a matriz D3 completa e política D4 já estão aprovadas.
- Avaliar observações QA, Security e Code Reviewer e suas correções.
- Aprovação final humana da P1, sem autorização automática para P2 ou integração.
