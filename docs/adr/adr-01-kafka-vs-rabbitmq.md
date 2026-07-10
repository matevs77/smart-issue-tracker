---
status: proposto
última-atualização: 2026-07-09
responsável: matevz77
---

# ADR-01 — Uso Simultâneo de Kafka e RabbitMQ

## Contexto

O sistema precisa de dois padrões de mensageria distintos:
1. **Streaming de eventos de domínio** para auditoria, reprocessamento e event sourcing (criação/atualização de issues, comentários).
2. **Notificações assíncronas pontuais** para informar utilizadores (issue atribuída, novo comentário), onde a latência é crítica mas a retenção histórica não é necessária.

A stack original do projeto previa Kafka + RabbitMQ. Foi levantada a questão de ser over-engineering usar dois brokers num projeto pessoal.

## Decisção

Manter os dois brokers, com responsabilidades claramente separadas:

- **Kafka**: event log imutável para eventos de domínio. Tópicos com retenção configurável (7 dias), permitindo reprocessamento e auditoria.
- **RabbitMQ**: notificações transitórias com entrega garantida, sem necessidade de replay. Filas com DLQ para tratamento de falhas.

## Alternativas Consideradas

### Alternativa A: Apenas Kafka

- **Prós:** broker único, simplifica operação; Kafka também suporta filas (consumer groups).
- **Contras:** Kafka é sub-ótimo para notificações pontuais (latência ligeiramente maior, overhead de partições); não tem dead-letter nativo (requer configuração adicional).
- **Razão para rejeitar:** Kafka foi desenhado para streaming, não para work queues. Para notificações, RabbitMQ oferece melhor latência e semântica mais natural.

### Alternativa B: Apenas RabbitMQ

- **Prós:** broker único, mais simples operacionalmente.
- **Contras:** RabbitMQ não foi desenhado para event sourcing — consumir eventos retrospetivamente (reprocessamento) é mais difícil; não há offsets para controlar replay.
- **Razão para rejeitar:** Perder a capacidade de reprocessamento e auditoria de eventos compromete um requisito não-funcional do projeto.

### Alternativa C: Um broker com duas estratégias (ex.: Redis Streams + RabbitMQ, ou Kafka com filas de retry)

- **Prós:** reduz o número de tecnologias.
- **Contras:** empurra os limites de cada broker para fora do seu caso de uso ideal.
- **Razão para rejeitar:** Prefere-se usar a ferramenta certa para cada trabalho, mesmo que sejam duas.

## Consequências

### Positivas

- Separação clara de responsabilidades: streaming vs notificação.
- Cada broker opera no seu domínio ideal.
- Demonstra conhecimento de duas tecnologias de mensageria distintas.

### Negativas / Trade-offs

- Complexidade operacional (dois brokers para configurar e monitorizar).
- Maior consumo de recursos no `docker-compose.yml`.
- Exige documentação clara para justificar a decisão em contexto de entrevista.

## Referências

- `02-architecture.md` — Diagrama de arquitetura e justificação detalhada
- `08-messaging.md` — Configuração detalhada de tópicos, filas, retry e DLQ
