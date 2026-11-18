# Revisão crítica — Matriz

> Historical P1 documentation review, not current application or security certification. Dated findings and human decisions below are preserved. For current acceptance and execution scope, see the [final checklist](final-review.md) and [verification evidence](verification.md).

## Fecho formal aprovado — 2026-11-11

O utilizador aprovou formalmente o fecho da P1 documental, aceitando as limitações do relatório final. P1 concluída; D1–D10 e pontos 1–7 mantidos. Este registo prevalece sobre referências anteriores a fecho pendente abaixo e nos restantes artefactos. Não constitui prova de implementação nem autorização P2. Preparação Git local autorizada separadamente, sem commit, remoto ou push.

## Relatório final da entrega — pontos 1–7 aprovados

D1–D10, D3/D4 completas e criação privada/transacional mantidas. Pontos 1–7 aprovados incorporados conforme implementation-plan.md: baseline condicionada à verificação futura de suporte; mesma origem/HTTPS/proxy; ordenação tarefas createdAt DESC, id DESC e projetos id DESC; pesquisa literal; descrições null; [] permitido ao membro na criação; listas completas e convenções CSRF. Não são pendências funcionais por decidir.

Retoma de 2026-11-11: relatórios dos workers anteriores indisponíveis no runtime (`task_list` sem tarefas recuperáveis). Executadas novas revisões independentes, só de leitura e limitadas às verificações essenciais em falta:

- **QA `a7e507313`:** sem achados comprovados em validações, nulidade/PATCH, TODO/MEDIUM, CreateTask de membro, pesquisa, ordenações, contagens e ausência de paginação.
- **Security `ad9f7c51c`:** sem incoerências comprovadas em D3/D4, identidade, criação privada/transacional, CSRF bootstrap/login/logout, mesma origem e códigos 401/403/404.
- **Code Reviewer `aec69f3cd`:** sem divergências vigentes comprovadas em rastreabilidade, âmbito, distinção entre aprovação/implementação/fecho e skills locais. A cronologia antiga está explicitamente subordinada às decisões vigentes; não foi certificada novamente.

Sessão principal confirmou os dois OpenAPI byte-idênticos, YAML sem duplicados, 154 referências locais resolvidas, 22 operações com identificadores únicos, 30 esquemas JSON Schema verificados e parâmetros de caminho obrigatórios. Confirmou também igualdade byte a byte das três skills autorizadas e carregamento da skill vigente. Hashes, recortes, evidências e limitações em `verification.md`, secção de conclusão da retoma.

Nenhuma divergência comprovada exigiu alterar contratos ou skills; nesta retoma apenas review.md e verification.md foram atualizados para consolidar resultados efetivamente recebidos. Sem baseline/diff Git, validador OpenAPI integral ou testes funcionais/segurança dinâmica; as verificações documentais não provam implementação. Suporte/compatibilidade e versões exatas ficam para antes de implementação futura autorizada.

**Sem bloqueio documental identificado nos recortes revistos. Consolidação pronta para apreciação humana; fecho P1 ainda depende de aprovação explícita. P2 não autorizada.** Sem código, instalações, inicialização Git, commits ou integração.

## Cronologia — reconciliação documental D1–D10

Decisões aprovadas pelo utilizador, registadas em `implementation-plan.md`. A aprovação final desta entrega P1 permanece pendente. Texto histórico das Fichas 01/02 preservado abaixo e explicitamente não vigente onde contraditório.

