---
status: proposto
última-atualização: 2026-07-19
responsável: matevz77
---

# Prompts de Correção — Pré-Fase 2

Este ficheiro reúne os prompts corretivos identificados no diagnóstico completo
do projeto, a executar **antes** de iniciar a Fase 2 (Segurança/JWT). Segue a
mesma convenção de `prompts-correcao-consistencia-tecnica.md`: prompts
sequenciais, cada um independentemente testável, com o raciocínio arquitetural
explicado antes da instrução de implementação — conforme `.cursorrules`,
secção 7.

**Ordem de execução:** os prompts A e B são correções pontuais e podem ser
aplicados em qualquer momento. Os prompts C, D e E têm dependência lógica
crescente (C depende de nada; D depende de C estar concluído para o
`GlobalExceptionHandler` já cobrir os novos casos de erro; E depende de C e D
estarem concluídos, para haver algo de substância a testar). Recomenda-se
executar pela ordem apresentada.

---

## Prompt A — Corrigir `docs/05-migrations.md` (V5 obsoleta)

### Contexto para o agente

A decisão sobre o sequenciamento de migrações já foi tomada e está refletida
no código: `V3__create_comment_table.sql` e `V4__create_notification_table.sql`
já incluem os seus próprios índices. Não existe nem existirá uma migração
`V5` dedicada a índices. `docs/05-migrations.md`, secção 4, ainda lista essa
V5 como planeada, e ainda repete as linhas de V6/V7 duas vezes. Isto viola a
convenção do próprio `docs/README.md`: "nunca considerar como fiável um
documento com informação desatualizada sem confirmação prévia."

### Instrução

1. Em `docs/05-migrations.md`, secção "4. Migrações Planeadas":
   - Remove a linha `| V5 | Adicionar índices (tb_comments, tb_notifications) | V3, V4 |`.
   - Remove a duplicação das linhas `V6` e `V7` (aparecem duas vezes na tabela).
   - Atualiza o campo `última-atualização` no cabeçalho de metadados para a
     data de execução deste prompt.
2. Não alteres nenhuma migração `.sql` existente — apenas a documentação.
3. Não avances a numeração de futuras migrações; `V6` continua reservada
   para a tabela de auditoria de prioridade, `V7` para o refresh token,
   ambas em "Futuro".

### Critério de teste

- A tabela da secção 4 passa a ter exatamente as entradas V1 a V4 (existentes)
  seguidas de V6 e V7 (futuras), sem duplicações e sem V5.
- Nenhum ficheiro em `src/main/resources/db/migration/` é alterado.

---

## Prompt B — Converter DTOs mutáveis em `record`

### Contexto para o agente

`.cursorrules`, secção 4, é explícita: "DTOs devem ser `record`, nunca
classes mutáveis." Quatro DTOs violam esta regra: `CreateCommentRequest`,
`CommentResponse`, `CreateUserRequest` e `UserResponse`. Os restantes DTOs
do projeto (`CreateIssueRequest`, `IssueResponse`, `IssueFilter`,
`NotificationResponse`) já seguem a convenção corretamente e servem de
referência de estilo.

Esta correção deve ser feita **antes** da Fase 2 porque essa fase introduzirá
novos DTOs de autenticação (`LoginRequest`, `AuthResponse`, ou equivalentes);
não convém propagar o padrão incorreto para código novo.

### Instrução

1. Converte `CreateCommentRequest` (atualmente em
   `comment/presentation/dto/`) para `record`, mantendo o campo `content`
   e as anotações de validação Bean já existentes (se as houver) ou
   adicionando `@NotBlank` em `content`, por analogia com
   `CreateIssueRequest`.
2. Converte `CommentResponse` para `record`, com os campos `id`, `issueId`,
   `authorId`, `content`, `createdAt` — mantendo os mesmos tipos e nomes
   atuais.
3. Converte `CreateUserRequest` para `record`, preservando todas as
   anotações de validação (`@NotBlank`, `@Email`, `@Size`) exatamente como
   estão na versão em classe.
4. Converte `UserResponse` para `record`, com os campos `id`, `username`,
   `email`, `role`, `active`, `createdAt`.
5. Atualiza todos os pontos de utilização destes DTOs (`UserController`,
   `CreateUserUseCase`, e qualquer classe de `comment/` que já os referencie)
   para usar os acessores de `record` (`request.username()` em vez de
   `request.getUsername()`, etc.) em vez dos getters/setters removidos.
6. Não alteres `CreateIssueRequest`, `IssueResponse`, `IssueFilter` nem
   `NotificationResponse` — já estão corretos.

