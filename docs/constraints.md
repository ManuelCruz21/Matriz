# Restrições, pressupostos e decisões pendentes — Matriz

## P1 — limites vigentes e decisões aprovadas

Prevalecem D1–D10 em [implementation-plan.md](implementation-plan.md) sobre propostas históricas incompatíveis. MVP completo Ficha 03; Java/Spring Boot/React/PostgreSQL, monólito modular; papéis gestor/membro por projeto; sessão Spring Security, HttpOnly, CSRF; zero ou mais responsáveis elegíveis por participação direta; seed; eliminação definitiva transacional; validações D8, contagens D9 e histórico limitado D10.

**Autorização desta atualização:** apenas os 10 Markdown enumerados em AGENTS.md e no plano; OpenAPI, skills, review e verification reservados à sessão principal/outro worker, sem alterações por esta tarefa. Não alterar outras skills, código, protótipo HTML/CSS/JS, dependências, configurações executáveis ou dados. Não instalar, executar migrations, commits, merges, push ou deploy. Não iniciar P2. Aprovação das decisões e autorização de edição não equivalem a aprovação final da P1.

**Decisões já aprovadas:** matriz D3 completa e códigos 401/403/404 conforme implementation-plan.md; criação privada com criador automaticamente gestor na mesma transação; D4 com 30 minutos de inatividade, máximo absoluto 8 horas, HttpOnly, SameSite=Lax, Secure em HTTPS, CSRF login/logout, rotação no login e invalidação no logout.

**Complementos aprovados:** Java 21 LTS, Spring Boot 3.5.x, React 19.x e PostgreSQL 17.x. Verificar suporte e compatibilidade antes da implementação e fixar as versões exatas nessa fase futura; qualquer substituição requer aprovação humana. Suporte e compatibilidade não foram verificados nesta atualização documental. Frontend e API na mesma origem, com HTTPS no ambiente publicado e proxy em desenvolvimento; não permitir origens arbitrárias com credenciais. Bootstrap CSRF e convenções `JSESSIONID` (cookie de sessão) e `X-CSRF-TOKEN` (header) aprovados, condicionados à coerência com D3/D4: CSRF inclusive login/logout, HttpOnly no cookie de sessão, SameSite=Lax, Secure em HTTPS, rotação no login e invalidação no logout. Não constituem configuração executável nem prova de implementação.

Listas completas sem paginação no MVP. Projetos: `id DESC`; tarefas: `createdAt DESC, id DESC`; comentários e histórico: data UTC `ASC, id ASC`; responsáveis elegíveis: nome sem distinguir maiúsculas/minúsculas `ASC, id ASC`. Pesquisa por substring literal no nome, com trim, sem distinguir maiúsculas/minúsculas (case-insensitive), mas distinguindo acentos (accent-sensitive). Valor omitido ou vazio após trim não aplica filtro; `%` e `_` são caracteres literais, não wildcards.

Descrições de projetos e tarefas: na criação, campo omitido ou `null` significa ausência; em PATCH, omitido mantém o valor e `null` remove-o. Texto é sujeito a trim e rejeitado se ficar vazio, respeitando os limites D8. A API devolve `null` quando a descrição está ausente. Em CreateTask, membro pode omitir responsáveis ou enviar `[]`; uma lista não vazia devolve 403 sem criação parcial, incluindo tentativa de autoatribuição. Gestor pode indicar responsáveis elegíveis por participação direta no projeto.

**Pendências reais:** verificar suporte/compatibilidade e fixar versões exatas antes da implementação futura; revisão de coerência e detalhes contratuais não cobertos pelas decisões; aprovação final humana da P1. Não reabrir os pontos 1–7 aprovados como pendências. Política de retenção: histórico é eliminado com tarefa/projeto, não é trilho imutável. Não inventar poderes específicos de gestores/membros. Seed não é autorização para o executar nesta fase.

**Evidências:** distinguir análise estática, execução de testes e aprovação; ver verification.md. Não declarar API/React/persistência implementados por atualizar documentação. CLAUDE.md não encontrado no levantamento inicial.

## Contexto histórico — Ficha 01

As restrições de aula e pendências abaixo descrevem o momento original; não anulam as decisões posteriores acima.

> **Estado:** proposta de especificação para revisão humana. Apenas a descrição inicial do cliente e as instruções da ficha são factos confirmados. Os restantes elementos não constituem decisões aprovadas.

## 1. Restrições conhecidas (ficha prática)

- Nesta primeira aula não deve ser implementado código.
- A escolha de linguagem, framework, base de dados e arquitetura fica para a aula seguinte.
- A documentação deve ser coerente, versionada em Git e reutilizável nas próximas fases.
- O projeto deve manter os documentos `docs/requirements.md`, `docs/user-stories.md` e `docs/constraints.md`, além de `README.md` e `AGENTS.md` na raiz.

## 2. Factos confirmados pelo cliente

