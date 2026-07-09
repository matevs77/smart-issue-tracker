# Análise de Viabilidade e Plano de Construção

## Verificação de Viabilidade da Stack

Antes de avançar para o planeamento, importa confirmar se a combinação tecnológica proposta é tecnicamente coerente e exequível.

**A stack é viável**, com as seguintes observações técnicas que deves considerar:

| Tecnologia | Viabilidade | Observação |
|---|---|---|
| Java 25+ (Virtual Threads) | ✅ Viável | Virtual Threads foram estabilizadas no Java 21 (JEP 444); Java 25 é LTS e mantém total suporte |
| Spring Boot + Spring AI | ✅ Viável | Spring AI fornece abstrações para integração com modelos LLM (OpenAI, Anthropic, etc.) |
| Kafka + RabbitMQ simultâneos | ⚠️ Viável, mas exige justificação arquitetural | Usar dois brokers de mensagens é incomum; deves justificar a separação de responsabilidades (ver secção 1) |
| Spring Security + JWT | ✅ Viável | Padrão de mercado consolidado |
| JPA/Hibernate + PostgreSQL | ✅ Viável | Combinação madura e amplamente documentada |
| Prometheus + Grafana | ✅ Viável | Integração nativa via Spring Boot Actuator + Micrometer |
| Docker | ✅ Viável | Sem restrições |

**Ponto de atenção**: a coexistência de Kafka e RabbitMQ no mesmo projeto pessoal pode ser vista, numa entrevista técnica, como sobre-engenharia, caso não consigas justificar claramente porque não bastaria um único broker. Recomendo que estejas preparado para explicar esta decisão com um argumento sólido (apresento uma proposta na secção seguinte).

---

## 1. Arquitetura

Proponho uma arquitetura em camadas, orientada a eventos, com separação clara de responsabilidades entre streaming de dados e mensageria de notificações.

### Justificação para uso de Kafka + RabbitMQ

- **Kafka**: destinado ao *streaming* de eventos de domínio de alto volume e persistentes (criação, atualização, mudança de estado de issues). É o *event log* imutável do sistema, permitindo reprocessamento e auditoria.
- **RabbitMQ**: destinado a notificações pontuais, assíncronas e de baixa latência (ex.: "enviar email quando issue for atribuída"), onde não é necessário reter histórico, apenas garantir entrega.

Esta é uma justificação academicamente válida: Kafka funciona como *source of truth* de eventos; RabbitMQ como mecanismo de *fan-out* para tarefas de curta duração.

### Diagrama de Arquitetura (camadas)

```
┌─────────────────────────────────────────────────────────┐
│                     Camada de Apresentação                │
│              (REST Controllers + Spring Security)         │
└────────────────────────┬────────────────────────────────┘
                          │
┌─────────────────────────▼────────────────────────────────┐
│                     Camada de Aplicação                   │
│         (Services, DTOs, Mapeadores, Validações)          │
└──────┬──────────────────┬──────────────────┬─────────────┘
       │                  │                  │
┌──────▼──────┐   ┌───────▼───────┐  ┌───────▼────────┐
│  Spring AI   │   │ Kafka Producer│  │ RabbitMQ        │
│ (Priorização)│   │ (Event Stream)│  │ (Notificações)  │
└──────────────┘   └───────┬───────┘  └───────┬─────────┘
                            │                  │
                   ┌────────▼────────┐ ┌───────▼─────────┐
                   │ Kafka Consumer   │ │ RabbitMQ Consumer│
                   │ (Virtual Threads)│ │(Virtual Threads) │
                   └────────┬─────────┘ └──────────────────┘
                            │
┌────────────────────────────▼──────────────────────────────┐
│                     Camada de Persistência                 │
│              (JPA/Hibernate + PostgreSQL)                  │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│         Observabilidade transversal: Prometheus/Grafana    │
└──────────────────────────────────────────────────────────┘
```

### Fluxo arquitetural resumido