| Conflito | Tratamento documental | Estado |
| --- | --- | --- |
| MVP RF01–RF06 vs âmbito completo Ficha 03 | RF01–RF12/US01–US12 atuais, incluindo edição/eliminação e autenticação | Resolvido por D1 |
| Stack/arquitetura pendentes | Java/Spring Boot/React/PostgreSQL; monólito modular | Resolvido por D2; versões pendentes |
| Papéis team leader/developer e equipas vs projeto | Gestor/membro por projeto; participação direta; seed | Resolvido por D3/D6; matriz completa e criador gestor aprovados |
| JWT/Bearer vs sessão | Sessões Spring Security, HttpOnly e CSRF | Resolvido por D4; detalhes contratuais/operacionais sujeitos a revisão |
| Um responsável obrigatório vs vários | Zero ou mais participantes diretos elegíveis; atribuição N:M | Resolvido por D5/D6; protótipo antigo não alterado |
| Eliminação excluída/sem política | Eliminação definitiva transacional de projeto/tarefa e dependentes | Resolvido por D1/D7/D10 |
| Limites e prioridade ausentes | Validações, enumerações e valores iniciais D8 | Resolvido documentalmente; não implementado |
| Contagens opcionais/ambíguas | Painel global autorizado, contagens projeto por prioridade, total lista pelos dois filtros, sem efeito de paginação | Resolvido por D9 |
| Histórico genérico vs limitado | Só alterações efetivas de estado/prioridade/responsáveis, anterior/novo/autor/UTC; removido com tarefa/projeto | Resolvido por D10 |
| Duas fontes de contrato | openapi.yaml canónico; docs/matriz-openapi.yaml cópia sincronizada | Resolvido por D10; evidência de igualdade em verification.md |
| Skills com regra antiga | Duas cópias locais e uma global autorizada alinhadas; outras skills não alteradas | Outras skills podem conter propostas antigas; prevalecem decisões aprovadas |

### Atualização final autorizada

D3 completa, D4 operacional e criação privada/transacional confirmadas como aprovadas. A classificação anterior de matriz e parâmetros de sessão como decisões pendentes foi corrigida; não se trata de controlos implementados. Criador é gestor automaticamente, sem outros membros, na mesma transação. 401 sem autenticação, 403 sem permissão, 404 recurso inexistente/inacessível. Ver matriz e limites precisos em implementation-plan.md. Fecho P1 continua não aprovado.

Revisão final independente `a1468e0db` concluída após esta atualização, com perspetivas Code Reviewer/Security na mesma sessão: sem bloqueios documentais identificados ou contradições vigentes D3/D4; cópias OpenAPI e três skills confirmadas idênticas, loader atualizado e verificações estáticas parciais aprovadas. Não houve nova revisão QA separada nem testes funcionais. Evidências e pendências residuais em verification.md.

### Revisões da entrega anterior (cronologia; ver atualização em verification.md)

QA, Security e Code Reviewer foram solicitados como workers independentes, em leitura. Security e Code Reviewer entregaram relatórios: CR-01 prioridade inicial, CR-02 associação do criador, CR-03 descrição de paginação e SEC-03 bootstrap CSRF originaram correções documentais solicitadas. Na revisão inicial, SEC-01/SEC-02 foram classificadas como pendências; essa classificação foi corrigida pela confirmação humana posterior de D3/D4 e criação privada/transacional (ver atualização abaixo). QA inicial falhou por codificação da saída do runtime; segunda revisão concluída em ASCII identificou QA-01 (prioridade inicial, alta), QA-02 (paginação do histórico, média) e QA-03 (resposta 400 de consulta de tarefa, baixa). As correções e a re-revisão final estão registadas em verification.md: Code Reviewer confirmou CR-01/02/03, QA-01/02/03 e SEC-03 tratados no contrato final, com cópias idênticas e verificações estáticas parciais aprovadas. SEC-01/SEC-02 deixam de ser pendências quanto à matriz e aos parâmetros D4 aprovados; origens CORS continuam por definir. Só o utilizador pode aprovar o fecho P1. Resultados e confirmações finais em `verification.md`; nenhuma aprovação é presumida. Pendências atuais: detalhes contratuais não abrangidos pelas decisões, topologia/origens CORS, versões e aprovação final humana. Nenhum teste funcional ou garantia de segurança em runtime decorre destas alterações.

## Contexto histórico — Fichas 01/02

As conclusões seguintes descrevem as revisões anteriores; não constituem testes executados nesta P1 nem anulam D1–D10.

## 1. Objetivo

Este documento regista a revisão crítica da documentação inicial
do projeto Matriz, elaborada com apoio de inteligência artificial
e revista por uma pessoa.

Não constitui validação pelo cliente.

## 2. Documentos analisados

- `requirements.md` — visão, requisitos e MVP proposto.
- `user-stories.md` — histórias de utilizador, critérios de aceitação e exceções.
- `constraints.md` — restrições, pressupostos e decisões pendentes.
- `../README.md` — apresentação e orientação do repositório.

## 3. Ambiguidades identificadas