- A aplicação chama-se **Matriz**.
- Destina-se a permitir que equipas organizem projetos, giram tarefas e acompanhem o progresso do trabalho.
- O cliente **não** especificou funcionalidades detalhadas, regras de negócio, requisitos técnicos nem prioridades de desenvolvimento.

## 3. Pressupostos de trabalho — NÃO confirmados pelo cliente

Os pontos seguintes orientam a proposta inicial do MVP e exigem validação antes de serem tratados como requisitos definitivos:

- Existem membros das equipas e responsáveis por projetos; os papéis e permissões ainda não estão definidos.
- **Propõe-se que cada tarefa pertença obrigatoriamente a um único projeto.** A criação de tarefas sem projeto e a transferência de tarefas entre projetos ficam por esclarecer.
- Propõe-se que cada tarefa tenha título, descrição opcional e estado; limites, obrigatoriedade de outros campos e validações estão pendentes.
- Os estados inicialmente sugeridos são **Por fazer**, **Em curso** e **Concluída**; transições e possibilidade de personalização não estão aprovadas.
- Propõe-se que tarefas possam ser atribuídas a membros do respetivo projeto; a existência de tarefas sem responsável e as regras de atribuição estão pendentes.
- O MVP proposto abrange **RF01–RF06** em `docs/requirements.md`, refletidos em **US01–US06** de `docs/user-stories.md`. Esta correspondência deve ser confirmada após qualquer alteração nesses ficheiros.
- Pressupõe-se que serão necessários mecanismos de autenticação e autorização, sem decisão sobre a solução concreta.
- A forma de calcular e apresentar o progresso dos projetos ainda não foi acordada.

## 4. Segurança e privacidade — requisitos propostos para validação

- Restringir o acesso a projetos e tarefas conforme as permissões a definir.
- Aplicar o princípio do menor privilégio.
- Proteger credenciais e dados pessoais, sem os expor em código, logs ou documentação.
- Validar entradas e evitar acesso ou divulgação indevida de informação.
- Definir políticas de retenção, eliminação de dados e responsabilidades de tratamento antes da implementação.
- Confirmar quais os dados pessoais estritamente necessários e as obrigações de privacidade aplicáveis.

**Nota:** são preocupações de segurança identificadas na análise, não políticas já aprovadas pelo cliente.

## 5. Decisões técnicas pendentes

- Linguagem e framework(s).
- Arquitetura e organização dos componentes.
- Modelo de dados e mecanismo de persistência.
- Alojamento e ambientes de execução.
- Autenticação, autorização e modelo de permissões.
- Tipo de interface e dispositivos suportados.
- Estratégia de testes e critérios não funcionais mensuráveis.

Não selecionar tecnologias nesta fase.

## 6. Limitações de âmbito

- **Nesta aula:** análise, especificação e preparação do contexto; sem implementação.
- **MVP proposto:** apenas RF01–RF06, sujeito a validação.
- **Fora da proposta inicial:** integrações externas, notificações, automações, relatórios avançados e outras funcionalidades futuras, salvo decisão humana em contrário.
- A gestão de membros e o mecanismo de acesso são dependências a clarificar; a sua menção não significa que estejam completamente especificados como funcionalidades do MVP.

## 7. Revisão crítica — Exercício 6 (provisória)

| Área | Observação | Ação proposta |
| --- | --- | --- |
| Ambiguidades | «Progresso», papéis e pertença a projetos não têm regras completas. | Pedir definição de métricas, papéis e regras de acesso. |
| Funcionalidades não justificadas | RF07 e outras funções futuras não estão confirmadas. | Mantê-las fora do MVP até validação. |
| Pressupostos vs. factos | Estados, responsáveis e regras de atribuição são propostas. | Preservar a identificação explícita de pressupostos. |
| Testabilidade | US01–US06 possuem critérios observáveis, mas faltam limites de campos e regras detalhadas. | Completar casos-limite e validações quando houver decisões. |
| Segurança e privacidade | Faltam decisões sobre identidade, permissões e ciclo de vida dos dados. | Validar requisitos antes da implementação. |
| Coerência documental | Correspondência proposta RF01–RF06 ↔ US01–US06; gestão de membros por esclarecer. | Rever os três documentos em conjunto após cada decisão. |

## 8. Decisões humanas pendentes

Antes de considerar a especificação aprovada, uma pessoa responsável deve **aceitar, alterar ou rejeitar** as propostas relativas a:

1. Âmbito e prioridades do MVP (RF01–RF06).
2. Papéis de utilizador e gestão de membros.
3. Associação obrigatória de cada tarefa a um projeto e regras de atribuição.
4. Campos, estados e transições das tarefas.
5. Definição e cálculo de progresso.
6. Autenticação, permissões, segurança e privacidade.
7. Exclusões e funcionalidades futuras.

**Regra Human-in-the-Loop:** a análise e as recomendações geradas por IA não equivalem à aprovação do cliente ou da equipa. Registar as decisões tomadas e atualizar `requirements.md`, `user-stories.md` e este ficheiro de forma consistente.
