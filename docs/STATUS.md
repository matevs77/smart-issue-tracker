---
status: em revisão
última-atualização: 2025-01-XX
responsável: teu nome
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
| Issue domain entity | 📋 | Estrutura criada |
| IssueStatus enum | 📋 | Estrutura criada |
| IssuePriority enum | 📋 | Estrutura criada |
| IssueRepository interface | 📋 | Estrutura criada |
| CreateIssueUseCase | 📋 | Estrutura criada |
| UpdateIssueUseCase | 📋 | Estrutura criada |
| IssueClassificationService | 📋 | Estrutura criada |
| IssueJpaRepository | 📋 | Estrutura criada |
| IssueEventPublisher | 📋 | Estrutura criada |
| IssueEventConsumer | 📋 | Estrutura criada |
| SpringAiClassifier | 📋 | Estrutura criada |
| IssueController | 📋 | Estrutura criada |
| CreateIssueRequest DTO | 📋 | Estrutura criada |
| IssueResponse DTO | 📋 | Estrutura criada |
| IssueUpdateRequest DTO | 📋 | Estrutura criada |

## Módulo: Comment

| Item | Status | Notas |
|------|--------|-------|
| Comment domain | 📋 | Estrutura criada |
| Comment application | 📋 | Estrutura criada |
| Comment infrastructure | 📋 | Estrutura criada |
| Comment presentation | 📋 | Estrutura criada |

## Módulo: Notification

| Item | Status | Notas |
|------|--------|-------|
| Notification domain | 📋 | Estrutura criada |
| Notification application | 📋 | Estrutura criada |
| Notification infrastructure | 📋 | Estrutura criada |
| NotificationProducer | 📋 | Estrutura criada |
| NotificationConsumer | 📋 | Estrutura criada |

## Módulo: User

| Item | Status | Notas |
|------|--------|-------|
| User domain | 📋 | Estrutura criada |
| User application | 📋 | Estrutura criada |
| User infrastructure | 📋 | Estrutura criada |
| User presentation | 📋 | Estrutura criada |

## Segurança

| Item | Status | Notas |
|------|--------|-------|
| SecurityConfig | 📋 | Estrutura criada |
| JwtAuthFilter | 📋 | Estrutura criada |
| JwtService | 📋 | Estrutura criada |

## Configuração

| Item | Status | Notas |
|------|--------|-------|
| KafkaConfig | 📋 | Estrutura criada |
| RabbitMqConfig | 📋 | Estrutura criada |
| VirtualThreadConfig | 📋 | Estrutura criada |
| ObservabilityConfig | 📋 | Estrutura criada |

## Shared

| Item | Status | Notas |
|------|--------|-------|
| DomainEvent | 📋 | Estrutura criada |
| GlobalExceptionHandler | 📋 | Estrutura criada |
| Util classes | 📋 | Pasta vazia |

## Infraestrutura

| Item | Status | Notas |
|------|--------|-------|
| application.yml | 📋 | Ficheiro vazio |
| application-dev.yml | 📋 | Ficheiro vazio |
| application-prod.yml | 📋 | Ficheiro vazio |
| V1__create_issue_table.sql | 📋 | Ficheiro vazio |
| docker-compose.yml | 📋 | Ficheiro vazio |
| Dockerfile | 📋 | Ficheiro vazio |
| pom.xml | 📋 | Ficheiro vazio |

## Documentação

| Item | Status | Notas |
|------|--------|-------|
| docs/ | ✅ | Estrutura completa criada |
| ADR | 🔧 | Decisões a registar |
