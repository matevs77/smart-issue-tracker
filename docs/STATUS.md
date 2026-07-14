---
status: em revisão
última-atualização: 2026-07-14
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
| Issue domain entity | ✅ | Implementado (Issue.java, 99 linhas, regras de negócio) |
| IssueJpaEntity | ✅ | Implementado (68 linhas, entidade JPA completa com índices e CHECK) |
| IssueMapper | ✅ | Implementado (MapStruct, 18 linhas) |
| IssueStatus enum (shared) | ⚡ | Implementado (shared/domain/IssueStatus.java) |
| IssuePriority enum (shared) | ⚡ | Implementado (shared/domain/IssuePriority.java) |
| IssueRepository interface | 📋 | Ficheiro vazio (0 linhas) |
| CreateIssueUseCase | 📋 | Ficheiro vazio (0 linhas) |
| UpdateIssueUseCase | 📋 | Ficheiro vazio (0 linhas) |
| IssueClassificationService | 📋 | Ficheiro vazio (0 linhas) |
| IssueJpaRepository | ✅ | Implementado (extends JpaRepository) |
| IssueEventPublisher | 📋 | Ficheiro não existe (events/ por criar) |
| IssueEventConsumer | 📋 | Ficheiro não existe (events/ por criar) |
| SpringAiClassifier | 📋 | Ficheiro vazio (0 linhas) |
| IssueController | 📋 | Ficheiro vazio (0 linhas) |
| CreateIssueRequest DTO | 📋 | Ficheiro vazio (0 linhas) |
| IssueResponse DTO | 📋 | Ficheiro vazio (0 linhas) |
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
| User domain | ⚡ | Implementado (User.java, 47 linhas) |
| User application | 📋 | CreateUserUseCase criado (Prompt C) |
| User infrastructure | ⚡ | JPA entity e mapper implementados; JpaRepository criado (Prompt C) |
| User presentation | 📋 | Controller e DTOs criados (Prompt C) |

## Segurança

| Item | Status | Notas |
|------|--------|-------|
| SecurityConfig | 📋 | Ficheiro vazio (0 linhas) |
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
| GlobalExceptionHandler | 📋 | Ficheiro vazio (shared/exception/GlobalExceptionHandler.java) |
| Util classes | 📋 | Pasta vazia |

## Infraestrutura

| Item | Status | Notas |
|------|--------|-------|
| application.yml | 📋 | Ficheiro vazio |
| application-dev.yml | 📋 | Ficheiro vazio |
| application-prod.yml | 📋 | Ficheiro vazio |
| V1__create_user_table.sql | ✅ | Implementado (cria tb_users, Prompt A) |
| V2__create_issue_table.sql | ✅ | Implementado (cria tb_issues com FKs, CHECK e índices, Prompt A) |
| docker-compose.yml | 📋 | Ficheiro vazio |
| Dockerfile | 📋 | Ficheiro vazio |
| pom.xml | ✅ | Ficheiro completo com todas as dependências (Spring Boot, Kafka, RabbitMQ, Flyway, Testcontainers, etc.) |

## Documentação

| Item | Status | Notas |
|------|--------|-------|
| docs/ | ✅ | Estrutura completa criada |
| ADR | 🔧 | Decisões a registar |