1. Cliente faz requisição REST (autenticada via JWT).
2. `IssueService` persiste a entidade e publica um evento `IssueCreatedEvent` no Kafka.
3. Um consumidor Kafka, executado sobre Virtual Threads, invoca o `AIPriorityService` (Spring AI) para classificar a prioridade.
4. Após classificação, o consumidor publica uma mensagem na fila RabbitMQ para disparar notificação assíncrona.
5. Um consumidor RabbitMQ, também sobre Virtual Threads, processa o envio da notificação (email, webhook, etc.).
6. Métricas de cada etapa são expostas via Actuator e recolhidas pelo Prometheus, visualizadas no Grafana.

---

## 2. Estrutura de Diretórios

Sugiro uma estrutura em **arquitetura hexagonal simplificada** (ports & adapters), organizada por funcionalidade, o que facilita a manutenção e os testes:

```
smart-issue-tracker/
├── src/
│   ├── main/
│   │   ├── java/com/teuprojeto/issuetracker/
│   │   │   ├── config/
│   │   │   │   ├── KafkaConfig.java
│   │   │   │   ├── RabbitMQConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── VirtualThreadConfig.java
│   │   │   │   └── SpringAIConfig.java
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Issue.java
│   │   │   │   │   ├── Comment.java
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Notification.java
│   │   │   │   │   └── Priority.java (enum)
│   │   │   │   └── event/
│   │   │   │       ├── IssueCreatedEvent.java
│   │   │   │       ├── IssueUpdatedEvent.java
│   │   │   │       └── CommentAddedEvent.java
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   ├── IssueRepository.java
│   │   │   │   ├── CommentRepository.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── NotificationRepository.java
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── IssueService.java
│   │   │   │   ├── CommentService.java
│   │   │   │   ├── AIPriorityService.java
│   │   │   │   ├── NotificationService.java
│   │   │   │   └── AuthService.java
│   │   │   │
│   │   │   ├── messaging/
│   │   │   │   ├── kafka/
│   │   │   │   │   ├── producer/IssueEventProducer.java
│   │   │   │   │   └── consumer/IssueEventConsumer.java
│   │   │   │   └── rabbitmq/
│   │   │   │       ├── producer/NotificationProducer.java
│   │   │   │       └── consumer/NotificationConsumer.java
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   ├── IssueController.java
│   │   │   │   ├── CommentController.java
│   │   │   │   ├── AuthController.java
│   │   │   │   └── NotificationController.java
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   │
│   │   │   ├── mapper/
│   │   │   │   ├── IssueMapper.java
│   │   │   │   └── CommentMapper.java
│   │   │   │
│   │   │   ├── security/
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   │
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── IssueNotFoundException.java
│   │   │   │
│   │   │   └── IssueTrackerApplication.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/ (Flyway, se optares por versionar o schema)
│   │
│   └── test/
│       └── java/com/teuprojeto/issuetracker/
│           ├── service/
│           ├── controller/
│           └── integration/
│
├── docker-compose.yml
├── Dockerfile
├── prometheus/
│   └── prometheus.yml
├── grafana/
│   └── dashboards/
├── pom.xml (ou build.gradle)
└── README.md
```

---

## 3. Entidades

Segue a modelação das entidades principais, com as respetivas relações:

### `User`
```java
@Entity
public class User {
    @Id @GeneratedValue
    private UUID id;
    private String username;
    private String email;
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    private Role role; // ADMIN, DEVELOPER, VIEWER
    private Instant createdAt;
}
```

### `Issue`
```java
@Entity
public class Issue {
    @Id @GeneratedValue
    private UUID id;
    private String title;
    private String description;
    @Enumerated(EnumType.STRING)
    private IssueStatus status; // OPEN, IN_PROGRESS, RESOLVED, CLOSED
    @Enumerated(EnumType.STRING)
    private Priority priority; // LOW, MEDIUM, HIGH, CRITICAL
    private Double aiConfidenceScore; // resultado da classificação Spring AI
    @ManyToOne
    private User reporter;
    @ManyToOne
    private User assignee;
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL)
    private List<Comment> comments;
    private Instant createdAt;
    private Instant updatedAt;
}
```

### `Comment`
```java
@Entity
public class Comment {
    @Id @GeneratedValue
    private UUID id;
    @ManyToOne
    private Issue issue;
    @ManyToOne
    private User author;
    private String content;
    private Instant createdAt;
}
```

