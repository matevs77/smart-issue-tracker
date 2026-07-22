---
status: em revisão
última-atualização: 2026-07-22
responsável: matevz77

---

# STATUS — Estado Atual de Implementação

> **Aviso:** Este documento é volátil e deve ser atualizado sempre que uma funcionalidade for implementada ou alterada.

## Legenda

| Símbolo | Significado |
|---------|-------------|
| ✅ | Implementado e testado |
| ⚡ | Implementado mas sem testes |
| 🔧 | Em desenvolvimento |
| 📋 | Planeado (não iniciado) |

## Módulo: Issue

| Item | Status | Notas |
|------|--------|-------|
| Issue domain entity | ✅ | Implementado (Issue.java, 99 linhas, regras de negócio); testado unitariamente (IssueTest, Prompt E); construtor defende `comments=null` (corrigido NPE em toDomain, ver Nota de Scaffolding) |
| IssueJpaEntity | ✅ | Implementado (68 linhas, entidade JPA completa com índices e CHECK) |
| IssueMapper | ✅ | Implementado (MapStruct, 18 linhas) |
| IssueFilter | ⚡ | Implementado (record com status/priority/reporterId/assigneeId opcionais) |
| IssueStatus enum (shared) | ⚡ | Implementado (shared/domain/IssueStatus.java) |
| IssuePriority enum (shared) | ⚡ | Implementado (shared/domain/IssuePriority.java) |
| IssueRepository interface | ⚡ | Implementado (save, findById, deleteById, findAll com filtro + paginação) |
| IssueRepositoryAdapter | ⚡ | Implementado (especificações JPA + carregamento de User via UserJpaRepository + deleteById) |
| CreateIssueUseCase | ✅ | Implementado (cria Issue, carrega reporter/assignee, persiste); testado unitariamente (CreateIssueUseCaseTest, Prompt E) |
| UpdateIssueUseCase | ⚡ | Implementado (changeStatus, overridePriority, reassign, updateDetails); autorização de Role pendente da Fase 2 (ver Nota de Scaffolding) |
| DeleteIssueUseCase | ⚡ | Implementado (remove issue em cascata); autorização de Role pendente da Fase 2 (ver Nota de Scaffolding) |
| IssueClassificationService | 📋 | Ficheiro vazio (0 linhas) |
| IssueJpaRepository | ✅ | Implementado (extends JpaRepository) |
| IssueEventPublisher | 📋 | Ficheiro não existe (events/ por criar) |
| IssueEventConsumer | 📋 | Ficheiro não existe (events/ por criar) |
| SpringAiClassifier | 📋 | Ficheiro vazio (0 linhas) |
| IssueController | ✅ | Implementado (POST /, GET /{id}, GET / com filtros e paginação; PATCH /{id}/status, /priority, /assignee, /details e DELETE /{id}); autorização por `@PreAuthorize` concluída na Fase 2 |
| CreateIssueRequest DTO | ✅ | Implementado (sem reporterId — extraído do SecurityContext via @AuthenticationPrincipal; Fase 2) |
| IssueResponse DTO | ⚡ | Implementado (record com UserRef e CommentEntry aninhados) |
| ChangeStatusRequest DTO | ⚡ | Implementado (record, Prompt C) |
| OverridePriorityRequest DTO | ⚡ | Implementado (record, Prompt C) |
| ReassignRequest DTO | ⚡ | Implementado (record, Prompt C) |
| UpdateDetailsRequest DTO | ⚡ | Implementado (record, Prompt C) |

## Módulo: Comment

| Item | Status | Notas |
|------|--------|-------|
| Comment domain | ✅ | Implementado (Comment.java, 43 linhas, regras de negócio; RN-06 validada em `create`); testado unitariamente (CommentTest, Prompt E) |
| Comment application | ⚡ | CreateCommentUseCase implementado (Prompt D): carrega Issue+Author, valida RN-06, persiste e cria Notification síncrona (RF-08) |
| Comment infrastructure | ⚡ | JPA entity, mapper e JpaRepository implementados; CommentRepository (domínio) com save/findByIssueId; CommentRepositoryAdapter implementado (reidrata issue+author) |
| Comment presentation | ✅ | CommentController implementado (POST /api/v1/issues/{issueId}/comments → 201; GET → lista paginada); CreateCommentRequest sem authorId (extraído do SecurityContext via @AuthenticationPrincipal; Fase 2) |
| RF-07 (criar comentário) | ⚡ | Implementado via CreateCommentUseCase + CommentController |
| RF-15 (listar comentários) | ⚡ | Implementado via GET /api/v1/issues/{issueId}/comments |

