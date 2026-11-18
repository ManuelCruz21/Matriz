# User Stories — Matriz

## P1 — backlog vigente (D1–D10)

As histórias desta secção substituem as versões históricas abaixo. Âmbito, matriz D3 completa, política D4 e criação privada/transacional aprovados; entrega documental e detalhes contratuais continuam sujeitos a aprovação final. “Autorizado” não define por omissão uma operação como permitida a qualquer membro. Ver [implementation-plan.md](implementation-plan.md).

| ID | História atual | Critérios de aceitação documentais do comportamento futuro |
| --- | --- | --- |
| US01 | Como utilizador autorizado, criar/listar/consultar projetos | Given sessão e permissão, When crio projeto com nome 3–100 após trim, Then aparece entre os acessíveis; nome vazio/inválido é rejeitado. Descrição opcional até 1000. |
| US02 | Como participante autorizado, criar tarefa | Given gestor ou membro do projeto, When envio título 3–150 após trim, Then tarefa pertence ao projeto e inicia TODO/MEDIUM. Membro pode omitir responsáveis ou enviar `[]`; lista não vazia devolve 403 sem criação parcial, inclusive autoatribuição. Gestor pode incluir responsáveis elegíveis. Descrição opcional até 2000. Responsável externo é rejeitado sem criação parcial. |
| US03 | Como participante autorizado, editar tarefa | Given tarefa acessível, When edito título/descrição/prioridade válidos, Then consulta reflete valores. Prioridade inválida ou título vazio é rejeitado; mudança efetiva de prioridade gera histórico. |
| US04 | Como gestor do projeto, gerir responsáveis | Given membros diretos do projeto, When associo/removo atribuições, Then tarefa pode ter zero/vários responsáveis sem duplicados. Pessoa inelegível é rejeitada; só alterações efetivas geram histórico anterior/novo. |
| US05 | Como participante autorizado, alterar estado | Given tarefa acessível, When altero para TODO/IN_PROGRESS/DONE, Then estado muda e alteração efetiva tem utilizador, UTC e valores anterior/novo. Valor fora do conjunto é rejeitado; no-op não gera histórico. Transições adicionais restritivas não foram aprovadas. |
| US06 | Como participante, consultar tarefas | Given projeto acessível, When filtro por estado e prioridade, Then lista e total respeitam ambos; lista vazia é explícita; lista completa sem paginação, ordenada por `createdAt DESC, id DESC`. |
| US07 | Como participante, consultar contagens do projeto | Given filtro de prioridade e/ou estado, When consulto contagens por estado, Then respeitam prioridade e ignoram estado, sobre o universo autorizado completo; sem paginação no MVP. |
| US08 | Como utilizador, iniciar/terminar sessão e consultar perfil | Given credenciais válidas, When inicio sessão, Then identidade vem da sessão Spring Security e cookie HttpOnly; inválidas não autenticam. When termino sessão, Then não a posso reutilizar. Pedidos mutáveis exigem CSRF; UI informa expiração. |
| US09 | Como gestor do projeto, editar/eliminar projeto | Given projeto acessível e permissão, When edito dados válidos, Then persistem. When confirmo eliminação na UI, Then projeto e dependentes incluindo histórico são eliminados definitivamente numa transação; falha não deixa eliminação parcial. |
| US10 | Como gestor do projeto, eliminar tarefa | Given tarefa acessível e confirmação na UI, When elimino, Then tarefa, atribuições, comentários e histórico desaparecem numa transação, sem órfãos. |
| US11 | Como participante autorizado, comentar | Given tarefa acessível, When envio conteúdo de 1–2000 após trim, Then comentário apresenta autor inferido da sessão e data. Texto vazio é rejeitado; não cria histórico de tarefa. |
| US12 | Como utilizador, pesquisar e ver painel global | Given projetos acessíveis, When pesquiso nome sem distinguir maiúsculas, Then vejo apenas resultados acessíveis por `id DESC`, sem paginação. Pesquisa é substring literal, após trim, case-insensitive e accent-sensitive; omitida/vazia não filtra, `%`/`_` são literais. Painel global conta tarefas dos projetos acessíveis, não apenas uma fatia da listagem. |

**Casos transversais futuros:** acesso não autenticado e acesso cruzado por identificadores diretos a projetos/tarefas/comentários/atribuições/histórico; ausência de CSRF; limites mínimo/máximo e fora de limite; texto só com espaços; zero/múltiplos responsáveis; transação revertida em falha. Descrição ausente é permitida; descrição textual vazia é rejeitada. Nenhum destes testes foi executado nesta P1 documental.

**Critérios complementares aprovados D3:** gestor edita/elimina projeto, cria/edita/altera estado de qualquer tarefa do seu projeto, elimina tarefas e gere responsáveis. Membro cria tarefas, só edita/altera estado das atribuídas a si. Ambos consultam/comentam. Tarefa sem responsáveis não pode ser editada pelo membro só por ser seu criador. Não permitir gerir responsáveis por um campo alternativo na criação/edição. Backend devolve 401 sem autenticação, 404 para recurso inexistente/inacessível e 403 para operação sem permissão sobre recurso acessível.

