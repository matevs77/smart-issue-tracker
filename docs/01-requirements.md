---
status: estável
última-atualização: 2026-07-09
responsável: matevz77
---

# 01 — Requisitos

Este documento define o escopo funcional e não-funcional do projeto, delimitando claramente o que pertence ao MVP (versão mínima viável) e o que fica reservado para fases seguintes. Serve como referência principal para decisões de priorização durante o desenvolvimento.

## 1. Objetivo do Projeto

Construir um sistema de rastreamento de issues e tarefas que demonstre, de forma prática e verificável, a aplicação de arquitetura orientada a eventos e classificação automática de prioridade via IA, num contexto tecnicamente realista — adequado tanto para uso pessoal como para apresentação em portefólio técnico.

## 2. Stack Tecnológica Exigida

Esta é a stack obrigatória, já validada quanto à viabilidade técnica. Qualquer alteração deve ser registada como ADR (ver `docs/adr/`).

| Componente | Tecnologia | Versão mínima |
|------------|-----------|---------------|
| Linguagem | Java | 25 (LTS) |
| Framework | Spring Boot | 3.3+ |
| IA | Spring AI | Última estável compatível com Spring Boot 3.3+ |
| Streaming de eventos | Apache Kafka | 3.7+ |
| Mensageria assíncrona | RabbitMQ | 3.13+ |
| Concorrência | Virtual Threads (Project Loom) | Nativo desde Java 21 |
| Persistência | JPA / Hibernate + PostgreSQL | PostgreSQL 16+ |
| Segurança | Spring Security + JWT | — |
| Observabilidade | Prometheus + Grafana | — |
| Containerização | Docker / Docker Compose | — |
| Build | Maven | 3.9+ |

## 3. Requisitos Funcionais

### 3.1. Gestão de Issues

| ID | Requisito |
|----|-----------|
| RF-01 | O sistema deve permitir criar uma issue com título, descrição e responsável (opcional) |
| RF-02 | O sistema deve classificar automaticamente a prioridade de uma issue recém-criada, usando IA |
| RF-03 | O sistema deve permitir alterar o estado de uma issue (OPEN, IN_PROGRESS, RESOLVED, CLOSED) |
| RF-04 | Apenas utilizadores com Role.ADMIN ou Role.DEVELOPER podem alterar o estado de uma issue |
| RF-05 | Um ADMIN pode sobrepor manualmente a prioridade calculada pela IA |
| RF-06 | Toda alteração manual de prioridade deve ser registada para fins de auditoria |

### 3.2. Comentários

| ID | Requisito |
|----|-----------|
| RF-07 | O sistema deve permitir adicionar comentários a uma issue existente |
| RF-08 | O autor original da issue deve ser notificado ao receber um novo comentário |

### 3.3. Notificações

| ID | Requisito |
|----|-----------|
| RF-09 | O sistema deve notificar o responsável atribuído quando uma issue for classificada |
| RF-10 | O envio de notificações deve ser assíncrono e não deve bloquear o fluxo principal |

### 3.4. Autenticação e Autorização

| ID | Requisito |
|----|-----------|
| RF-11 | O sistema deve autenticar utilizadores via credenciais (username/password) e emitir token JWT |
| RF-12 | O sistema deve restringir o acesso a endpoints consoante o Role do utilizador autenticado |

## 4. Requisitos Não-Funcionais

| ID | Requisito | Critério de Aceitação |
|----|-----------|----------------------|
| RNF-01 | Desempenho de concorrência | Redução mensurável de latência com Virtual Threads face a thread pool convencional, sob carga controlada e documentada (ver `10-testing-strategy.md`) |
| RNF-02 | Precisão da classificação de IA | Precisão validada contra um conjunto de teste rotulado manualmente, com metodologia reprodutível (ver `10-testing-strategy.md`) |
| RNF-03 | Resiliência a falhas externas | Falha na chamada ao modelo de IA não deve impedir a criação da issue (fallback obrigatório) |
| RNF-04 | Observabilidade | Toda operação crítica (criação de issue, classificação, notificação) deve expor métricas via Prometheus |
| RNF-05 | Segurança de dados | Nenhuma credencial ou segredo deve ser armazenado em texto plano no código-fonte |
| RNF-06 | Idempotência | Consumidores Kafka e RabbitMQ devem tratar reentregas sem duplicar efeitos colaterais (ver `08-messaging.md`) |

