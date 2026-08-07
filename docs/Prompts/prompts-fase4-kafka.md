---
status: proposto
última-atualização: 2026-07-30
responsável: matevz77
---

# Prompts de Implementação — Fase 4 (Kafka)

Este ficheiro segue a convenção já validada em `prompts-correcao-pre-fase2.md`,
`prompts-fase2-seguranca.md` e `prompts-fase3-virtual-threads.md`: prompts
sequenciais, cada um independentemente testável, com o raciocínio
arquitetural explicado antes da instrução de implementação, conforme
`.cursorrules`, secção 7. A numeração decimal (`4.x`) estende o padrão já
adotado nas fases anteriores.

**Âmbito da Fase 4** (conforme `README.md` e `inicial.md`, secção "Fase 4 —
Mensageria (Kafka)"): adicionar o *broker* Kafka à infraestrutura do
projeto, e implementar a publicação e o consumo do evento
`IssueCreatedEvent`, reaproveitando o executor de Virtual Threads já
preparado na Fase 3 (`VirtualThreadConfig`, com o seu `// TODO(Fase 4/5)`
ainda por resolver). **Fora de âmbito, explicitamente:**

- Qualquer invocação ao Spring AI dentro do consumidor Kafka — essa
  integração pertence, por definição, à Fase 6, conforme `inicial.md`
  ("foca-te apenas na publicação e consumo do evento `IssueCreatedEvent`,
  sem ainda envolver IA").
- A publicação de `IssueUpdatedEvent`, `CommentAddedEvent` ou
  `IssuePrioritizedEvent` — nenhuma instrução desta fase os exige; ficam
  reservados para quando os respetivos fluxos (Fases 5 e 6) precisarem
  deles, evitando invenção de âmbito (`.cursorrules`, secção 7).
- A integração com RabbitMQ (Fase 5) e a validação formal de desempenho
  (RNF-01, Fase 8).

**Nota sobre decisão arquitetural:** ao contrário da Fase 3 (que apenas
implementou uma ADR já aceite), esta fase introduz três escolhas de
implementação que nenhuma ADR existente cobre — `docs/adr/adr-01-kafka-vs-rabbitmq.md`
justifica *que* o projeto usa Kafka, não *como* o Kafka é configurado.
Essas escolhas são discutidas abaixo e formalizadas como ADR-08 no
fecho da fase (Prompt 4.7), seguindo o mesmo padrão já usado para o
ADR-07 no fecho da Fase 2.

---

## Estado Atual (Auditado)

| Componente | Estado | Nota |
|---|---|---|
| `docker-compose.yml` | 🔧 Só PostgreSQL | Sem serviço Kafka; a adicionar no Prompt 4.1 |
| `pom.xml` | ✅ Pronto | `spring-kafka`, `spring-kafka-test` e `testcontainers:kafka` já são dependências declaradas — nenhuma alteração de `pom.xml` é necessária nesta fase |
| `shared/event/DomainEvent.java` | 📋 Vazio | A implementar no Prompt 4.2 |
| `issue/domain/event/` | 📋 Pacote inexistente | A criar no Prompt 4.2, para `IssueCreatedEvent` |
| `config/KafkaConfig.java` | 📋 Vazio | A implementar no Prompt 4.3 |
| `config/VirtualThreadConfig.java` | ✅ Pronto | Bean `ExecutorService` de Virtual Threads já disponível (Fase 3); `// TODO(Fase 4/5)` explícito a aguardar a injeção nesta fase |
| `issue/infrastructure/messaging/IssueEventPublisher.java` | 📋 Vazio | A implementar no Prompt 4.4 |
| `issue/infrastructure/messaging/IssueEventConsumer.java` | 📋 Vazio | A implementar no Prompt 4.5 |
| `CreateIssueUseCase.java` | ✅ Funcional, mas não publica eventos | A estender no Prompt 4.4 |
| `docs/08-messaging.md` | ⚠️ Desenho-alvo, com uma incoerência a corrigir | Já documenta tópicos, estrutura de eventos, produtores/consumidores e o exemplo de Virtual Threads — mas a secção 3 (retry) descreve, para o Kafka, uma ação de esgotamento ("log + skip") divergente da prática nativa do Spring Kafka; ver Decisão 3 |
| `docs/10-testing-strategy.md`, secção 3.3 | ✅ Já prevê | Exemplo de teste de integração Kafka+PostgreSQL com Testcontainers, a concretizar no Prompt 4.6 |
| `docs/adr/adr-01-kafka-vs-rabbitmq.md` | ✅ Aceite | Justifica a existência do Kafka no projeto, mas não cobre decisões de implementação (modo do cluster, serialização, retry) — ver ADR-08, Prompt 4.7 |

---

## Decisões Arquiteturais Desta Fase

### Decisão 1 — Modo do cluster Kafka: KRaft, sem Zookeeper

**Contexto:** `inicial.md` deixa a escolha em aberto ("juntamente com
Zookeeper ou KRaft, consoante a versão"). O projeto exige Kafka 3.7+
(`docs/01-requirements.md`, secção 2).

**Decisão:** usar o modo KRaft (`confluentinc/cp-kafka:7.6.0` com
`KAFKA_PROCESS_ROLES=broker,controller`, sem serviço Zookeeper).

**Razão:** o KRaft é modo de produção suportado desde o Kafka 3.3, e o
Apache Kafka já assinalou o Zookeeper para remoção numa versão futura.
Para um único *broker* de desenvolvimento/portefólio, o KRaft elimina um
contentor inteiro do `docker-compose.yml` sem qualquer perda funcional, o
que também simplifica a consolidação final prevista na Fase 9.

**Alternativa considerada — Zookeeper:** rejeitada por ser o modo legado,
já em vias de remoção pelo próprio projeto Apache Kafka, e por acrescentar
um contentor adicional sem benefício demonstrável num cluster de um único
*broker*.

### Decisão 2 — Serialização das mensagens: JSON, sem Schema Registry

**Contexto:** `docs/08-messaging.md`, secção 1.2, já documenta os eventos
como objetos JSON (`eventId`, `eventType`, `timestamp`, `payload`).

**Decisão:** usar `JsonSerializer`/`JsonDeserializer` do Spring Kafka, sem
Avro nem Protobuf.

**Razão:** mantém coerência direta com o contrato já documentado; evita
introduzir um Schema Registry (ex.: Confluent Schema Registry) — infraestrutura
adicional desproporcional para um monólito modular de portefólio, sem
múltiplos serviços independentes que justifiquem governança formal de
esquemas.

**Alternativa considerada — Avro + Schema Registry:** *prós:* evolução de
esquema mais rigorosa, formato binário mais compacto. *Contras:* exige um
contentor adicional e configuração de compatibilidade de esquemas.
*Razão para rejeitar:* o benefício não se justifica sem múltiplos serviços
consumidores independentes; pode ser reconsiderada em ADR futura caso o
projeto venha a exigir governança formal de contratos entre serviços.

### Decisão 3 — Retry com `@RetryableTopic` e Dead Letter Topic (correção de deriva documental)

**Contexto:** `docs/08-messaging.md`, secção 3, tabela de estratégia de
retry, descreve para o Kafka: "Ação após esgotar: Log + skip (evento
permanece no tópico)". Esta descrição diverge do comportamento nativo e
idiomático do `@RetryableTopic` do Spring Kafka, que, por omissão, publica
a mensagem falhada num tópico *dead-letter* (sufixo `-dlt`) após esgotar as
tentativas — não a deixa, de facto, "no tópico original" sem qualquer
rasto. Seguindo o princípio já registado como aprendizagem-chave deste
projeto ("verificação empírica sobre confiança documental"), esta
divergência deve ser corrigida antes de codificar: implementar
literalmente "log + skip, sem DLT" exigiria suprimir deliberadamente uma
funcionalidade nativa e bem testada do *framework*, sem benefício
aparente.

**Decisão:** adotar `@RetryableTopic`, com 4 tentativas no total (1
original + 3 retries, conforme já documentado) e *backoff* exponencial
(1s, 2s, 4s), com a estratégia por omissão de Dead Letter Topic automático
(`issue-events-dlt`). Isto alinha o comportamento do Kafka com o já usado
para o RabbitMQ (DLQ + alerta), em vez de os tratar de forma assimétrica.
`docs/08-messaging.md`, secção 3, é corrigido no Prompt 4.7 para refletir
esta escolha.

**Alternativa A — manter "log + skip" sem DLT:** *contras:* exigiria um
`ErrorHandler` customizado apenas para suprimir o comportamento nativo, sem
qualquer rastreabilidade de mensagens falhadas. *Razão para rejeitar:*
complexidade acrescida sem benefício, e perda de observabilidade.

**Alternativa B — DLT automático via `@RetryableTopic` (escolhida):**
idiomática, observável, sem código de infraestrutura adicional.

---

## Prompt 4.1 — Kafka em modo KRaft no `docker-compose.yml`

### Contexto para o agente

O `docker-compose.yml` atual só contém o serviço `postgres`.
`docs/12-deployment-and-cicd.md`, secção 5, já lista `kafka` com a imagem
`confluentinc/cp-kafka:7.6.0` na porta `9092`, mas sem detalhar o modo do
cluster. Conforme a Decisão 1, adota-se KRaft: um único *broker* que
acumula simultaneamente os papéis de `broker` e `controller`, dispensando
Zookeeper.

### Instrução

1. Adiciona ao `docker-compose.yml` um serviço `kafka`, imagem
   `confluentinc/cp-kafka:7.6.0`, com as variáveis de ambiente mínimas para
   modo KRaft de nó único: `KAFKA_NODE_ID`, `KAFKA_PROCESS_ROLES=broker,controller`,
   `KAFKA_LISTENERS` (incluindo um *listener* `CONTROLLER` interno),
   `KAFKA_ADVERTISED_LISTENERS` (acessível como `localhost:9092` a partir
   do anfitrião), `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP`,
   `KAFKA_CONTROLLER_LISTENER_NAMES`, `KAFKA_CONTROLLER_QUORUM_VOTERS` e
   `CLUSTER_ID` (um identificador base64 fixo, gerado uma única vez — a
   imagem `cp-kafka` aceita `CLUSTER_ID` diretamente, sem exigir o
   utilitário `kafka-storage.sh` manualmente). Define também
   `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1`, por se tratar de um único
   *broker*.
2. Expõe a porta `9092:9092` e adiciona um volume `kafka-data` para
   persistência entre reinícios, seguindo o mesmo padrão já usado para
   `postgres-data`.
3. Não adiciones nenhum serviço `zookeeper` — a Decisão 1 exclui-o
   explicitamente.
4. Atualiza `docs/12-deployment-and-cicd.md`, secção 5 (tabela de
   serviços), acrescentando uma nota de que o `kafka` corre em modo KRaft
   de nó único, sem Zookeeper.

### Critério de teste

- `docker compose up -d kafka` arranca sem exigir nenhum outro serviço
  (nomeadamente sem Zookeeper) e sem erros no log do contentor.
- `docker exec tracker-kafka kafka-topics --bootstrap-server localhost:9092 --list`
  executa com sucesso (ainda que devolva uma lista vazia, confirma que o
  *broker* está operacional).

---

## Prompt 4.2 — Eventos de domínio: `DomainEvent` e `IssueCreatedEvent`

### Contexto para o agente

`shared/event/DomainEvent.java` existe como ficheiro vazio desde a
estrutura inicial do projeto. `.cursorrules`, secção 4, exige que eventos
de domínio sejam imutáveis; o padrão já estabelecido no projeto para
objetos imutáveis é o `record` (ver `docs/03-domain-model.md`, secção 4,
e todos os DTOs já convertidos no Prompt B da correção pré-Fase 2). A
estrutura do evento já está definida em `docs/08-messaging.md`, secção
1.2, e deve ser respeitada sem divergência, para que a Fase 6 (que virá a
consumir este mesmo evento para classificação) não exija retrabalho.

### Instrução

1. Implementa `shared/event/DomainEvent.java` como uma interface mínima,
   com três métodos de acesso: `UUID eventId()`, `String eventType()` e
   `Instant timestamp()`. Esta interface é o contrato comum a todos os
   eventos de domínio futuros (`IssueUpdatedEvent`, `CommentAddedEvent`,
   `IssuePrioritizedEvent`, entre outros, quando forem necessários em
   fases posteriores — não os crias agora, por não serem exigidos por
   nenhuma instrução desta fase).
2. Cria o pacote `issue/domain/event/` e, dentro dele,
   `IssueCreatedEvent.java`, como `record` que implementa `DomainEvent`,
   com a seguinte forma sugerida (para espelhar exatamente a estrutura
   JSON já documentada em `08-messaging.md`, secção 1.2):
   - Campos de topo: `UUID eventId`, `String eventType`, `Instant timestamp`,
     `IssueCreatedPayload payload`.
   - Um `record` aninhado `IssueCreatedPayload(UUID issueId, String title,
     String description, UUID reporterId)`.
   - Um método estático de fábrica, `IssueCreatedEvent.from(Issue issue)`,
     que gera um novo `eventId` (`UUID.randomUUID()`), define
     `eventType = "ISSUE_CREATED"`, `timestamp = Instant.now()`, e constrói
     o `payload` a partir dos dados da `Issue` já persistida.
3. Não crias ainda `IssueUpdatedEvent`, `CommentAddedEvent` nem
   `IssuePrioritizedEvent` — ficam fora de âmbito desta fase, conforme
   assinalado na introdução deste ficheiro.

### Critério de teste

- O projeto compila (`mvn compile`).
- `IssueCreatedEvent.from(issue)` produz um evento cujo `payload` contém
  exatamente os quatro campos documentados em `08-messaging.md`, sem
  campos adicionais nem em falta.

---

## Prompt 4.3 — `KafkaConfig`: tópico, serialização JSON e integração com Virtual Threads

### Contexto para o agente

Este prompt resolve, finalmente, o `// TODO(Fase 4/5)` deixado em
`VirtualThreadConfig.java` desde a Fase 3 — a parte relativa ao Kafka.
`docs/08-messaging.md`, secção 1.5, já documenta o padrão de referência:
injetar o executor de Virtual Threads no
`ConcurrentKafkaListenerContainerFactory`. É importante notar que a API do
Spring Kafka para esta injeção espera um tipo `TaskExecutor` (ou
equivalente), não um `java.util.concurrent.ExecutorService` bruto — o
*bean* já existente (Fase 3) deve, por isso, ser adaptado (por exemplo,
via `org.springframework.core.task.support.TaskExecutorAdapter`), e não
injetado diretamente. Confirma o método exato disponível na versão do
Spring Kafka trazida pelo Spring Boot 3.4.4 antes de escreveres o código
definitivo.

### Instrução

1. Em `application.yml`, adiciona uma secção `spring.kafka` com:
   `bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`;
   propriedades do *producer* (`key-serializer` String,
   `value-serializer` `JsonSerializer`, `acks: all`, e
   `properties.enable.idempotence: true`, conforme RNF-06 e
   `08-messaging.md`, secção 4); propriedades do *consumer*
   (`group-id: issue-classification-group`, `auto-offset-reset: earliest`,
   `key-deserializer` String, `value-deserializer` `JsonDeserializer`, e
   `properties."spring.json.trusted.packages": com.teuprojecto.tracker.issue.domain.event`).
2. Implementa `config/KafkaConfig.java` com:
   - Um `@Bean NewTopic` para `issue-events`, com 3 partições e fator de
     replicação 1 (cluster de um único *broker*), conforme
     `08-messaging.md`, secção 1.1.
   - Um `@Bean ConcurrentKafkaListenerContainerFactory<String, IssueCreatedEvent>`
     que injeta o `ExecutorService` de `VirtualThreadConfig` (Fase 3),
     devidamente adaptado ao tipo exigido pela API do Spring Kafka.
3. Em `config/VirtualThreadConfig.java`, atualiza o comentário
   `// TODO(Fase 4/5)`: remove a referência à Fase 4 (agora concluída) e
   mantém apenas a referência à Fase 5 (RabbitMQ), ainda pendente.
4. Não crias `RabbitMqConfig.java` nem qualquer conteúdo relativo a
   RabbitMQ — permanece corretamente fora de âmbito nesta fase.

### Critério de teste

- O projeto compila e a aplicação arranca com o novo *bean* disponível no
  contexto Spring (`mvn spring-boot:run`, com `docker compose up -d`
  a incluir o `kafka` já configurado no Prompt 4.1).
- O tópico `issue-events` é criado automaticamente no arranque (confirmar
  via `kafka-topics --describe --topic issue-events`), com 3 partições.

---

## Prompt 4.4 — `IssueEventPublisher` e publicação em `CreateIssueUseCase`

### Contexto para o agente

Com o `KafkaConfig` pronto, este prompt liga a criação de uma *issue* à
publicação do evento correspondente. Vale a pena assinalar, com
transparência, uma limitação conhecida desta implementação: a persistência
da `Issue` no PostgreSQL e a publicação no Kafka não são atómicas (não
existe, nesta fase, um padrão *transactional outbox*); uma falha entre os
dois passos pode resultar numa *issue* persistida sem o evento
correspondente publicado. Esta é uma limitação aceite deliberadamente para
o âmbito atual do projeto — assinalada como *trade-off* negativo na
ADR-08 (Prompt 4.7) — e não bloqueia esta fase.

### Instrução

1. Implementa `issue/infrastructure/messaging/IssueEventPublisher.java`,
   injetando um `KafkaTemplate<String, IssueCreatedEvent>` (disponibilizado
   automaticamente pela autoconfiguração do Spring Boot a partir das
   propriedades `spring.kafka.producer.*` já definidas). Expõe um método
   `void publishIssueCreated(Issue issue)`, que constrói o evento via
   `IssueCreatedEvent.from(issue)` e o publica no tópico `issue-events`,
   usando `issue.getId().toString()` como chave da mensagem (garante que
   eventos da mesma *issue* ficam na mesma partição, preservando ordem).
2. Em caso de falha na publicação, regista um `log.error(...)` com o
   `issueId`, mas **não** relança a exceção — a criação da *issue* não deve
   falhar por indisponibilidade do Kafka (mesma filosofia de resiliência
   já aplicada à falha do Spring AI em ADR-05, ainda que por razões
   distintas).
3. Em `CreateIssueUseCase`, injeta `IssueEventPublisher` como nova
   dependência (via construtor) e invoca `publishIssueCreated(issue)`
   **depois** de `issueRepository.save(issue)` ter sido bem-sucedido.
4. Atualiza `CreateIssueUseCaseTest`: adiciona um `@Mock` para
   `IssueEventPublisher`, e verifica (`verify(...)`) que
   `publishIssueCreated` é invocado com a `Issue` persistida, no cenário
   de caminho feliz já existente.

### Critério de teste

- `mvn test` passa, incluindo o `CreateIssueUseCaseTest` atualizado.
- `POST /api/v1/issues` (autenticado) continua a devolver `201 Created`
  como antes; uma mensagem correspondente aparece no tópico
  `issue-events` (confirmar via `kafka-console-consumer --topic issue-events --from-beginning`).

---

## Prompt 4.5 — `IssueEventConsumer` com retry e Dead Letter Topic (scaffolding para a Fase 6)

### Contexto para o agente

Conforme a Decisão 3, o consumidor implementa `@RetryableTopic` com DLT
automático, corrigindo a deriva documental identificada em
`08-messaging.md`. Conforme o âmbito desta fase, o consumidor **não**
invoca o Spring AI nem atualiza a prioridade da *issue* — limita-se a
confirmar a receção do evento de forma observável, deixando o ponto de
extensão explicitamente assinalado para a Fase 6.

### Instrução

1. Implementa `issue/infrastructure/messaging/IssueEventConsumer.java`
   com um método anotado `@KafkaListener(topics = "issue-events",
   groupId = "issue-classification-group", containerFactory = "...")`
   (o nome do *bean* de fábrica definido no Prompt 4.3), recebendo um
   `IssueCreatedEvent`.
2. Anota o mesmo método com `@RetryableTopic(attempts = "4", backoff =
   @Backoff(delay = 1000, multiplier = 2.0))`, reproduzindo os intervalos
   de 1s, 2s e 4s já documentados em `08-messaging.md`, secção 3 (4
   tentativas no total: 1 original + 3 retries). Não configures
   `dltTopicSuffix` nem `dltStrategy` explicitamente — a estratégia por
   omissão do Spring Kafka (DLT automático, sufixo `-dlt`) é exatamente a
   escolhida na Decisão 3.
3. No corpo do método, regista um log estruturado (nível INFO) com o
   `eventId`, o `issueId` do `payload`, e uma mensagem clara de que a
   receção foi confirmada — sem qualquer chamada a `AIPriorityService`
   nem a `IssueRepository` para atualização de prioridade.
4. Adiciona o comentário `// TODO(Fase 6): invocar AIPriorityService,
   classificar a prioridade da issue e publicar IssuePrioritizedEvent`,
   seguindo a convenção de scaffolding já usada no projeto.
5. Não implementas ainda `IssueClassificationService` nem
   `SpringAiClassifier` — ambos permanecem, corretamente, vazios até à
   Fase 6.

### Critério de teste

- Uma *issue* criada via `POST /api/v1/issues` gera, nos *logs* da
  aplicação, a confirmação de receção do evento pelo consumidor.
- Provocar deliberadamente uma falha no consumidor (por exemplo, lançando
  uma exceção não controlada de forma temporária, só para teste manual)
  confirma que a mensagem é reencaminhada 3 vezes com os intervalos
  esperados, e termina no tópico `issue-events-dlt` — reverte esta
  instrumentação de teste manual assim que confirmado.

---

## Prompt 4.6 — Teste de integração Kafka (Testcontainers)

### Contexto para o agente

`docs/10-testing-strategy.md`, secção 3.3, já prevê este cenário
(`IssueFlowIntegrationTest`, combinando PostgreSQL e Kafka via
Testcontainers). Este prompt concretiza a parte do fluxo já implementada
até aqui — criação de *issue* → publicação → consumo — sem ainda incluir
RabbitMQ (Fase 5) nem IA (Fase 6), seguindo o mesmo padrão de
`AuthFlowIntegrationTest` (Fase 2) para configuração de propriedades
dinâmicas via `@DynamicPropertySource`.

### Instrução

1. Cria `src/test/java/.../issue/infrastructure/messaging/IssueEventFlowIntegrationTest.java`,
   com `@SpringBootTest(webEnvironment = RANDOM_PORT)` e `@Testcontainers`,
   usando `PostgreSQLContainer` (tal como em `AuthFlowIntegrationTest`) e
   um `KafkaContainer` (imagem `confluentinc/cp-kafka:7.6.0`, ou a imagem
   Testcontainers dedicada ao Kafka, conforme já usada em
   `docs/10-testing-strategy.md`, secção 3.3).
2. Regista as propriedades dinâmicas necessárias
   (`spring.datasource.*`, `spring.kafka.bootstrap-servers`, e o
   *placeholder* de Flyway já usado em `AuthFlowIntegrationTest`, dado que
   o fluxo de criação de *issue* exige um utilizador autenticado).
3. O teste deve: autenticar-se como `admin` (reaproveitando o padrão de
   login já validado em `AuthFlowIntegrationTest`); criar uma *issue* via
   `POST /api/v1/issues`; e confirmar que o evento correspondente é
   publicado e consumido — usa um componente de teste auxiliar (por
   exemplo, um `@KafkaListener` de teste que acumula eventos recebidos
   numa `LinkedBlockingQueue`, aguardando com um `poll(timeout)`) para
   validar a receção pelo consumidor real, evitando depender apenas da
   inspeção de logs.
4. Não incluas neste teste qualquer asserção sobre prioridade da *issue*
   — o consumidor desta fase não a altera.

### Critério de teste

- `mvn test` executa este novo teste com sucesso, sem depender de um
  ambiente Kafka já em execução fora do Testcontainers.
- O teste falha de forma clara e diagnosticável se o evento não for
  publicado ou não for consumido dentro do tempo limite definido.

---

## Prompt 4.7 — ADR-08 e fecho documental da Fase 4

### Contexto para o agente

Fecho da fase, seguindo o mesmo padrão já usado no Prompt 2.11 (Fase 2):
formalizar como ADR as decisões desta fase que têm alternativas
consideradas e consequências reais (Decisões 1 a 3 deste ficheiro), e
sincronizar toda a documentação afetada — incluindo a correção da deriva
identificada em `08-messaging.md`.

### Instrução

1. Cria `docs/adr/adr-08-kafka-implementacao.md`, a partir de
   `docs/adr/template.md`, documentando as três Decisões desta fase
   (modo KRaft, serialização JSON sem Schema Registry, retry via
   `@RetryableTopic` com DLT automático), reaproveitando o texto já
   elaborado na secção "Decisões Arquiteturais Desta Fase". Inclui, na
   secção de consequências negativas, a limitação do problema de
   escrita dupla (persistência PostgreSQL + publicação Kafka não
   atómicas, Prompt 4.4) como *trade-off* aceite nesta fase.
2. Atualiza `docs/adr/README.md`, adicionando a entrada
   `| 08 | [Implementação do Kafka: KRaft, serialização e retry](adr-08-kafka-implementacao.md) | Aceite |`.
3. Corrige `docs/08-messaging.md`, secção 3: substitui, na coluna Kafka
   da tabela de "Ação após esgotar", "Log + skip (evento permanece no
   tópico)" por "Move para tópico *dead-letter* (`issue-events-dlt`) +
   alerta", alinhando a descrição com a implementação real (Decisão 3) e
   com o comportamento já documentado para o RabbitMQ. Atualiza também o
   campo `última-atualização` do cabeçalho.
4. Atualiza `docs/STATUS.md`:
   - Secção "Módulo: Issue": adiciona `IssueEventPublisher` e
     `IssueEventConsumer` (atualmente listados como "ficheiro não
     existe") com o estado `⚡`, e uma nota a referir a Fase 6 como
     extensão pendente do consumidor.
   - Secção "Configuração": atualiza `KafkaConfig` de `📋` para `⚡`.
   - Secção "Shared": atualiza `DomainEvent` de `📋` para `⚡`, e
     acrescenta uma linha para `IssueCreatedEvent` (novo).
   - Secção "Infraestrutura": atualiza `docker-compose.yml`, referindo
     agora a inclusão do serviço `kafka` em modo KRaft.
5. Revê `README.md`, secção "Roteiro de Desenvolvimento": confirma que a
   descrição da Fase 4 ("Kafka — Publicação e consumo de eventos de
   domínio") permanece precisa; nenhuma alteração estrutural é esperada.
6. Não alteres `docs/09-ai-classification.md` nem `docs/10-testing-strategy.md`
   nesta fase — pertencem, respetivamente, à Fase 6 e à validação formal
   da Fase 8, e já estão corretos face ao âmbito atual.

### Critério de teste

- `docs/STATUS.md`, `docs/08-messaging.md` e `docs/adr/README.md` refletem
  fielmente o estado real do código após esta fase.
- `docs/adr/adr-08-kafka-implementacao.md` está completo, com contexto,
  decisão, alternativas consideradas e consequências — incluindo o
  *trade-off* da escrita dupla.
- Revisão cruzada confirma que `VirtualThreadConfig.java` já não contém
  nenhuma referência pendente à Fase 4 no seu comentário de scaffolding.

---

## Resumo de Sequenciamento

| Prompt | Depende de | Bloqueia o fecho da Fase 4? |
|---|---|---|
| 4.1 — Kafka (KRaft) no `docker-compose.yml` | Nenhum | Sim |
| 4.2 — `DomainEvent` e `IssueCreatedEvent` | Nenhum (pode correr em paralelo com 4.1) | Sim |
| 4.3 — `KafkaConfig` (tópico, JSON, Virtual Threads) | 4.1, 4.2 | Sim |
| 4.4 — `IssueEventPublisher` + `CreateIssueUseCase` | 4.3 | Sim |
| 4.5 — `IssueEventConsumer` (retry + DLT) | 4.3 | Sim |
| 4.6 — Teste de integração (Testcontainers) | 4.4, 4.5 | Sim |
| 4.7 — ADR-08 e fecho documental | Todos os anteriores | Sim |

---

## Critérios de Saída da Fase 4

Espelhando `docs/01-requirements.md`, secção 8, e o padrão de fecho já
aplicado às Fases 2 e 3, a Fase 4 considera-se concluída quando:

1. O *broker* Kafka corre em modo KRaft, sem Zookeeper, integrado no
   `docker-compose.yml`.
2. Toda a criação de *issue* publica um `IssueCreatedEvent` válido no
   tópico `issue-events`, consumido de forma observável pelo
   `IssueEventConsumer`, sem qualquer invocação ao Spring AI.
3. O consumidor usa o executor de Virtual Threads já preparado na
   Fase 3, resolvendo definitivamente a parte Kafka do
   `// TODO(Fase 4/5)`.
4. Falhas de consumo são reencaminhadas com *backoff* exponencial e
   terminam num tópico *dead-letter*, sem perda silenciosa de mensagens.
5. `docs/STATUS.md`, `docs/08-messaging.md` e `docs/adr/` refletem
   fielmente o estado real do código, incluindo a correção da deriva
   documental identificada na Decisão 3.
6. Nenhuma regressão foi introduzida nos fluxos já validados nas Fases 1
   a 3 (confirmar via `mvn test` completo).