### Critério de teste

- O projeto compila sem erros (`mvn compile`).
- Nenhuma classe DTO em `presentation/dto/` (de qualquer módulo) permanece
  como classe mutável com getters/setters.
- `UserController.create(...)` continua a devolver `201 Created` com o
  mesmo corpo de resposta (validar manualmente via `curl` ou Postman, dado
  não existirem ainda testes de integração).

---

## Prompt C — Completar o CRUD de Issue (RF-16 a RF-19, sem autorização de Role)

### Contexto para o agente

`docs/06-api-contract.md`, secções 3.4 a 3.8, documenta seis operações sobre
issues que ainda não têm endpoint correspondente: alterar estado
(`PATCH /status`), sobrepor prioridade (`PATCH /priority`), reatribuir
responsável (`PATCH /assignee`), eliminar (`DELETE`) e editar título/descrição
(`PATCH /details`). O `STATUS.md` atual classifica-as como dependentes da
Fase 2, mas essa classificação está incorreta: a **lógica de negócio** destas
operações (RF-16, RF-17, RF-18, RF-19) não depende de JWT — já existe em
`Issue.java` (`changeStatus`, `assignTo`, `setPriority`) ou é trivial de
adicionar. Só a **autorização por Role** (RN-01, RN-02) depende da Fase 2.

Segue-se, portanto, o mesmo padrão já usado em `POST /api/v1/users`: o
endpoint fica funcional agora, sem proteção de Role, com um comentário
`// TODO(Fase 2)` explícito a assinalar a autorização pendente — exatamente
como já documentado em `STATUS.md`, secção "Notas de Scaffolding".

**Não** implementar ainda o registo de auditoria da RF-06 (sobreposição
manual de prioridade) nem a integração com Kafka/RabbitMQ — esses aspetos
pertencem a fases posteriores e não bloqueiam este prompt.

### Instrução

1. Cria `IssueUpdateRequest` (o ficheiro já existe, vazio) como um `record`
   único cobrindo os quatro cenários de atualização parcial, à semelhança do
   contrato em `06-api-contract.md`. Sugestão de desenho: usa DTOs
   dedicados por operação (`ChangeStatusRequest`, `OverridePriorityRequest`,
   `ReassignRequest`, `UpdateDetailsRequest`), todos como `record`, em vez
   de um único DTO genérico — mantém a coerência com `CreateIssueRequest`.
2. Implementa `UpdateIssueUseCase` (o ficheiro já existe, vazio) com quatro
   métodos (ou quatro use cases separados, se preferires manter um único
   caso de uso por ficheiro, conforme o padrão de `CreateIssueUseCase`):
   - `changeStatus(UUID id, IssueStatus newStatus)` → invoca
     `Issue.changeStatus`, que já lança `IllegalStateException` (fechada) ou
     `IllegalArgumentException` (RN-05), ambas já mapeadas para 422 pelo
     `GlobalExceptionHandler`.
   - `overridePriority(UUID id, IssuePriority priority)` → invoca
     `Issue.setPriority(priority, null)`, uma vez que a sobreposição manual
     não tem `aiConfidenceScore` associado.
   - `reassign(UUID id, UUID newAssigneeId)` → carrega o novo assignee via
     `UserRepository`, invoca `Issue.assignTo`.
   - `updateDetails(UUID id, String title, String description)` → requer
     um novo método `Issue.updateDetails(String title, String description)`
     no domínio (atualmente `title` é `final`; avalia se deve deixar de o
     ser, visto que a RF-19 exige edição de título — sinaliza esta mudança
     estrutural ao utilizador antes de a implementares, por implicar alterar
     um invariante de domínio já existente).
   - Todos os métodos devem lançar `IssueNotFoundException` se o `id` não
     existir, reutilizando a exceção já existente.
3. Implementa `DeleteIssueUseCase` (novo ficheiro,
   `issue/application/DeleteIssueUseCase.java`), que remove a issue via
   `IssueRepository` (adiciona o método `deleteById(UUID id)` à interface
   `IssueRepository` e à sua implementação em `IssueRepositoryAdapter`).
4. Adiciona a `IssueController` os cinco endpoints correspondentes
   (`PATCH /{id}/status`, `PATCH /{id}/priority`, `PATCH /{id}/assignee`,
   `PATCH /{id}/details`, `DELETE /{id}`), com o comentário
   `// TODO(Fase 2): restringir por Role conforme RN-01/RN-02 quando o
   SecurityContext estiver operacional` em cada um.
