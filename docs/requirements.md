# Requisitos — Matriz

## P1 — especificação atual aprovada (D1–D10)

Esta secção prevalece sobre a proposta histórica das secções seguintes. Decisões aprovadas pelo utilizador, não implementação: ver registo completo em [implementation-plan.md](implementation-plan.md). A aprovação final da entrega documental P1 continua pendente.

| ID atual | Requisito do MVP aprovado | User Stories |
| --- | --- | --- |
| RF01 | Criar, listar e consultar projetos acessíveis | US01 |
| RF02 | Criar e consultar tarefas de um projeto | US02 |
| RF03 | Editar título, descrição e prioridade de tarefas | US03 |
| RF04 | Associar/remover zero ou mais responsáveis elegíveis, participantes diretos do projeto | US04 |
| RF05 | Alterar estado; histórico limitado a alterações efetivas de estado, prioridade e responsáveis | US05 |
| RF06 | Listar tarefas com filtros de estado e prioridade; total respeita ambos | US06 |
| RF07 | Contagens por estado do projeto respeitam prioridade e ignoram estado; listas completas sem paginação no MVP | US07 |
| RF08 | Iniciar/terminar sessão e consultar perfil, com sessão Spring Security, cookie HttpOnly e CSRF | US08 |
| RF09 | Editar e eliminar projetos; eliminação definitiva/transacional dos dependentes | US09 |
| RF10 | Eliminar tarefas e respetivos dependentes definitivamente numa transação | US10 |
| RF11 | Adicionar/listar comentários com autor e data | US11 |
| RF12 | Pesquisar projetos por nome sem distinguir maiúsculas; painel global dos projetos acessíveis | US12 |

Todos RF01–RF12 integram o MVP (D1). D2, com o complemento aprovado, define Java 21 LTS, Spring Boot 3.5.x, React 19.x e PostgreSQL 17.x e monólito modular. Verificar suporte e compatibilidade antes da implementação e fixar as versões exatas nessa fase futura; qualquer substituição requer aprovação humana. Suporte e compatibilidade não foram verificados nesta atualização documental. D3 aprova: gestor edita/elimina projeto, cria/edita/altera estado de qualquer tarefa do projeto, elimina tarefas e gere responsáveis; membro cria tarefas e só edita/altera estado das atribuídas a si; ambos consultam e comentam. 401 sem autenticação, 403 sem permissão, 404 recurso inexistente ou inacessível. Criador fica automaticamente gestor na mesma transação de criação, projeto privado sem outros membros. D6 aprova participação direta e seed, sem dependência de equipas. Autoria e identidade devem vir da sessão, não de campos fornecidos pelo cliente.

**D8:** remover espaços exteriores antes de validar: nome projeto 3–100; título tarefa 3–150; descrição projeto até 1000 e tarefa até 2000 (opcionais; quando fornecidas como texto não podem ficar vazias); comentário 1–2000. Estados `TODO`, `IN_PROGRESS`, `DONE`; prioridades `LOW`, `MEDIUM`, `HIGH`; criação em `TODO`/`MEDIUM`.

**D9, com complementos aprovados:** listas completas sem paginação no MVP. Projetos: `id DESC`; tarefas: `createdAt DESC, id DESC`; comentários e histórico: data UTC `ASC, id ASC`; responsáveis elegíveis: nome sem distinguir maiúsculas/minúsculas `ASC, id ASC`. Pesquisa por substring literal no nome, com trim, sem distinguir maiúsculas/minúsculas (case-insensitive), mas distinguindo acentos (accent-sensitive). Valor omitido ou vazio após trim não aplica filtro; `%` e `_` são caracteres literais, não wildcards. O painel abrange apenas projetos acessíveis. A filtragem de tarefas não modifica esse universo global. Contagens por projeto ignoram estado mas respeitam prioridade; total da listagem respeita ambos os filtros.

**D10:** histórico contém autor, instante UTC, anterior e novo; não regista edições de texto, criação, comentários ou operações sem mudança efetiva. É eliminado com tarefa/projeto. OpenAPI canónico na raiz e cópia sincronizada em docs.

**D4 aprovada:** inatividade máxima 30 minutos; duração absoluta máxima 8 horas; cookie HttpOnly, SameSite=Lax, Secure em HTTPS; CSRF incluindo login/logout; rotação da sessão no login e invalidação no logout.

**Complementos D8 e D3:** Descrições de projetos e tarefas: na criação, campo omitido ou `null` significa ausência; em PATCH, omitido mantém o valor e `null` remove-o. Texto é sujeito a trim e rejeitado se ficar vazio, respeitando os limites D8. A API devolve `null` quando a descrição está ausente. Em CreateTask, membro pode omitir responsáveis ou enviar `[]`; uma lista não vazia devolve 403 sem criação parcial, incluindo tentativa de autoatribuição. Gestor pode indicar responsáveis elegíveis por participação direta no projeto.

**Topologia e convenções aprovadas:** Frontend e API na mesma origem, com HTTPS no ambiente publicado e proxy em desenvolvimento; não permitir origens arbitrárias com credenciais. Bootstrap CSRF e convenções `JSESSIONID` (cookie de sessão) e `X-CSRF-TOKEN` (header) aprovados, condicionados à coerência com D3/D4: CSRF inclusive login/logout, HttpOnly no cookie de sessão, SameSite=Lax, Secure em HTTPS, rotação no login e invalidação no logout. Não constituem configuração executável nem prova de implementação.