- **Progresso:** não está definido se significa apenas o estado
  de cada tarefa, contagens por estado ou percentagem de conclusão.

- **Permissões:** não está definido quem pode criar projetos,
  criar ou editar tarefas, atribuí-las e alterar estados.

- **Membros:** falta definir como são associados a um projeto
  e se uma tarefa pode ficar sem responsável.

- **Estados:** «Por fazer», «Em curso» e «Concluída» são uma
  proposta; as transições permitidas não foram validadas.

## 4. Funcionalidades e âmbito

- **MVP proposto:** criar e consultar projetos (RF01);
  criar, consultar e editar tarefas (RF02–RF03);
  atribuir tarefas (RF04); alterar estados (RF05);
  listar tarefas e estados (RF06).

- **Fora do MVP:** edição e eliminação de projetos,
  notificações, integrações, automações e relatórios avançados.

- **Opcional:** contagem de tarefas por estado (RF07),
  sujeita a validação.

- **Autenticação:** desejável, mas a sua inclusão no MVP
  permanece em aberto; não se deve pressupor que já está implementada.

## 5. Pressupostos e regras de negócio

- Cada tarefa pertence obrigatoriamente a um projeto:
  decisão de trabalho tomada durante a revisão,
  ainda não validada pelo cliente.

- Os papéis «membro» e «responsável» são propostos,
  não confirmados.

- Título obrigatório, descrição opcional e estado inicial
  «Por fazer» são propostas a validar.

- A atribuição de tarefas pressupõe uma forma de gerir
  membros dos projetos, ainda não especificada.

## 6. Testabilidade

As histórias US01–US06 incluem critérios de aceitação observáveis
no formato Given/When/Then.

Antes da implementação, convém definir:

- Limites dos campos.
- Comportamento perante identificadores inexistentes.
- Regras de autorização.
- Transições de estado permitidas.

Devem ser testados, pelo menos, os seguintes casos:

1. Título vazio.
2. Acesso não autorizado.
3. Atribuição a membro externo ao projeto.
4. Estado de tarefa inválido.

## 7. Segurança e privacidade

A documentação identifica a necessidade de:

- Controlar acessos.
- Validar entradas.
- Proteger dados pessoais.
- Evitar a exposição indevida de informação sensível.

Falta decidir:

- O método de autenticação.
- O modelo de permissões.
- A retenção e eliminação de dados.
- O tratamento de erros sem exposição de informação sensível.

Estas decisões devem anteceder a implementação
de funcionalidades que dependam delas.

## 8. Coerência entre documentos

- US01–US06 correspondem, em termos gerais, a RF01–RF06.

- O MVP descrito deve manter-se igual em `requirements.md`,
  `user-stories.md`, `constraints.md` e `README.md`.

- A regra de associação obrigatória de tarefas a projetos
  deve aparecer de forma consistente.

- **Ponto de atenção:** a US04 (atribuir tarefa) depende da
  existência de membros associados a projetos.
  Essa gestão ainda não tem requisitos detalhados.

- **Ponto de atenção:** referências a permissões nos critérios
  de aceitação não significam que o modelo de autenticação
  esteja decidido.

## 9. Decisões humanas e próximos passos

1. Manter provisoriamente RF01–RF06 como âmbito do MVP
   e RF07 como opcional.

2. Não incluir edição nem eliminação de projetos
   na primeira versão proposta.

3. Manter a autenticação e o modelo de permissões
   como decisões pendentes.

4. Validar com o cliente:
   - Papéis dos utilizadores.
   - Gestão de membros.
   - Estados das tarefas.
   - Regras de atribuição.
   - Definição de progresso.

5. Escolher tecnologias e arquitetura apenas na fase seguinte,
   conforme a ficha prática.

## 10. Conclusão

A documentação é uma base coerente para discussão,
mas não é uma especificação aprovada pelo cliente.

As sugestões da IA foram analisadas criticamente.

A aceitação final dos requisitos e pressupostos depende
de validação humana e, quando aplicável, do cliente.

Nesta fase não foi implementado código.
---

# Ficha Prática 02 — Exercício 5: Revisão Crítica e Human-in-the-Loop

## Âmbito e método

