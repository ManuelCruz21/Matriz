# Matriz — Plano de implementação

## Current reconciliation — 2026-11-16

D1–D10 remain approved domain decisions; P1/P2 work/authorization statements below are historical, not current implementation status or permission for new actions. Current functional sources/tests and delivery configuration exist. Post-fix local regression passed **252 distinct tests** (167 backend + 37 PostgreSQL + 5 Java Playwright + 43 frontend), zero failures/errors/skips, production build passed. See [verification](verification.md) for commands/evidence and [final review](final-review.md) for exact Ficha 03–05 traceability. No suites or executable changes in this documentation audit.

The architecture Mermaid, all-service healthchecks/startup-gate runtime evidence, unit/integration/browser coverage and scoped code/security review are present, not pending implementation. Remaining mandatory closure: remote Actions/controlled-failure recovery, human disposition of remaining validation limitations, final secret-reviewed complete Git snapshot/clean checkout and GitHub submission. Candidate identity remains proposed; no staging, commit or push authorized.

## Historical documentation navigation update — 2026-11-12

Use the public [project requirements](requirements.md) and [verification evidence](verification.md) for delivery scope and acceptance results. Third-party course materials are retained unchanged locally and excluded from public distribution. All course paths and transcription references below are local-only historical provenance, not public navigation or packaging dependencies. P1/P2 status entries below are historical records, not a current implementation inventory. Backend, frontend, tests and delivery files exist, but file presence does not establish completeness, runtime correctness, implementation approval or successful acceptance checks.

D1–D10 remain the recorded domain decisions. The [normalization report](documentation-normalization-report.md) records this documentation-only update and remaining contradictions. Application fixes and executable configuration changes are outside scope. Assignment 03 is indexed through `ficha03/enunciado.md`, an unchanged copy of its preserved Portuguese transcription; the earlier write-permission blocker is resolved. Historical Git preparation authorization below does not authorize staging or commits in this task.

## Estado e portão — historical record

P1 documental formalmente aprovada e concluída pelo utilizador em 2026-11-11, com as limitações do relatório final aceites. Este registo de fecho prevalece sobre referências anteriores a aprovação pendente neste plano e nos restantes artefactos P1, preservados como histórico da entrega. D1–D10 e pontos 1–7 mantidos. P2 **não autorizada**. Este plano não demonstra implementação nem execução de testes.

Autorização posterior e separada: preparar Git local apenas nesta pasta, criar .gitignore, verificar candidatos e preparar staging explícito. Sem commit, remoto, push ou P2. Materiais de aulas, protótipo executável e skills não revistas ficam fora da baseline preparada.

Fontes: decisões D1–D10 aprovadas pelo utilizador nesta sessão; Ficha Prática Laboratorial 03, transcrição em `docs/aulas/ficha03/10 - Ficha Prática 03.md` (PDF original na mesma pasta, sem versão/data editorial confirmada); Specification Pack e Architecture Pack anteriores. As decisões abaixo substituem propostas anteriores incompatíveis, não o registo histórico das Fichas 01/02.

## Registo de decisões aprovadas

| ID | Decisão | Rastreabilidade |
| --- | --- | --- |
| D1 | MVP completo da Ficha 03: autenticação, CRUD de projetos/tarefas, prioridades, múltiplos responsáveis, comentários, pesquisa, filtros e painel. Registo público e recuperação de palavra-passe continuam opcionais, não incluídos automaticamente. | RF01–RF12; US01–US12 |
| D2 | Java, Spring Boot, React, PostgreSQL; backend monólito modular. Complemento aprovado: Java 21 LTS, Spring Boot 3.5.x, React 19.x e PostgreSQL 17.x; versões exatas a fixar futuramente após verificar suporte/compatibilidade, sem substituições não aprovadas. | architecture.md |
| D3 | Matriz aprovada: gestor edita/elimina projeto, cria/edita/altera estado de qualquer tarefa do seu projeto, elimina tarefas e gere responsáveis. Membro cria tarefas; só edita/altera estado das atribuídas a si. Ambos consultam e comentam. Backend: 401 sem autenticação; 403 sem permissão; 404 recurso inexistente ou inacessível. | RF01–RF12; constraints.md |
| D4 | Sessões Spring Security; inatividade máxima 30 minutos e duração absoluta máxima 8 horas; HttpOnly, SameSite=Lax, Secure em HTTPS; CSRF inclusive login/logout; rotação da sessão no login e invalidação no logout. Não JWT/Bearer. | RF08; US08; OpenAPI |
| D5 | Zero ou mais responsáveis elegíveis por tarefa. | RF04; US02/US04 |
| D6 | Participação direta nos projetos e seed. Elegibilidade baseada na participação direta, sem exigir equipa. Não implica CRUD de membros na UI. | RF04; database.md |
| D7 | Eliminação definitiva e transacional, sem dependentes órfãos. | RF09/RF10; US09/US10 |
| D8 | Projeto: nome 3–100, descrição até 1000; tarefa: título 3–150, descrição até 2000; comentário 1–2000. Remover espaços exteriores e rejeitar texto vazio. Estados TODO/IN_PROGRESS/DONE; prioridades LOW/MEDIUM/HIGH; nova tarefa TODO/MEDIUM. Descrições mantêm-se opcionais: ausência permitida, texto fornecido vazio rejeitado. | RF01–RF05/RF11; OpenAPI |
| D9 | Painel global dos projetos acessíveis. Contagens do projeto respeitam prioridade, ignoram estado; total da listagem respeita ambos. Pesquisa de projetos por nome sem distinguir maiúsculas. Ordenação estável; contagens independentes da paginação. | RF06/RF07/RF12; US06/US07/US12 |
| D10 | Histórico só de alterações efetivas de estado, prioridade e responsáveis, com utilizador, data UTC, valor anterior e novo. Apagado com tarefa/projeto. openapi.yaml canónico; docs/matriz-openapi.yaml cópia sincronizada. | RF05/RF04; database.md; OpenAPI |