### `Notification`
```java
@Entity
public class Notification {
    @Id @GeneratedValue
    private UUID id;
    @ManyToOne
    private User recipient;
    private String message;
    @Enumerated(EnumType.STRING)
    private NotificationStatus status; // PENDING, SENT, FAILED
    private Instant createdAt;
}
```

**Nota metodológica**: recomendo que consideres `IssueStatus`, `Priority`, `Role` e `NotificationStatus` como *enums* dedicados, em ficheiros separados dentro de `domain/entity/`, para manter a coesão do modelo.

---

## 4. DTOs

Deves manter uma separação estrita entre entidades de domínio e objetos expostos na API, evitando acoplamento entre a camada de persistência e a camada de apresentação.

### Request DTOs

```java
public record CreateIssueRequest(
    @NotBlank String title,
    @NotBlank String description,
    UUID assigneeId
) {}

public record UpdateIssueStatusRequest(
    @NotNull IssueStatus status
) {}

public record CreateCommentRequest(
    @NotBlank String content
) {}

public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {}
```

### Response DTOs

```java
public record IssueResponse(
    UUID id,
    String title,
    String description,
    IssueStatus status,
    Priority priority,
    Double aiConfidenceScore,
    String reporterUsername,
    String assigneeUsername,
    List<CommentResponse> comments,
    Instant createdAt
) {}

public record CommentResponse(
    UUID id,
    String authorUsername,
    String content,
    Instant createdAt
) {}

public record AuthResponse(
    String token,
    String tokenType,
    long expiresIn
) {}
```

Sugiro o uso de `record` (disponível desde Java 16, plenamente aplicável em Java 25) por seres imutáveis e concisos — apropriados para DTOs.

---

## 5. Fluxo de Regras (Regras de Negócio)

Apresento o fluxo principal do sistema, decomposto nas suas etapas lógicas:

### Fluxo 1 — Criação de Issue com priorização automática

1. Utilizador autenticado (JWT válido) submete `CreateIssueRequest`.
2. `IssueService` valida os dados e persiste a entidade com `status = OPEN` e `priority = null` (ainda não classificada).
3. `IssueService` publica `IssueCreatedEvent` no tópico Kafka `issue-events`.
4. `IssueEventConsumer`, executado numa Virtual Thread, consome o evento.
5. `AIPriorityService` invoca o modelo via Spring AI, enviando título e descrição como contexto.
6. O modelo devolve uma classificação (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) e um `aiConfidenceScore`.
7. `IssueService` atualiza a entidade `Issue` com a prioridade calculada.
8. Um novo evento (`IssuePrioritizedEvent`) é publicado, disparando `NotificationProducer` para a fila RabbitMQ.
9. `NotificationConsumer` processa o envio da notificação ao responsável atribuído.

### Fluxo 2 — Adição de comentário

1. Utilizador submete `CreateCommentRequest` associado a um `issueId`.
2. `CommentService` valida a existência da issue e persiste o comentário.
3. Evento `CommentAddedEvent` é publicado no Kafka (para fins de auditoria/histórico).
4. Notificação assíncrona é enviada ao autor original da issue via RabbitMQ.

### Fluxo 3 — Autenticação

1. Utilizador submete `LoginRequest`.
2. `AuthService` valida credenciais via `UserDetailsServiceImpl`.
3. `JwtTokenProvider` gera token assinado, com tempo de expiração configurável.
4. Requisições subsequentes passam por `JwtAuthFilter`, que valida o token e popula o `SecurityContext`.

### Regras de negócio transversais a considerar

- Apenas utilizadores com `Role.ADMIN` ou `Role.DEVELOPER` podem alterar `status` de uma issue.
- A prioridade calculada pela IA pode ser sobreposta manualmente por um `ADMIN`, devendo o sistema registar essa alteração como auditoria.
- Falhas na chamada ao modelo de IA não devem bloquear a criação da issue (aplicar padrão *circuit breaker* ou *fallback* para prioridade `MEDIUM` por defeito).

---

## 6. Dependências Necessárias (Maven)