Revisão cruzada do Specification Pack (Ficha 01), da proposta de arquitetura,
do modelo ER, do contrato OpenAPI 3.1 e do protótipo UI. Foram comparados os
artefactos efetivamente presentes no repositório, sem tratar os exemplos do
professor como requisitos aprovados. As alterações nesta cópia constituem
**propostas revistas**, não validação pelo cliente.

## Matriz de inconsistências e tratamento

| ID | Observação | Tratamento nesta cópia | Estado |
| --- | --- | --- | --- |
| R01 | US02 não exigia responsável na criação, embora o modelo ER e o OpenAPI o exijam. | Critério de aceitação atualizado: título e responsável elegível obrigatórios; estado inicial TODO. | Proposta corrigida; validar com cliente. |
| R02 | US04 descrevia atribuição inicial, mas a tarefa já nasce atribuída. | Renomeada para reatribuição; mantém um único responsável e prevê registo no histórico. | Proposta corrigida; validar permissões. |
| R03 | `architecture.md` desaconselhava comentários enquanto o modelo ER e o OpenAPI os incluem. | Distinção explícita entre núcleo do MVP e extensões propostas (comentários/histórico). | Contradição documental corrigida; âmbito pendente. |
| R04 | O README principal só descrevia a Ficha 01. | Atualizado com o Architecture Pack e os caminhos relevantes. | Corrigido. |
| R05 | O contrato estava em `docs/matriz-openapi.yaml`, mas o enunciado pede `openapi.yaml` na raiz. | Criada cópia idêntica em `openapi.yaml`; original preservado. | Corrigido; manter cópias sincronizadas. |
| R06 | A UI grava tarefas em `localStorage`, mas não existe backend. | Mantida a simulação; a limitação já está descrita em `docs/ui/README.md`. | Coerente enquanto protótipo, não funcionalidade final. |
| R07 | A UI permite alterar estados sem autenticação nem histórico. | Não implementar nesta ficha; documentar a diferença entre protótipo e API. | Pendente para implementação. |
| R08 | O modelo inclui equipas e membros, mas o OpenAPI não inclui gestão de associações. | Não inventar endpoints; identificar a necessidade de definir o processo de gestão de membros antes da implementação. | Pendente de validação. |
| R09 | O prompt de UI sugere estados loading/empty/error; o protótipo trata alguns erros locais, mas não cobre todos os estados visualmente. | Registar lacuna sem ampliar o protótipo nesta revisão. | Melhoria recomendada. |

## Verificações de coerência

- **Arquitetura:** monólito modular proposto e justificado; sem backend implementado.
- **Dados:** projeto com gestor; equipas e utilizadores em relações N:M; cada tarefa pertence a um projeto e tem exatamente um responsável.
- **API:** criação de projeto e tarefa, consulta/edição de tarefas, reatribuição, atualização de estado, comentários e histórico documentados; operações adicionais são propostas e não requisitos confirmados.
- **UI:** dashboard com estados Por fazer/Em progresso/Concluído; criação com título e responsável obrigatórios; estado TODO na criação; dados guardados apenas no navegador.
- **Limite importante:** a UI não comprova autorização, pertença a projeto, histórico auditável ou persistência de servidor; essas garantias dependem do backend futuro.

## Decisões e validações humanas pendentes

1. Confirmar com o cliente o responsável único obrigatório, as regras de elegibilidade e a reatribuição.
2. Definir exatamente quem pode criar projetos, editar tarefas, alterar estados e reatribuir responsáveis.
3. Decidir se comentários e histórico integram o MVP ou são extensões futuras; manter os contratos como proposta até essa decisão.
4. Especificar como se criam equipas e se gerem as associações entre utilizadores, equipas e projetos.
5. Confirmar transições permitidas, limites dos campos e regras para nomes/títulos duplicados.
6. Definir autenticação e tecnologias de implementação sem as assumir como requisitos confirmados.

## Resultado da revisão

Os artefactos estão globalmente alinhados enquanto **proposta académica de Architecture Pack**.
As inconsistências documentais R01–R05 foram tratadas numa cópia revista; R06–R09
são limitações ou decisões pendentes, não defeitos de um backend existente.
Esta revisão não constitui aprovação do cliente, nem teste funcional da API.
Antes da entrega, rever o diff e versionar as alterações no Git.