5. Atualiza `docs/STATUS.md` para refletir estes itens como `⚡` (implementado
   sem testes), com uma nota a explicar que a autorização de Role permanece
   pendente da Fase 2.

### Critério de teste

- Os cinco novos endpoints respondem com o código HTTP documentado em
  `06-api-contract.md` (200 para os PATCH, 204 para o DELETE).
- Testar manualmente: `PATCH /status` para `CLOSED` sem descrição de
  resolução devolve 422 (RN-05); `DELETE` de uma issue com comentários
  associados remove-os em cascata (validar via `GET` subsequente ao
  comentário, quando o Prompt D estiver concluído).
- O projeto compila e os endpoints existentes (`POST`, `GET`) continuam a
  funcionar sem regressão.

---

## Prompt D — Implementar minimamente Comment e Notification (RF-07, RF-08, RF-15)

### Contexto para o agente

Os domínios `Comment.java` e `Notification.java` já estão corretamente
modelados, incluindo a regra RN-06 (o autor de uma issue não pode comentar a
sua própria issue). Falta, no entanto, ligar esse domínio à infraestrutura e
à apresentação, seguindo exatamente o padrão já validado em `Issue` e `User`:
interface de repositório de domínio → adapter JPA → use case → controller.

**Fora de escopo deste prompt:** a publicação de eventos Kafka
(`CommentAddedEvent`) e a notificação assíncrona via RabbitMQ — RF-08 será
implementada aqui de forma **síncrona** (persistência direta da
`Notification`), com o mesmo comentário de scaffolding temporário já usado
noutros pontos do projeto, a substituir na Fase 5.

### Instrução

1. Define `CommentRepository` (interface de domínio, atualmente vazia) com
   os métodos `save(Comment comment)`, `findByIssueId(UUID issueId)`, por
   analogia com `IssueRepository` e `UserRepository`.
2. Implementa `CommentRepositoryAdapter` (novo ficheiro em
   `comment/infrastructure/persistence/`), usando `CommentJpaRepository` e
   `CommentMapper` já existentes, seguindo o padrão de
   `IssueRepositoryAdapter` para reconstrução das associações (`issue` e
   `author`, atualmente ignoradas pelo `CommentMapper.toDomain`).
3. Implementa `CreateCommentUseCase` (ficheiro já existe, vazio):
   - Carrega a `Issue` via `IssueRepository` e o `author` via
     `UserRepository`.
   - Invoca `Comment.create(issue, author, content)`, que já valida RN-06
     internamente.
   - Persiste via `CommentRepository`.
   - Cria a `Notification` correspondente (RF-08: notificar o autor
     original da issue) via `Notification.create(...)` e persiste-a
     diretamente através de `NotificationRepository` — sem passar por
     RabbitMQ nesta fase. Assinala com
     `// TODO(Fase 5): substituir persistência direta por publicação
     RabbitMQ + NotificationConsumer`.
4. Implementa `CommentController` (ficheiro já existe, vazio) com:
   - `POST /api/v1/issues/{issueId}/comments` → `201 Created`.
   - `GET /api/v1/issues/{issueId}/comments` → lista de `CommentResponse`.
5. Define `NotificationRepository` (interface de domínio, vazia) com
   `save(Notification notification)` e
   `findByRecipientId(UUID recipientId, Pageable pageable)`.
6. Implementa `NotificationRepositoryAdapter` (novo ficheiro), seguindo o
   mesmo padrão do Comment.
7. Implementa `NotificationService` (ficheiro já existe, vazio) com um
   método `listForUser(UUID recipientId, Pageable pageable)`, e
   `NotificationController` (ficheiro já existe, vazio) com
   `GET /api/v1/notifications` (RF-20), extraindo por agora o `recipientId`
   como parâmetro de query — assinala com
   `// TODO(Fase 2): extrair recipientId do SecurityContext (JWT) em vez de
   query param`, seguindo o mesmo padrão de scaffolding já usado em
   `reporterId`.

### Critério de teste

- `POST /api/v1/issues/{issueId}/comments` com um `author` diferente do
  `reporter` da issue devolve `201`; com o mesmo utilizador, devolve `422`
  (RN-06, já mapeada pelo `GlobalExceptionHandler`).
- Após criar um comentário, `GET /api/v1/notifications?recipientId={reporterId}`
  devolve a notificação `COMMENT_ADDED` correspondente, com `status: PENDING`.
- `GET /api/v1/issues/{issueId}/comments` devolve a lista paginada.
- Nenhum destes fluxos depende de Kafka ou RabbitMQ estarem ativos.