```xml
<dependencies>
    <!-- Spring Boot Core -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring AI -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>

    <!-- Persistência -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- RabbitMQ -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- Segurança -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Validação -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Observabilidade -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- Migrações de schema (recomendado) -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- Testes -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>kafka</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>rabbitmq</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Utilitários -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.6.3</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

**Configuração de `pom.xml`**: define `<java.version>25</java.version>` na secção de propriedades, e assegura-te de que o *plugin* do compilador Maven suporta essa versão.

---

## Explicação — Como Construir o Projeto (Roteiro Metodológico)

Segue-se um roteiro sequencial e didático para a construção do projeto, organizado em fases progressivas.

### Fase 1 — Fundação (infraestrutura mínima)

Começa por criar o projeto base com Spring Initializr, apenas com `web`, `data-jpa`, `postgresql` e `validation`. Configura o `docker-compose.yml` com PostgreSQL, e valida que consegues persistir e consultar entidades simples (`User`, `Issue`) através de *endpoints* REST básicos, sem qualquer mensageria ou IA envolvida. Este passo garante que o núcleo funcional está sólido antes de introduzires complexidade.

### Fase 2 — Segurança

Introduz Spring Security com autenticação JWT. Implementa `JwtTokenProvider`, `JwtAuthFilter` e protege os *endpoints* com base em `Role`. Testa exaustivamente os fluxos de login e acesso não autorizado antes de avançar.

### Fase 3 — Virtual Threads

Configura o executor de Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`) e integra-o no Tomcat embebido (via `application.yml`: `spring.threads.virtual.enabled=true`, disponível a partir do Spring Boot 3.2+). Este passo é transversal e beneficia diretamente o processamento de comentários e consumidores de mensageria, que introduzirás nas fases seguintes.

### Fase 4 — Mensageria (Kafka)

Adiciona o *broker* Kafka ao `docker-compose.yml` (juntamente com Zookeeper ou KRaft, consoante a versão). Implementa o produtor e consumidor de eventos de issue. Nesta fase, foca-te apenas na publicação e consumo do evento `IssueCreatedEvent`, sem ainda envolver IA.

### Fase 5 — Mensageria (RabbitMQ)

Adiciona RabbitMQ ao `docker-compose.yml`. Implementa o produtor e consumidor de notificações, simulando o envio (por exemplo, apenas com log) antes de integrares um serviço real de email.

### Fase 6 — Integração com Spring AI

Configura a chave de API do provedor de modelo escolhido (OpenAI, Anthropic, ou outro suportado pelo Spring AI). Implementa `AIPriorityService`, com um *prompt* bem definido que solicite classificação estruturada (idealmente com *structured output*, para obteres diretamente um `enum` de prioridade). Integra este serviço no fluxo do consumidor Kafka, criado na Fase 4.

### Fase 7 — Observabilidade

Adiciona `actuator` e `micrometer-registry-prometheus`. Expõe o endpoint `/actuator/prometheus`. Configura Prometheus para consumir essas métricas (via `prometheus.yml`) e cria dashboards no Grafana para visualizar latência de processamento, taxa de eventos Kafka e volume de notificações RabbitMQ.

### Fase 8 — Testes e validação das métricas alegadas

Para que possas sustentar tecnicamente as afirmações de "60% de redução de latência" e "85% de precisão", deves:
- Implementar testes de carga (ex.: com JMH ou k6) comparando *thread pool* convencional versus Virtual Threads, documentando a metodologia.
- Construir um conjunto de teste rotulado manualmente (ex.: 100-200 issues) para validares a precisão da classificação da IA de forma reprodutível.

### Fase 9 — Containerização final

Finaliza o `Dockerfile` da aplicação e consolida o `docker-compose.yml` com todos os serviços (PostgreSQL, Kafka, RabbitMQ, Prometheus, Grafana, aplicação). Valida que o sistema completo sobe corretamente com `docker-compose up`.

---

Concluindo: a stack que apresentaste é tecnicamente viável e bem escolhida para demonstrar competências avançadas em Java moderno, arquitetura orientada a eventos e integração de IA. O ponto que exige maior atenção da tua parte é a justificação arquitetural do uso simultâneo de Kafka e RabbitMQ, bem como a validação empírica e documentada das métricas de desempenho que pretendes divulgar no teu portefólio.