### Criação de projetos — complemento aprovado de D3/D6

Quem cria um projeto fica automaticamente associado como gestor. Projeto e participação são criados na mesma transação; projeto começa privado e sem outros membros. A identidade vem da sessão, não de um gestor escolhido no pedido. Falha de qualquer parte reverte toda a criação.

D8: a remoção de espaços precede a avaliação de comprimento. Não converter ausência de descrição em requisito obrigatório. Limpeza e representação de nulidade já aprovadas no ponto 5 abaixo; não as manter como propostas pendentes.

## Registo de complementos aprovados — pontos 1–7

Este registo substitui as pendências anteriores sobre versões de referência, topologia/origens, ordenação, pesquisa, nulidade, CreateTask e convenções de sessão/CSRF. Mantém D1–D10, a matriz D3 e toda a política D4; não fecha P1 nem autoriza P2.

| Ponto | Complemento aprovado | Rastreabilidade |
| --- | --- | --- |
| 1 | Java 21 LTS, Spring Boot 3.5.x, React 19.x e PostgreSQL 17.x. Verificar suporte e compatibilidade antes da implementação e fixar as versões exatas nessa fase futura; qualquer substituição requer aprovação humana. Suporte e compatibilidade não foram verificados nesta atualização documental. | D2; arquitetura; ambiente futuro I1 |
| 2 | Mesma origem frontend/API; HTTPS no ambiente publicado; proxy em desenvolvimento; não permitir origens arbitrárias com credenciais. | D4; arquitetura; US08 |
| 3 | Projetos: `id DESC`; tarefas: `createdAt DESC, id DESC`; comentários e histórico: data UTC `ASC, id ASC`; responsáveis elegíveis: nome sem distinguir maiúsculas/minúsculas `ASC, id ASC`. | D9/D10; RF01/RF06/RF11/RF12; US01/US04/US06/US11/US12 |
| 4 | Pesquisa por substring literal no nome, com trim, sem distinguir maiúsculas/minúsculas (case-insensitive), mas distinguindo acentos (accent-sensitive). Valor omitido ou vazio após trim não aplica filtro; `%` e `_` são caracteres literais, não wildcards. | D9; RF12/US12 |
| 5 | Descrições de projetos e tarefas: na criação, campo omitido ou `null` significa ausência; em PATCH, omitido mantém o valor e `null` remove-o. Texto é sujeito a trim e rejeitado se ficar vazio, respeitando os limites D8. A API devolve `null` quando a descrição está ausente. | D8; RF01/RF02/RF03/RF09; US01/US02/US03/US09 |
| 6 | Em CreateTask, membro pode omitir responsáveis ou enviar `[]`; uma lista não vazia devolve 403 sem criação parcial, incluindo tentativa de autoatribuição. Gestor pode indicar responsáveis elegíveis por participação direta no projeto. | D3/D5/D6; RF02/RF04; US02/US04 |
| 7 | Listas completas, sem paginação no MVP. Bootstrap CSRF e convenções `JSESSIONID` e `X-CSRF-TOKEN` aprovados sob condição de coerência com as decisões D3/D4, incluindo CSRF login/logout e política completa de sessão/cookies. | D4/D9; US06/US08/US12 |

Não se verificou suporte na web nem se fixaram versões exatas ou configurações executáveis nesta atualização. A revisão da coerência contratual permanece trabalho da sessão principal/outro worker.

## P1 autorizada — tarefas documentais