### Nota metodológica

Este prompt tem escopo alargado — considera dividi-lo em duas execuções
separadas do agente (Comment primeiro, Notification depois), caso a
experiência mostre que o Cursor perde coerência arquitetural em prompts que
tocam dois módulos simultaneamente. Consulta primeiro o padrão de granularidade
que funcionou melhor em `prompts-fase1-fundacao.md`.

---

## Prompt E — Primeira vaga de testes unitários

### Contexto para o agente

`.cursorrules`, secção 5, exige teste unitário para toda nova funcionalidade
de serviço, e `10-testing-strategy.md` fixa uma cobertura alvo de 90% para a
camada unitária. Atualmente não existe um único ficheiro em
`src/test/java`. Antes de introduzires JWT e autorização por Role na Fase 2,
é necessário ter uma rede de segurança contra regressões nas regras de
negócio já implementadas — nomeadamente RN-01, RN-05 e RN-06, que são
precisamente o tipo de lógica que a introdução de filtros de segurança tende
a perturbar silenciosamente.

Este prompt deve ser executado **depois** dos Prompts C e D, para cobrir a
totalidade da lógica de negócio hoje existente, incluindo o novo código.

### Instrução

Cria testes unitários (JUnit 5 + Mockito, sem Spring context, sem
Testcontainers — pura camada de domínio e de aplicação com dependências
mockadas), na estrutura já prevista em `10-testing-strategy.md`, secção 2.2:

1. `issue/domain/IssueTest.java`:
   - `changeStatus` para `CLOSED` sem descrição lança `IllegalArgumentException`.
   - `changeStatus` numa issue já `CLOSED` lança `IllegalStateException`.
   - `assignTo` atualiza `assignee` e `updatedAt`.
   - `setPriority` atualiza `priority` e `aiConfidenceScore`.
2. `comment/domain/CommentTest.java`:
   - `Comment.create` com `author.equals(issue.getReporter())` lança
     `IllegalArgumentException` (RN-06).
   - `Comment.create` com autor diferente do reporter é bem-sucedido.
3. `notification/domain/NotificationTest.java`:
   - `markAsSent` a partir de `PENDING` funciona; a partir de `SENT` ou
     `FAILED` lança `IllegalStateException`.
   - Idem para `markAsFailed`.
4. `issue/application/CreateIssueUseCaseTest.java`:
   - Mock de `IssueRepository` e `UserRepository`; testar o caminho feliz e
     o caso de `reporterId` inexistente (`IllegalArgumentException`).
5. `user/application/CreateUserUseCaseTest.java`:
   - Mock de `UserRepository` e `PasswordEncoder`; testar duplicação de
     username, duplicação de email, e o caminho feliz (verificar que
     `passwordEncoder.encode` é invocado e que o `User` persistido tem o
     `Role` correto).
6. `shared/exception/GlobalExceptionHandlerTest.java`:
   - Testar que cada `@ExceptionHandler` devolve o `HttpStatus` e o
     `type` (URI) documentados em `06-api-contract.md`, secção 5.

Não escrever testes triviais de getters/setters (conforme `.cursorrules`,
secção 5). Não é necessário, neste prompt, cobrir os use cases criados no
Prompt D (Comment/Notification) — podem ficar para um prompt de continuação,
caso o volume deste prompt já seja considerado suficiente para uma execução
isolada e testável.

### Critério de teste

- `mvn test` executa todos os testes com sucesso.
- Cobertura da camada de domínio (`Issue`, `Comment`, `Notification`)
  aproxima-se do alvo de 90% definido em `10-testing-strategy.md` (validar
  com JaCoCo, se já configurado; caso contrário, apenas confirmar
  manualmente que os cenários de negócio relevantes estão cobertos).
- `docs/STATUS.md` é atualizado para refletir os itens agora testados como
  `✅` em vez de `⚡`.

---

## Resumo de Sequenciamento

| Prompt | Depende de | Bloqueia a Fase 2? |
|---|---|---|
| A — Corrigir migrations.md | Nenhum | Não, mas recomendado primeiro (trivial) |
| B — DTOs para record | Nenhum | Recomendado antes de novos DTOs de auth |
| C — CRUD completo de Issue | Nenhum | Recomendado — evita acumular CRUD na Fase 2 |
| D — Comment e Notification mínimos | C (parcialmente, para consistência) | Recomendado — fecha RF-07/08/15 |
| E — Testes unitários | C e D | **Sim** — rede de segurança antes de JWT |