## Módulo: Notification

| Item | Status | Notas |
|------|--------|-------|
| Notification domain | ✅ | Implementado (Notification.java, 60 linhas, regras de negócio; markAsSent/markAsFailed); testado unitariamente (NotificationTest, Prompt E) |
| Notification application | ⚡ | NotificationService implementado (Prompt D): listForUser(UUID, Pageable) |
| Notification infrastructure | ⚡ | JPA entity, mapper e JpaRepository implementados; NotificationRepository (domínio) com save/findByRecipientId; NotificationRepositoryAdapter implementado (reidrata recipient) |
| NotificationProducer | 📋 | Ficheiro vazio (0 linhas) |
| NotificationConsumer | 📋 | Ficheiro vazio (0 linhas) |
| NotificationResponse DTO | ⚡ | record com factory `from(Notification)` (Prompt D) |
| RF-08 (notificar autor de issue) | ⚡ | Implementado de forma síncrona (persistência direta da Notification em CreateCommentUseCase); substituir por RabbitMQ na Fase 5 (ver Nota de Scaffolding) |
| RF-20 (listar notificações) | ✅ | Implementado via GET /api/v1/notifications (recipientId extraído do SecurityContext via @AuthenticationPrincipal; Fase 2) |

## Módulo: User

| Item | Status | Notas |
|------|--------|-------|
| User domain | ⚡ | Implementado (User.java, 47 linhas); UserRepository alargado (save, findById, existsByUsername, existsByEmail); DuplicateUserException criada |
| User application | ✅ | CreateUserUseCase implementado (valida duplicados, hash BCrypt, persiste); testado unitariamente (CreateUserUseCaseTest, Prompt E) |
| User infrastructure | ⚡ | JPA entity, mapper e JpaRepository implementados; UserRepositoryAdapter implementado (save/existBy*) |
| User presentation | ⚡ | Controller (POST /api/v1/users → 201) e DTOs criados; CreateUserRequest com validação Bean |
| PasswordEncoderConfig | ⚡ | Implementado (security/PasswordEncoderConfig.java, BCrypt força 10) — distinto de SecurityConfig (Fase 2) |

## Módulo: Segurança

| Item | Status | Notas |
|------|--------|-------|
| SecurityConfig | ✅ | Implementado (SecurityFilterChain com JWT, STATELESS, CORS, `@EnableMethodSecurity`; Fase 2) |
| JwtAuthFilter | ✅ | Implementado (OncePerRequestFilter: extração e validação de token Bearer, população do SecurityContext; Fase 2); testado unitariamente (JwtAuthFilterTest) |
| JwtService | ✅ | Implementado (geração e validação de tokens HMAC-SHA256, extração de claims; Fase 2); testado unitariamente (JwtServiceTest) |
| UserDetailsServiceImpl | ✅ | Implementado (carrega User por username, devolve AuthenticatedUserDetails; Fase 2) |
| AuthenticatedUserDetails | ✅ | Implementado (UserDetails wrapper com getId(), getRole(); Fase 2) |
| AuthenticatedPrincipal | ✅ | Implementado (record com id + username; Fase 2) |
| RestAuthenticationEntryPoint | ✅ | Implementado (resposta 401 RFC 7807 para falhas de autenticação; Fase 2) |
| AuthController | ✅ | Implementado (POST /api/v1/auth/login; Fase 2) |
| AuthService | ✅ | Implementado (login com AuthenticationManager + JwtService; Fase 2); testado unitariamente (AuthServiceTest) |
| V5__seed_default_admin_user.sql | ✅ | Implementado (bootstrap do utilizador ADMIN com hash parametrizado; Fase 2) |
| InvalidTokenException | ✅ | Implementado (exceção específica para tokens inválidos/expirados; Fase 2) |
| JwtProperties | ✅ | Implementado (record @ConfigurationProperties para security.jwt; Fase 2) |
| AuthFlowIntegrationTest | ✅ | Teste de integração (fluxo login -> token -> endpoint protegido com Testcontainers; Fase 2) |