Pendentes: verificação futura de suporte/compatibilidade e fixação de versões exatas antes da implementação, revisão dos detalhes contratuais não cobertos pelas decisões e aprovação final humana da P1/contrato. Os pontos 1–7 no plano já estão aprovados; não representam pendências de decisão. D3, D4 e criação privada/transacional já não são pendências. Registo público, recuperação de palavra-passe, notificações e integrações não são incluídos automaticamente. O histórico não equivale a auditoria permanente.

## Contexto histórico — Ficha 01 (não vigente onde diverge de D1–D10)

As secções abaixo preservam a análise original. Expressões como “fora do MVP”, “pendente” e “nesta fase” referem-se à Ficha 01, não revogam as decisões P1 acima.

## 1. Visão e objetivos

A Matriz é uma aplicação destinada à gestão de projetos e tarefas de equipas de trabalho, permitindo organizar o trabalho e acompanhar o seu progresso.

## 2. Factos confirmados pelo enunciado

- Nome da aplicação: **Matriz**.
- Público-alvo: **equipas de trabalho**.
- Necessidades gerais: **organizar projetos, gerir tarefas e acompanhar o progresso**.
- O cliente **não definiu** funcionalidades detalhadas, regras de negócio, prioridades ou tecnologias.

As funcionalidades e regras descritas abaixo, exceto estes factos, constituem propostas ou decisões provisórias da análise, sujeitas a validação com o cliente.

## 3. Tipos de utilizadores (proposta)

- **Membro da equipa:** consulta projetos e tarefas e atualiza as tarefas a que tem acesso.
- **Responsável pelo projeto:** organiza os projetos e as respetivas tarefas.

As permissões específicas de cada perfil ainda carecem de validação.

## 4. Requisitos funcionais propostos

| ID | Requisito | Prioridade | Justificação |
| --- | --- | --- | --- |
| RF01 | Criar e consultar projetos | Alta | Organizar o trabalho por projeto |
| RF02 | Criar e consultar tarefas associadas a um projeto | Alta | Registar o trabalho a realizar |
| RF03 | Editar o título e a descrição das tarefas | Alta | Manter a informação atualizada |
| RF04 | Atribuir tarefas a membros do projeto | Alta | Clarificar responsabilidades |
| RF05 | Alterar o estado de uma tarefa entre **Por fazer**, **Em curso** e **Concluída** | Alta | Acompanhar a execução |
| RF06 | Listar as tarefas de um projeto e os respetivos estados | Alta | Visualizar o progresso do trabalho |
| RF07 | Apresentar a contagem de tarefas por estado | Média | Resumir o progresso do projeto |

**Nota:** Todos os requisitos funcionais são propostas, não funcionalidades confirmadas pelo cliente. Os três estados são igualmente uma proposta a validar.

## 5. Regras de negócio propostas e decisões provisórias

- **RN01 — Associação obrigatória:** cada tarefa deve pertencer obrigatoriamente a um projeto. Não existem tarefas independentes.
- **RN02 — Âmbito da gestão de projetos:** no MVP é possível **criar e consultar projetos**. A **edição e eliminação de projetos ficam fora do MVP**, podendo ser avaliadas numa fase futura.
- **RN03 — Estados de tarefas:** propõem-se os estados **Por fazer**, **Em curso** e **Concluída**, sujeitos a confirmação.

Estas regras são decisões de trabalho para a especificação inicial, não requisitos confirmados pelo cliente.

## 6. Requisitos não funcionais (propostas)

- **Segurança e privacidade:** proteger o acesso à informação e limitar a exposição de dados de utilizadores e projetos, de acordo com um modelo de permissões ainda por definir.
- **Validação:** validar os dados de entrada e apresentar mensagens de erro compreensíveis.
- **Consistência:** garantir que as alterações às tarefas preservam a integridade dos dados.
- **Usabilidade e acessibilidade:** disponibilizar uma interface compreensível e acessível.

Não foram acordadas métricas de desempenho, disponibilidade ou outros níveis de serviço.

## 7. Âmbito do MVP proposto

O **Minimum Viable Product (MVP)** inclui os requisitos **RF01 a RF06**:

- Criar e consultar projetos.
- Criar e consultar tarefas de um projeto.
- Editar o título e a descrição das tarefas.
- Atribuir tarefas a membros do projeto.
- Alterar o estado das tarefas.
- Listar tarefas e respetivos estados por projeto.

O **RF07** (contagem de tarefas por estado) é desejável, mas **não bloqueia a primeira versão**.

A edição e eliminação de projetos **não integram o MVP**.

## 8. Fora do âmbito inicial (propostas)

- Edição e eliminação de projetos.
- Notificações.
- Integrações externas.
- Relatórios avançados e métricas detalhadas de progresso.
- Automações.
- Registo de tempos.
- Aplicações móveis nativas.

Estas exclusões poderão ser revistas após validação com o cliente.

## 9. Questões pendentes de validação

1. Quem pode criar projetos e quem pode consultar cada projeto?
2. Como são adicionados ou convidados os membros de um projeto?
3. Uma tarefa pode ficar sem responsável? Pode ter mais do que um responsável?
4. Os estados **Por fazer**, **Em curso** e **Concluída** são suficientes? Que transições são permitidas?
5. Como deve ser calculado o progresso de um projeto? A contagem por estado é suficiente ou é necessária uma percentagem de conclusão?
6. A autenticação é obrigatória no MVP? Como são identificados os utilizadores e definidas as suas permissões?
7. É necessário guardar um histórico de alterações às tarefas?
8. A edição ou eliminação de projetos deverá ser incluída numa versão futura? Em que condições?

## 10. Estado da especificação

Documento inicial de análise de requisitos. O âmbito do MVP e as regras propostas servem de base à criação das User Stories e critérios de aceitação, mas **continuam sujeitos a validação**. Nesta fase, não são escolhidas tecnologias nem é implementado código.
