# Changelog

## Unreleased — Matriz v1.3 working tree (2026-11-16)

`v1.3` is the project label, not a published application tag. Proposed SemVer candidate **0.1.0-rc.1** awaits owner approval; backend still declares `0.1.0-SNAPSHOT`, frontend `0.0.0`. No version/configuration files changed in the documentation audit.

### Implemented
- Spring Boot/React/PostgreSQL MVP: session authentication, project/task CRUD, state/priority, eligible multiple assignees, comments, search, filters and dashboard.
- Separate PostgreSQL Testcontainers integration and Java Playwright E2E Maven profiles; Node frontend tests.
- Multi-stage backend/frontend Dockerfiles, ignore files, persistent Compose storage and all-service healthchecks with initial startup gating; backend non-root runtime.
- GitHub Actions push/PR workflow: integration, separate browser E2E, frontend tests/build, then gated Docker builds; explicit read-only repository permission.
- Responsible-ID parsed numeric integrality/range validation and task-title/project-name Unicode code-point validation, with focused regressions.
- Assignment traceability, architecture diagram, setup/demo/test commands and bounded security review documentation.

### Executed verification
- Latest isolated verification after the description/comment Unicode and raw-decimal fixes (2026-11-16): **244 passes (199 backend + 45 frontend)**, zero failures/errors/skips; production build passed. PostgreSQL/browser suites were not rerun after these later fixes; no complete all-layer regression after them is claimed.
- Earlier full-regression baseline (2026-11-16), after the responsible-ID and title/name fixes but before the later fixes above: **252 distinct tests passed** (167 backend + 37 PostgreSQL + 5 Java Playwright + 43 frontend), zero failures/errors/skips; production build passed; every command exit 0. Overlapping runs and Maven `verify` repetitions are not additional unique coverage. See `docs/verification.md` for exact recorded commands/results/evidence.
- Prior isolated Docker/Compose build/startup/communication/persistence-after-recreation and healthy/unhealthy startup-gate checks passed; not rerun after these validation fixes. See `docs/ficha05-validation.md`.

### Pending / limitations
- Real GitHub Actions green run and controlled-failure/recovery evidence; identified secret-reviewed GitHub submission and clean-checkout verification.
- Human acceptance of candidate identity, remaining security/Unicode/deserialization limitations and sanitized evidence publication. Description/comment Unicode and raw-decimal fixes have focused and complete isolated backend/frontend evidence; PostgreSQL/browser coverage after those fixes is not certified.
- Public HTTPS/proxy/cookie verification and deployment hardening require a separately approved target. No production release, independent security audit or remote CI approval claimed.
