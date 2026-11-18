# Ficha 05 — plano de entrega

> Historical preparation plan, not current setup instructions or a complete publication manifest. The short file list and pending-execution statements below describe preparation at that time. Use the [README](../README.md), [verification evidence](verification.md) and [final checklist](final-review.md) for current guidance. The path-level candidate manifest remains a proposal pending approval; no approved manifest or publication is claimed.
>
> Latest isolated post-fix verification: **244 passes (199 backend + 45 frontend)** plus production build. Earlier full-regression baseline: **252 tests (167 backend + 37 PostgreSQL + 5 browser E2E + 43 frontend)**, before the later description/comment Unicode and raw-decimal fixes; no PostgreSQL/browser rerun after those fixes is claimed. Dated Compose runtime evidence is in [delivery validation](ficha05-validation.md). Historical pending-test, outdated-README and unverified-persistence statements below are superseded within those recorded scopes.

## Componentes reais
- Backend: Java 21, Spring Boot 3.5.16, Maven, API HTTP na porta 8080, Flyway e PostgreSQL 17.
- Frontend: React 19.3.1, Vite 8.3.8, servido por Nginx no Docker.
- Autenticação: sessão HTTP, cookie e CSRF. Frontend e API são apresentados pela mesma origem através do proxy `/api/`.
- Persistência: volume `postgres_data` do Docker Compose.

## Ficheiros preparados
- `backend/Dockerfile`, `backend/.dockerignore`
- `frontend/Dockerfile`, `frontend/.dockerignore`, `frontend/nginx.conf`
- `compose.yaml`, `.env.example`
- `.github/workflows/ci.yml`
- `docs/ficha05-delivery-plan.md`, `docs/ficha05-validation.md`, `docs/release-notes-0.1.0-rc.1.md`, `CHANGELOG.md`
- Bloco de instruções adicional em `docs/ficha05-readme-addendum.md` para integrar no README existente após revisão.

## Decisões
1. Docker Compose com imagens compiladas (não servidores de desenvolvimento com volumes de código).
2. Frontend e API na mesma origem; Nginx encaminha `/api/` para `backend:8080`.
3. PostgreSQL isolado sem porta publicada; volume persistente.
4. CI com testes Maven, testes/build frontend e imagens Docker após sucesso das verificações.
5. Não se cria deploy público automático: Render/Neon são uma recomendação opcional e exigem configuração de HTTPS, base de dados e gestão de segredos.

## Riscos / revisão humana
- Confirmar regras de sessão e `Secure` antes de disponibilizar por HTTPS; Nginx incluído serve HTTP **apenas local**.
- Verificar que o `compose.yaml` atual pode ser substituído: o anterior executava Maven e Vite em modo de desenvolvimento.
- Testes da Ficha 04, integração e E2E ainda carecem de execução real.
- Atualizar README/AGENTS.md, que ainda contêm informação histórica desatualizada, após revisão do código.
- Confirmar que a aplicação inicia e os dados persistem após recriar containers.
