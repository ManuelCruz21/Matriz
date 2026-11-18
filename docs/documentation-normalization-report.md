# Documentation normalization report

## Public distribution notice — 2026-11-16

This report preserves historical local documentation work, not a public redistribution manifest. All `docs/aulas/` paths and course-source references below are local-only provenance: third-party PDFs, handouts, Markdown transcriptions and annexes remain unchanged locally and are excluded from public delivery. Historical source/index checks describe local availability, not public-checkout availability. Use [project requirements](requirements.md), [implementation decisions](implementation-plan.md) and [verification](verification.md) for self-contained public project documentation. No academic material was modified or removed, and no new execution or source-distribution permission is implied.

Date: 2026-11-12

## Current reading note — academic submission

The normalization records below are dated history, not current setup or Git-availability guidance. README corrections and the earlier write-permission blockers have since been resolved. Frontend tests now select all `src/*.test.js`; the latest isolated verification recorded **244 passes (199 backend + 45 frontend)** plus production build. The earlier **252-test full-regression baseline (167 backend + 37 PostgreSQL + 5 browser E2E + 43 frontend)** predates the later description/comment Unicode and raw-decimal fixes; PostgreSQL/browser suites were not rerun after those fixes. See [verification](verification.md) and the [public README](../README.md).

All teaching files, including Markdown, are now ignored under `/docs/aulas/**`, retained unchanged locally and excluded from public delivery. Historical source paths and eligibility checks below are local-only provenance, not public-checkout dependencies. No new source comparison, test execution or distribution permission is asserted here.

## Historical pre-commit audit follow-up — outcome at that time

**Partially completed.** The Assignment 03 canonical filename is now present, and all five assignments have `enunciado.md` reading copies. The earlier write-permission blocker for that file is resolved. Its 249 lines match the preserved Portuguese transcription exactly after newline normalization (source LF, tool-written copy CRLF): all seven page markers, nine exercise headings, tables as transcribed, deliverables and completion criteria remain. No missing requirements were reconstructed, and no new PDF-equivalence claim is made.

One write was denied in this follow-up: the README configuration correction. It was not retried or bypassed. README's Assignment 03 navigation was updated before that denial, but its configuration paragraphs remain stale. **Use these inspected facts rather than those paragraphs:**

- `frontend/package.json`: React/react-dom **19.3.1**, Vite **8.3.8**; `npm --prefix frontend test` still invokes only `node --test src/health.test.js`.
- `backend/pom.xml`: Java **21**, Spring Boot **3.5.16**.
- `compose.yaml`: image **`postgres:17.11-bookworm`**; required local variable **`DB_PASSWORD`**, mapped to the database container's `POSTGRES_PASSWORD` as well as the backend's `DB_PASSWORD`.
- Only nginx publishes a host port, **`127.0.0.1:8080:80`**. The backend/database have no published host ports; `frontend/nginx.conf` proxies `/api/` to `backend:8080`.
- `.env.example` is **untracked but not ignored** under the existing exception. Real `.env` files remain ignored. The optional seed defaults to disabled; enabling it requires both demo password variables (at least 12 characters each in the inspected seed). It does not reset existing accounts or delete data.

This follow-up changed only documentation and `.gitignore` comments. No ignore patterns changed relative to the start of this follow-up; existing exceptions for teaching Markdown and `.env.example` were retained. No originals were modified, removed, moved, force-added, staged or committed. Application/configuration files were inspected only. The sections below retain evidence from the previous normalization pass and are historical where explicitly labelled.

### Follow-up files changed

| File | Follow-up change |
| --- | --- |
| `README.md` | Resolved the Assignment 03 filename note; configuration correction denied and remains blocked. |
| `docs/aulas/ficha03/enunciado.md` | Created full Portuguese reading copy from the existing transcription; newline convention is the only byte-level difference. |
| `docs/aulas/README.md` | Indexed all five canonical statements; documented local-only source links and Git availability. |
| `docs/aulas/ficha01/README.md`, `docs/aulas/ficha02/README.md` | Qualified missing historical source folders and ignored annexes without deleting originals. |
| `AGENTS.md`, `docs/implementation-plan.md` | Removed obsolete current guidance about the Assignment 03 filename blocker; preserved decisions and approval history. |
| `.gitignore` | Removed the contradictory no-exceptions comment and clarified that non-Markdown teaching sources/examples remain ignored; rules unchanged. |
| `docs/documentation-normalization-report.md` | Recorded current status, denial, verification and unresolved sources. |

### Link review and unresolved sources