**US01 complementada:** criação associa automaticamente criador como gestor na mesma transação; projeto privado sem outros membros; falha da associação não deixa projeto criado parcialmente.

**US08 complementada:** após 30 minutos sem atividade ou 8 horas de duração absoluta, sessão deixa de autenticar; atividade não reinicia o máximo absoluto. Cookie HttpOnly, SameSite=Lax, Secure em HTTPS; CSRF inclusive login/logout; rotação no login, invalidação no logout. São critérios de testes futuros, não testes executados.

**Critérios complementares aprovados (pontos 1–7):** Descrições de projetos e tarefas: na criação, campo omitido ou `null` significa ausência; em PATCH, omitido mantém o valor e `null` remove-o. Texto é sujeito a trim e rejeitado se ficar vazio, respeitando os limites D8. A API devolve `null` quando a descrição está ausente. Projetos: `id DESC`; tarefas: `createdAt DESC, id DESC`; comentários e histórico: data UTC `ASC, id ASC`; responsáveis elegíveis: nome sem distinguir maiúsculas/minúsculas `ASC, id ASC`. Todas as listas são completas, sem paginação no MVP. Frontend e API na mesma origem, com HTTPS no ambiente publicado e proxy em desenvolvimento; não permitir origens arbitrárias com credenciais. Bootstrap CSRF e convenções `JSESSIONID` (cookie de sessão) e `X-CSRF-TOKEN` (header) aprovados, condicionados à coerência com D3/D4: CSRF inclusive login/logout, HttpOnly no cookie de sessão, SameSite=Lax, Secure em HTTPS, rotação no login e invalidação no logout. Não constituem configuração executável nem prova de implementação.

**Dependências reais:** Java 21 LTS, Spring Boot 3.5.x, React 19.x e PostgreSQL 17.x. Verificar suporte e compatibilidade antes da implementação e fixar as versões exatas nessa fase futura; qualquer substituição requer aprovação humana. Suporte e compatibilidade não foram verificados nesta atualização documental. Revisão de coerência contratual e aprovação final humana P1 continuam pendentes; não há autorização P2.

## Contexto histórico — histórias das Fichas 01/02

Preservado para rastreabilidade; não vigente onde contradiz D1–D10. Em particular, responsável único obrigatório e exclusão de edição/eliminação de projetos foram substituídos.

**Estado:** Backlog proposto, sujeito a validação pelo cliente.
**Prioridade:** Alta = incluída na proposta de MVP.

## Contexto e pressupostos

A Matriz pretende permitir que equipas organizem projetos, giram tarefas e acompanhem o progresso do trabalho.

As histórias seguintes são **propostas funcionais**, não requisitos confirmados pelo cliente.

- Cada tarefa pertence obrigatoriamente a um projeto existente e tem exatamente um responsável desde a criação (proposta de desenho).
- Criar, editar, atribuir e atualizar tarefas depende de permissões a definir.
- Os papéis «responsável pelo projeto» e «membro autorizado» são provisórios.
- Estados propostos: **Por fazer**, **Em curso** e **Concluída**.
- Não está prevista a edição ou eliminação de projetos no MVP.
- A gestão de membros e o modelo de permissões ainda precisam de validação.

## US01 — Criar projeto (Alta)

**Como** responsável pelo projeto, **quero** criar um projeto com nome, **para** organizar as tarefas da equipa.

**Critérios de aceitação**
1. **Given** que tenho permissão para criar projetos, **When** submeto um nome com pelo menos um caráter diferente de espaço, **Then** o projeto é criado e aparece na lista de projetos.
2. **Given** que submeto um nome vazio ou composto apenas por espaços, **When** tento guardar, **Then** o sistema rejeita a criação e apresenta uma mensagem de validação.
3. **Given** que não tenho permissão para criar projetos, **When** tento criar um projeto, **Then** o sistema rejeita a operação e não cria o projeto.

## US02 — Criar tarefa (Alta)

**Como** membro autorizado, **quero** criar uma tarefa num projeto, **para** registar trabalho a realizar.

**Critérios de aceitação**
1. **Given** que tenho acesso a um projeto existente e permissão para criar tarefas, **When** crio uma tarefa com título não vazio e seleciono um responsável elegível, **Then** a tarefa fica associada a esse projeto, apresenta o responsável selecionado e inicia no estado «Por fazer».
2. **Given** que o título está vazio ou contém apenas espaços, **When** tento criar a tarefa, **Then** o sistema rejeita a criação e apresenta uma mensagem de validação.
3. **Given** que não selecionei um responsável ou que o utilizador não pertence ao projeto e a uma equipa nele alocada, **When** tento criar a tarefa, **Then** a criação é rejeitada e é apresentada uma mensagem de validação.
4. **Given** que o projeto não existe ou não tenho acesso a ele, **When** tento criar uma tarefa nesse projeto, **Then** o sistema rejeita a operação e não cria a tarefa.

