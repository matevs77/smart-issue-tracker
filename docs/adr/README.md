---
status: estável
última-atualização: 2025-01-XX
responsável: teu nome
---

# Architecture Decision Records (ADR)

Este diretório regista as decisões arquiteturais do projeto Smart Issue Tracker, seguindo o formato ADR (Architecture Decision Record).

## O que é um ADR?

Um ADR é um documento conciso que regista:
- **Contexto**: qual era o problema ou situação que exigia uma decisão.
- **Decisão**: o que foi decidido e porquê.
- **Alternativas consideradas**: outras opções que foram avaliadas e rejeitadas.
- **Consequências**: impacto positivo e negativo da decisão.

## Índice

| # | Decisão | Estado |
|---|---------|--------|
| 01 | [Uso simultâneo de Kafka e RabbitMQ](adr-01-kafka-vs-rabbitmq.md) | Proposto |
| 02 | [Arquitetura hexagonal modular por funcionalidade](adr-02-hexagonal-modular.md) | Proposto |
| 03 | [Separação entre entidades de domínio e JPA](adr-03-domain-separated-from-jpa.md) | Proposto |
| 04 | [Virtual Threads para concorrência](adr-04-virtual-threads.md) | Proposto |
| 05 | [Fallback na classificação por IA](adr-05-spring-ai-fallback.md) | Proposto |
| 06 | [Observabilidade desde o início](adr-06-observability-first.md) | Proposto |

## Como propor um novo ADR

1. Copiar o ficheiro `template.md` para `adr-NN-titulo.md`.
2. Preencher todas as secções.
3. Atualizar este `README.md` com a nova entrada.