The local inline Markdown-link scan covered root README/AGENTS and Markdown under `docs/`, excluding fenced code. It checked filesystem targets, not heading anchors, external URLs or rendered Markdown. It found **nine unresolved link occurrences to five distinct teaching-example targets**, all in `docs/aulas/ficha02/materiais/05---adrs.md`: `./0000-use-architecture-decision-records.md`, `./0001-use-modular-monolith.md`, `./0002-use-postgresql.md`, `./0003-use-rest-between-clients-and-api.md` and `./decisions/`. These are preserved lecture examples, not missing approved project ADRs. No placeholder files were invented.

Six existing link occurrences point to ignored local PDFs: five in `docs/aulas/README.md` (Assignment 03/04/05 statements and Fullstack/DevOps slides), and the Assignment 05 statement's source link in `docs/aulas/ficha05/enunciado.md`. They work locally but cannot resolve from a clean checkout without a separately approved source-distribution decision. Other ignored local teaching sources include the two Assignment 04 QA PDFs, TXT annexes for Assignments 02/03/04, and Assignment 02's example YAML. The index lists these limitations. Original Assignment 01/02 PDFs and `originais/` folders, and Assignment 01's `anexos/` folder, remain absent. All existing originals are preserved; no Git tracking expansion is authorized or performed here.

### Follow-up verification and remaining blockers

- Compared Assignment 03 source/copy line content: identical after LF/CRLF normalization; seven page markers and nine actual exercise headings retained. This is transcription-copy verification, not a fresh PDF comparison.
- Inspected Compose, frontend manifest, backend POM, nginx configuration, sanitized environment template and optional seed source for the facts above. No application tests, builds, migrations, startup or CI were run.
- Checked ignore behavior: `.env.example` and teaching Markdown are eligible but untracked; PDFs and the example YAML remain ignored. Eligibility is not inclusion in a commit or clean checkout.
- Scanned changed files explicitly for trailing whitespace because `git diff --check` does not cover untracked Markdown. No findings in the initial follow-up scan.
- `git diff --check` passed for tracked changes, with an existing LF/CRLF warning for `frontend/src/main.jsx`. `git diff --cached --stat` showed no staged changes. Pre-existing code/configuration changes were not edited by this task.
- Initial read-only Python checks failed because the command tool uses PowerShell quoting rather than the advertised Bash syntax, then because piped non-ASCII source text was mangled. Checks were corrected using PowerShell-compatible stdin and ASCII glob patterns; no shell-based file writes were used.

**Remaining blockers:** README configuration corrections require write approval; ignored teaching-source links require an explicit distribution/tracking decision if clean-checkout access is desired; missing Assignment 01/02 originals and full Assignment 04 PDF equivalence remain unverified. Historical P1 statements and application/contract/security/acceptance gaps remain outside this bounded hygiene pass. No release-readiness claim is made.

## Previous normalization pass — historical scope and outcome

Executed the documentation-only instructions in `docs/openharness-documentation-task.md`. Application code, dependencies, API contracts, executable configuration, data, original teaching materials and Git history were not changed by this task. No files were moved, deleted, staged or committed.

**Partial completion:** Assignment 05 has a complete six-page text transcription, the teaching sources have a reading index, and README/AGENTS/implementation-plan guidance distinguishes current file presence from historical P1 records. The five canonical filenames cannot all be claimed: creation of `docs/aulas/ficha03/enunciado.md` was blocked twice by the write tool, including after explicit renewed user authorization. The preserved existing Assignment 03 transcription is the indexed fallback. No shell-based write workaround was used.

## Files created or updated

| File | Change |
| --- | --- |
| `docs/aulas/ficha05/enunciado.md` | Created Portuguese transcription of all six original PDF pages, with English provenance, page markers, exercises 01–06, mandatory checks, optional challenges, delivery and closing considerations. |
| `docs/aulas/README.md` | Created assignment/source index, supporting-material navigation, duplicate policy and explicit missing-source/canonical-file limitations. |
| `README.md` | Added English current-working-tree overview, source navigation, inspected configuration facts, unexecuted command references and historical-status qualification; preserved existing content. |
| `AGENTS.md` | Added current documentation-only scope and navigation; explicitly disallowed staging/commits and distinguished historical permissions from this task. Existing decisions and historical content remain intact. |
| `docs/implementation-plan.md` | Added source navigation and historical-status qualification without changing D1–D10 or inventing approvals. |
| `docs/documentation-normalization-report.md` | Created this report. |

Not created: `docs/aulas/ficha03/enunciado.md`, because write permission was denied. Its existing `10 - Ficha Prática 03.md` was read in full, including all nine exercises, deliverables and completion criteria, but was not modified.

## Source inventory and provenance

Assignments 01, 02 and 04 already had `enunciado.md` before this task; none was overwritten. Original Assignment 01 and 02 PDFs are not present in the current filesystem inventory, despite historical references in their folder guides. Their transcriptions remain usable, but completeness against the original PDFs is not independently verified. Do not silently reconstruct those sources.

