---
status: em revisão
última-atualização: 2026-07-18
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
| Issue domain entity | ✅ | Implementado (Issue.java, 99 linhas, regras de negócio); construtor defende `comments=null` (corrigido NPE em toDomain, ver Nota de Scaffolding) |
| IssueJpaEntity | ✅ | Implementado (68 linhas, entidade JPA completa com índices e CHECK) |
| IssueMapper | ✅ | Implementado (MapStruct, 18 linhas) |
| IssueFilter | ⚡ | Implementado (record com status/priority/reporterId/assigneeId opcionais) |
| IssueStatus enum (shared) | ⚡ | Implementado (shared/domain/IssueStatus.java) |
| IssuePriority enum (shared) | ⚡ | Implementado (shared/domain/IssuePriority.java) |
| IssueRepository interface | ⚡ | Implementado (save, findById, findAll com filtro + paginação) |
| IssueRepositoryAdapter | ⚡ | Implementado (especificações JPA + carregamento de User via UserJpaRepository) |
| CreateIssueUseCase | ⚡ | Implementado (cria Issue, carrega reporter/assignee, persiste) |
| UpdateIssueUseCase | 📋 | Pendente (depende de JWT da Fase 2 para validação de Role); ficheiro vazio (0 linhas) |
| IssueClassificationService | 📋 | Ficheiro vazio (0 linhas) |
| IssueJpaRepository | ✅ | Implementado (extends JpaRepository) |
| IssueEventPublisher | 📋 | Ficheiro não existe (events/ por criar) |
| IssueEventConsumer | 📋 | Ficheiro não existe (events/ por criar) |
| SpringAiClassifier | 📋 | Ficheiro vazio (0 linhas) |
| IssueController | ⚡ | Implementado (POST /, GET /{id}, GET / com filtros e paginação); PATCH/DELETE ainda por implementar (Fase 2) |
| CreateIssueRequest DTO | ⚡ | Implementado (inclui reporterId temporário — ver Nota de Scaffolding) |
| IssueResponse DTO | ⚡ | Implementado (record com UserRef e CommentEntry aninhados) |
| IssueUpdateRequest DTO | 📋 | Ficheiro vazio (0 linhas) |

## Módulo: Comment

| Item | Status | Notas |
|------|--------|-------|
| Comment domain | ⚡ | Implementado (Comment.java, 43 linhas, regras de negócio) |
| Comment application | 📋 | CreateCommentUseCase criado (Prompt C) |
| Comment infrastructure | ⚡ | JPA entity e mapper implementados; JpaRepository criado (Prompt C) |
| Comment presentation | 📋 | Controller criado (Prompt C) |

## Módulo: Notification

| Item | Status | Notas |
|------|--------|-------|
| Notification domain | ⚡ | Implementado (Notification.java, 60 linhas, regras de negócio) |
| Notification application | 📋 | NotificationService criado (Prompt C) |
| Notification infrastructure | ⚡ | JPA entity e mapper implementados; JpaRepository criado (Prompt C) |
| NotificationProducer | 📋 | Ficheiro vazio (0 linhas) |
| NotificationConsumer | 📋 | Ficheiro vazio (0 linhas) |
| NotificationResponse DTO | 📋 | Criado como record (Prompt C) |

## Módulo: User

| Item | Status | Notas |
|------|--------|-------|
| User domain | ⚡ | Implementado (User.java, 47 linhas); UserRepository alargado (save, findById, existsByUsername, existsByEmail); DuplicateUserException criada |
| User application | ⚡ | CreateUserUseCase implementado (valida duplicados, hash BCrypt, persiste) |
| User infrastructure | ⚡ | JPA entity, mapper e JpaRepository implementados; UserRepositoryAdapter implementado (save/existBy*) |
| User presentation | ⚡ | Controller (POST /api/v1/users → 201) e DTOs criados; CreateUserRequest com validação Bean |
| PasswordEncoderConfig | ⚡ | Implementado (security/PasswordEncoderConfig.java, BCrypt força 10) — distinto de SecurityConfig (Fase 2) |

## Segurança

| Item | Status | Notas |
|------|--------|-------|
| SecurityConfig | ⚡ | Implementado (SecurityFilterChain temporário: CSRF desativado, permitAll; ver Nota de Scaffolding) |
| JwtAuthFilter | 📋 | Ficheiro vazio (0 linhas) |
| JwtService | 📋 | Ficheiro vazio (0 linhas) |

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
| GlobalExceptionHandler | ⚡ | Implementado (shared/exception/GlobalExceptionHandler.java — RFC 7807: 400/404/409/422/500) |
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
| reporterId em CreateIssueRequest | Incluído temporariamente como campo obrigatório porque o JWT (Fase 2) ainda não está implementado. `// TODO(Fase 2): remover e extrair do SecurityContext após JWT`. Eliminar quando o SecurityContext estiver operacional. |
| Autorização ADMIN em POST /api/v1/users | RF-13 exige que a criação de utilizadores seja restrita a ADMIN, mas a validação de Role só é possível após a Fase 2 (JWT/SecurityContext). O endpoint está temporariamente sem autenticação/autorização. `// TODO(Fase 2): aplicar Role.ADMIN via SecurityConfig/JwtAuthFilter`. |
| SecurityFilterChain temporário em SecurityConfig | `spring-boot-starter-security` está no classpath; sem SecurityFilterChain definido, o Spring ativaria proteção por omissão. Implementado um `SecurityFilterChain` temporário que desativa CSRF e faz `permitAll()` em todos os pedidos, para permitir testar os endpoints REST da Fase 1. `// TODO(Fase 2): substituir por SecurityFilterChain com JwtAuthFilter e autorização por Role (RN-01, RN-02, RF-04)`. |
| Bug corrigido: NPE em Issue (POST /issues 500) | `Issue.<init>` fazia `new ArrayList<>(comments)`; ao reconstruir o domínio a partir da JPA entity (MapStruct `toDomain` ignora `comments` e passa `null`), lançava `NullPointerException` → 500. Corrigido com guarda nula no construtor de `Issue.java`. |
| Bug corrigido: 404/405 engulfados como 500 | `GlobalExceptionHandler` capturava `NoResourceFoundException` e `HttpRequestMethodNotSupportedException` no handler genérico, devolvendo 500. Adicionados handlers dedicados: 404 NOT_FOUND e 405 METHOD_NOT_ALLOWED (RFC 7807). |

## Documentação

| Item | Status | Notas |
|------|--------|-------|
| docs/ | ✅ | Estrutura completa criada |
| ADR | 🔧 | Decisões a registar |