## Configuração

| Item | Status | Notas |
|------|--------|-------|
| KafkaConfig | 📋 | Ficheiro vazio (0 linhas) |
| RabbitMqConfig | 📋 | Ficheiro vazio (0 linhas) |
| VirtualThreadConfig | 📋 | Ficheiro vazio (0 linhas) |
| ObservabilityConfig | 📋 | Ficheiro vazio (0 linhas) |

## Shared

| Item | Status | Notas |
|------|--------|-------|
| IssuePriority | ⚡ | Implementado (shared/domain/, enum) |
| IssueStatus | ⚡ | Implementado (shared/domain/, enum) |
| NotificationStatus | ⚡ | Implementado (shared/domain/, enum) |
| NotificationType | ⚡ | Implementado (shared/domain/, enum) |
| Role | ⚡ | Implementado (shared/domain/, enum) |
| DomainEvent | 📋 | Ficheiro vazio (shared/event/DomainEvent.java) |
| GlobalExceptionHandler | ✅ | Implementado (shared/exception/GlobalExceptionHandler.java — RFC 7807: 400/404/409/422/500); testado unitariamente (GlobalExceptionHandlerTest, Prompt E) |
| IssueNotFoundException | ⚡ | Implementado (shared/exception/IssueNotFoundException.java — mapeada para 404) |
| Util classes | 📋 | Pasta vazia |

## Infraestrutura

| Item | Status | Notas |
|------|--------|-------|
| TrackerApplication | ⚡ | Implementado (classe de arranque com @SpringBootApplication e main, Prompt 1.4) |
| application.yml | ⚡ | Implementado (nome, porta 8080, Flyway baseline-on-migrate, virtual threads false) |
| application-dev.yml | ⚡ | Implementado (PostgreSQL localhost:5432, credenciais por env com defaults, ddl-auto=validate) |
| application-prod.yml | 📋 | Ficheiro vazio (reservado para Fase 9) |
| V1__create_user_table.sql | ✅ | Implementado (cria tb_users, Prompt A) |
| V2__create_issue_table.sql | ✅ | Implementado (cria tb_issues com FKs, CHECK e índices, Prompt A) |
| V3__create_comment_table.sql | ✅ | Implementado (cria tb_comments, FK issue_id ON DELETE CASCADE, FK author_id, índices, Prompt 1.4) |
| V4__create_notification_table.sql | ✅ | Implementado (cria tb_notifications, CHECK type/status, índices, Prompt 1.4) |
| docker-compose.yml | ⚡ | Implementado (apenas postgres:16 com volume postgres-data; sem Kafka/RabbitMQ/Prometheus) |
| Dockerfile | 📋 | Ficheiro vazio |
| pom.xml | ✅ | Ficheiro completo com todas as dependências (Spring Boot, Kafka, RabbitMQ, Flyway, Testcontainers, etc.) |

## Notas de Scaffolding

| Nota | Descrição |
|------|-----------|
| Notificação síncrona em CreateCommentUseCase (RF-08) | RF-08 implementada com persistência direta da Notification (sem RabbitMQ) porque o consumidor assíncrono pertence à Fase 5. `// TODO(Fase 5): substituir persistência direta por publicação RabbitMQ + NotificationConsumer`. |
| Bug corrigido: NPE em Issue (POST /issues 500) | `Issue.<init>` fazia `new ArrayList<>(comments)`; ao reconstruir o domínio a partir da JPA entity (MapStruct `toDomain` ignora `comments` e passa `null`), lançava `NullPointerException` → 500. Corrigido com guarda nula no construtor de `Issue.java`. |
| Bug corrigido: 404/405 engulfados como 500 | `GlobalExceptionHandler` capturava `NoResourceFoundException` e `HttpRequestMethodNotSupportedException` no handler genérico, devolvendo 500. Adicionados handlers dedicados: 404 NOT_FOUND e 405 METHOD_NOT_ALLOWED (RFC 7807). |

## Documentação

| Item | Status | Notas |
|------|--------|-------|
| docs/ | ✅ | Estrutura completa criada |
| ADR | ✅ | ADR-01 a ADR-06 propostos; ADR-07 (JWT bootstrap e claims) aceite |