| Tarefa | Responsável real/proposto | Entradas → saídas | Dependências e aceitação |
| --- | --- | --- | --- |
| P1-01 | Orchestrator/Documentation, sessão principal | D1–D10 + Fichas → requisitos, histórias, restrições e decisões | Documentar aprovado vs proposto vs pendente; preservar contexto histórico |
| P1-02 | Worker real de contrato | D1–D10 + contrato anterior → dois OpenAPI | P1-01; cópias idênticas, sem JWT nem responsável singular obrigatório |
| P1-03 | Sessão principal | Decisões → arquitetura, modelo, README e skill | Sem implementação; três cópias da skill coerentes |
| P1-04 | QA, Security, Code Reviewer em revisões independentes | Diff e fontes → observações com gravidade e evidências | Só leitura; sem testes funcionais simulados; resultados em verification.md |
| P1-05 | Orchestrator + utilizador | Diffs, revisões, limitações → decisão final | Corrigir apenas problemas documentais autorizados; aguardar aprovação final da P1 |

Ficheiros permitidos (16 no projeto): README.md; AGENTS.md; docs/requirements.md; docs/user-stories.md; docs/constraints.md; docs/architecture.md; docs/database.md; docs/review.md; docs/agent-workflow.md; docs/implementation-plan.md; docs/verification.md; docs/ui/README.md; openapi.yaml; docs/matriz-openapi.yaml; .openharness/skills/matriz-orchestrator/SKILL.md; .agents/skills/matriz-orchestrator/SKILL.md. Adicional global autorizado: `~/.openharness/skills/matriz-orchestrator/SKILL.md` (perfil local do utilizador), exclusivamente sincronização. Nenhuma outra skill ou ficheiro global.

### Distribuição real desta atualização documental

O worker de documentação alterou apenas os dez Markdown funcionais/técnicos; o worker de contrato ficou responsável pelos dois OpenAPI; a sessão principal atualizou review, verification e as três cópias autorizadas da skill. Esta distribuição evita escritas simultâneas e não reduz nem alarga a lista de ficheiros autorizados. Sem código, instalações, migrations, dados, configurações executáveis, mutações Git ou commits.

## Sequência futura proposta — não iniciada, não autorizada (P2)

| Incremento proposto | Especializações | Componentes e entidades | Dependências | Critérios e verificações futuras (não executadas) |
| --- | --- | --- | --- | --- |
| I1 Ambiente e persistência | Backend/DevOps | Módulos Java, React, PostgreSQL; utilizador/projeto/participação | Aprovação P1 e autorização separada P2; verificar suporte/compatibilidade e fixar versões exatas da stack aprovada; D3 vigente | Arranque reproduzível, seed fictício de pelo menos dois utilizadores e acessos distintos; não apagar dados a cada arranque |
| I2 Sessão e projetos | Backend/Frontend/Security | Sessões, CSRF, CRUD projetos, login/listagem/detalhe | I1; política de autorização | Login válido/inválido, logout, acesso direto cruzado, validações de projeto, persistência após reinício |
| I3 Tarefas e responsáveis | Backend/Frontend | Tarefa, atribuição N:M, prioridade, histórico | I2; regras de autorização | TODO/MEDIUM iniciais; zero/vários responsáveis; elegibilidade; alterações efetivas auditadas; no-op sem histórico |
| I4 Comentários e consultas | Backend/Frontend/QA | Comentários, pesquisa, filtros, painel | I3; contrato aprovado | Limites D8; contagens D9; lista vazia; ordenações e pesquisa aprovadas; listas completas sem paginação; contagens sobre todo o universo autorizado |
| I5 Eliminações e validação transversal | Backend/QA/Security/Reviewer | Cascatas transacionais, todos os ecrãs/API | I2–I4 | Rollback em falha; sem órfãos; eliminação de histórico; testes de autorização/CSRF; revisão independente |

Os nomes de pastas de código, versões exatas de dependências, comandos de instalação e implementação não são definidos nem executados nesta P1. As famílias de versões do ponto 1 já estão aprovadas; suporte/compatibilidade continuam por verificar antes da implementação. QA e Security podem rever documentos em paralelo; ninguém altera simultaneamente os ficheiros do contrato.

## Pendências e critérios de saída

- D3/D4 e criação de projetos já aprovadas: não as voltar a apresentar como pendentes. Criador associado automaticamente como gestor, na mesma transação; projeto privado e sem outros membros. Para autorização: 404 se recurso inexistente/inacessível; 403 se recurso acessível mas operação não permitida; 401 sem autenticação.
- Rever coerência e detalhes contratuais ainda não cobertos pelas decisões (por exemplo, rotas/payloads remanescentes). Pontos 1–7, incluindo ordenação, descrições, CreateTask, ausência de paginação e convenções CSRF/sessão, já aprovados; não reabrir como pendências. OpenAPI a cargo da sessão principal/outro worker.
- Antes da implementação autorizada: verificar suporte e compatibilidade de Java 21 LTS/Spring Boot 3.5.x/React 19.x/PostgreSQL 17.x e fixar versões exatas. Se for necessária substituição, pedir aprovação humana. Mesma origem/HTTPS publicado/proxy de desenvolvimento e D4 já aprovados; não há suporte verificado nem configuração executável nesta P1.
- Rever resultados independentes e limitações em `verification.md`.
- Aprovação final humana da P1. Essa aprovação não autoriza P2, instalações, migrations, commits, merges, push ou deploy.