Assignment 04's current PDF is readable at the compressed-stream level: all 20 inspected Flate streams decompressed successfully. Its SHA-256 is `c5413e706c5e0d6f6a991cee092ae2b73715aadac6cabc1d2a7c1ff7b2fea86a`. This is not the corrupted binary described in the earlier audit. The current Markdown was read, but a full PDF-to-Markdown equivalence check was not performed, so complete source equivalence is not certified.

Assignment 05 source SHA-256: `bee1ed3bc7c59cfeb2680f6e5ce9d56a280768237834394f0d253a794ee37620`. The six page objects, their content streams, font references and ToUnicode range maps were decoded in memory using Python standard-library `re` and `zlib`. Both literal and hexadecimal text operands were decoded, grouped by line coordinates and inspected in reading order. There were no unmapped text glyphs in those six page content streams. This matters because literal-only extraction omits list text encoded through font maps. Repeated running headers/footers were consolidated; the stray final backslash in the optional-challenges heading was documented as omitted layout noise. No rendered-page visual comparison was performed.

No PDF extraction packages were installed. `pypdf`, `fitz`, `pdfplumber`, `pdftotext` and `mutool` were unavailable in the inspected environment.

## Duplicate comparison

The two Assignment 03 Fullstack presentations were read and compared before designating a canonical copy:

- `08 - Desenvolvimento Fullstack.md`: canonical, because it matches the original PDF basename.
- `08---desenvolvimento-fullstack.md`: preserved alternate export, not a second independently maintained authority.

The files are not byte-identical. Removing heading lines, provenance blockquotes and whitespace gives identical body text: 13,660 characters in each. That comparison establishes body equivalence under the stated normalization, not equivalence of visual structure or provenance metadata. Neither file was moved or deleted.

The inventory also contains TXT/Markdown teaching-example pairs under Assignment 02 and Assignment 03. These are retained source/export pairs; they were not removed or declared byte-equivalent without a separate comparison. Required OpenAPI copies remain outside this task and were not edited.

## Verification performed

- Read the task, AGENTS, README, implementation plan, assignment statements and relevant folder guides; inventoried `docs/aulas` and searched Markdown references.
- Inspected backend/frontend manifests and Compose only to ground the new README facts. Did not change them.
- Compared Fullstack bodies as described above.
- Decoded Assignment 05 PDF text including Unicode font maps and all six page streams; inspected objectives, prerequisites, six exercises, mandatory requirements, optional challenges and delivery content against the new Markdown.
- Checked local Markdown link targets in the affected navigation files. The first check exposed missing original sources and incorrect presumed presentation filenames; the new index was corrected to match the actual inventory. Final link and whitespace results are reported in the task response.
- Final local-link check: 47 local Markdown targets checked in the six changed files; zero missing targets. Assignment 05 contains six page markers and six exercise headings.
- `git diff --check` reported no whitespace errors in tracked changes; it emitted an existing frontend LF/CRLF warning. New/ignored Markdown is not covered by that Git check.
- Checked Git status and diff for task boundaries. Pre-existing modified/untracked application and delivery files are not changes made by this documentation task. `git diff --cached --stat` returned no staged changes.
- `git check-ignore` confirms that `docs/aulas/README.md` and `docs/aulas/ficha05/enunciado.md` are ignored by the existing repository policy. They exist locally but will not appear in ordinary untracked status or a clean checkout. Ignore rules were not changed, and no force-add/staging was performed.

No application tests, builds, dependency installation, migrations, Docker commands, browser checks or GitHub Actions runs were executed. No new passing-test or release-readiness claim is made.

## Remaining issues and recommended next steps

1. Resolve the tool-permission blocker for `docs/aulas/ficha03/enunciado.md`, then create its canonical reading copy from the existing transcription with complete tables, all nine exercises, deliverables and criteria. Update the index and the temporary blocker notes together.
2. Obtain missing original Assignment 01/02 sources and verify their existing transcriptions. Preserve existing teaching material; do not overwrite unique content.
3. Compare Assignment 04 Markdown fully against the current readable PDF before claiming complete original-source coverage.
4. Historical folder guides refer to unavailable PDFs/example locations. They are preserved; a later authorized documentation pass should qualify those references without inventing or deleting original content.
5. Historical Portuguese project documents still contain obsolete P1/P2 statements. The new English guidance qualifies them but does not rewrite approval history or claim to resolve every contradiction. Broader translation and technical-document synchronization require a bounded follow-up.
6. Application defects, test-coverage gaps, Git inclusion and delivery verification from the audit remain outside this documentation-only task. Recheck any audit finding against the current files before remediation; the teaching-source inventory has changed since that audit.