## US03 — Editar tarefa (Alta)

**Como** membro autorizado, **quero** editar o título e a descrição de uma tarefa, **para** manter a informação correta.

**Critérios de aceitação**
1. **Given** que a tarefa existe e tenho permissão para a editar, **When** guardo um novo título válido e uma descrição, **Then** a consulta da tarefa apresenta os valores atualizados.
2. **Given** que introduzo um título vazio ou apenas com espaços, **When** tento guardar, **Then** o sistema rejeita a alteração e mantém o título anterior.
3. **Given** que a tarefa não existe ou não tenho acesso a ela, **When** tento editá-la, **Then** a alteração não é efetuada.

## US04 — Reatribuir tarefa (Alta)

**Como** responsável pelo projeto, **quero** reatribuir uma tarefa a outro membro elegível, **para** atualizar quem a executa.

**Critérios de aceitação**
1. **Given** que a tarefa pertence ao projeto e o membro selecionado pertence a esse projeto, **When** reatribuo a tarefa ao membro, **Then** a tarefa apresenta esse membro como único responsável e a alteração fica registada no histórico.
2. **Given** que o membro selecionado não pertence ao projeto, **When** tento atribuir-lhe a tarefa, **Then** o sistema rejeita a atribuição e mantém o responsável anterior.
3. **Given** que não tenho permissão para atribuir tarefas, **When** tento alterar o responsável, **Then** o sistema rejeita a alteração.

## US05 — Atualizar estado (Alta)

**Como** membro autorizado, **quero** atualizar o estado de uma tarefa, **para** comunicar o seu progresso.

**Critérios de aceitação**
1. **Given** que tenho permissão para atualizar a tarefa, **When** seleciono um estado permitido («Por fazer», «Em curso» ou «Concluída»), **Then** o novo estado fica guardado e visível na consulta da tarefa.
2. **Given** que é enviado um estado fora dos valores permitidos, **When** tento atualizar a tarefa, **Then** a alteração é rejeitada e o estado anterior é mantido.
3. **Given** que não tenho permissão para alterar a tarefa, **When** tento atualizar o seu estado, **Then** o sistema rejeita a operação.

## US06 — Consultar tarefas (Alta)

**Como** membro do projeto, **quero** consultar a lista de tarefas e os respetivos estados, **para** acompanhar o trabalho.

**Critérios de aceitação**
1. **Given** que tenho acesso ao projeto, **When** abro a lista de tarefas, **Then** vejo apenas as tarefas associadas a esse projeto e os respetivos estados.
2. **Given** que o projeto não tem tarefas, **When** abro a lista, **Then** vejo uma indicação de lista vazia.
3. **Given** que não tenho acesso ao projeto, **When** tento consultar as suas tarefas, **Then** o sistema impede a consulta.

## Casos excecionais e casos-limite

1. **Campos obrigatórios vazios:** rejeitar nomes de projetos ou títulos de tarefas vazios ou apenas com espaços.
2. **Acesso não autorizado:** impedir consultas e alterações sem a permissão necessária.
3. **Membro fora do projeto:** impedir a atribuição de tarefas a pessoas que não pertencem ao projeto.
4. **Projeto ou tarefa inexistente:** rejeitar operações sobre identificadores que não correspondem a entidades existentes.
5. **Estado inválido:** rejeitar estados fora do conjunto permitido e preservar o valor anterior.
6. **Projeto sem tarefas:** apresentar uma lista vazia com mensagem informativa.

## Pontos pendentes de validação pelo cliente

- **Permissões e papéis:** quem pode criar projetos, criar/editar/atribuir tarefas, alterar estados e consultar informação?
- **Gestão de membros:** como entram os membros nos projetos e quem gere essa associação?
- **Campos:** quais os limites de tamanho para nomes, títulos e descrições? A descrição é opcional?
- **Responsável:** proposta interna: responsável único obrigatório desde a criação e reatribuição autorizada; falta validação do cliente.
- **Estados:** são estes os três estados pretendidos? São permitidas todas as transições entre eles?
- **Identificadores e duplicados:** podem existir projetos com o mesmo nome ou tarefas com o mesmo título?

## Decisões de âmbito para o MVP

- **Incluído (proposta):** criar projetos; criar, editar, atribuir, consultar e atualizar o estado de tarefas.
- **Fora do MVP (decisão de âmbito já tomada):** editar e eliminar projetos.
- **Não decidido:** restantes operações ou funcionalidades não descritas neste backlog.

> **Nota de validação:** Os critérios acima especificam o comportamento esperado **caso os pressupostos sejam aprovados**. Antes da implementação, atualizar este documento e `docs/requirements.md` com as decisões do cliente, mantendo-os consistentes.