## 5. Escopo do MVP

O MVP corresponde ao conjunto mínimo de funcionalidades necessárias para demonstrar o fluxo completo do sistema, de ponta a ponta, com todas as tecnologias da stack integradas — ainda que de forma simples.

**Incluído no MVP:**
- CRUD de issues e comentários.
- Autenticação JWT com três roles (ADMIN, DEVELOPER, VIEWER).
- Publicação e consumo de eventos de domínio via Kafka.
- Classificação automática de prioridade via Spring AI, com fallback.
- Notificação assíncrona via RabbitMQ (envio simulado por log, sem integração real de email).
- Observabilidade básica: métricas expostas via Actuator, consumidas por Prometheus, com pelo menos um dashboard Grafana funcional.
- Containerização completa via Docker Compose (aplicação + todas as dependências de infraestrutura).

**Explicitamente fora do MVP** (ver secção 6):
- Envio real de notificações por email ou webhook.
- Dashboard de utilizador (frontend).
- Multi-tenancy.

## 6. Fase 2 — Funcionalidades Diferidas

As funcionalidades seguintes são reconhecidamente valiosas, mas foram deliberadamente excluídas do MVP para não comprometer o prazo de entrega de um sistema funcional e coerente de ponta a ponta.

| Funcionalidade | Justificação do adiamento |
|---------------|--------------------------|
| Notificações por email real (SMTP/SendGrid) | O MVP valida o fluxo de mensageria; a integração externa é um detalhe de infraestrutura, não de arquitetura |
| Webhooks para integrações externas (Slack, Jira) | Depende de um catálogo de eventos já estabilizado, o que só ocorre após o MVP |
| Dashboard de utilizador (frontend web) | Fora do escopo de backend que o projeto pretende demonstrar |
| Multi-tenancy | Introduz complexidade significativa no modelo de dados e na segurança; requer decisão arquitetural própria (candidato a ADR futuro) |
| Refresh token com rotação | O MVP usa apenas access token com expiração curta; rotação de refresh token é uma melhoria de segurança de fase 2 |
| Auditoria completa (histórico de todas as alterações) | O MVP regista apenas auditoria da sobreposição manual de prioridade (RF-06); um sistema de auditoria genérico é mais abrangente |

## 7. Fora de Escopo (não planeado)

Para evitar ambiguidade, os seguintes itens não fazem parte do roteiro do projeto, mesmo a longo prazo, salvo decisão explícita em contrário:

- Aplicação mobile nativa.
- Suporte a múltiplos idiomas na interface (i18n).
- Migração para arquitetura de microsserviços distribuídos (o projeto mantém-se como aplicação modular monolítica).

## 8. Critérios de Sucesso do Projeto

O projeto considera-se bem-sucedido quando:

1. O fluxo completo (criação de issue → classificação IA → notificação) funciona de ponta a ponta em ambiente containerizado local.
2. As métricas de desempenho e precisão divulgadas no portefólio estão sustentadas por testes documentados e reprodutíveis.
3. A documentação técnica (esta pasta `docs/`) reflete fielmente o estado real do código, validado através do `STATUS.md`.

## 9. Referências Cruzadas

- Para o detalhe arquitetural do fluxo descrito na secção 3, consultar `02-architecture.md`.
- Para a metodologia de validação dos requisitos não-funcionais RNF-01 e RNF-02, consultar `10-testing-strategy.md`.
- Para o racional da exclusão de multi-tenancy do MVP, consultar `adr/README.md` (a registar como ADR, caso a decisão seja retomada).
