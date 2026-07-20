---
status: aceite
última-atualização: 2026-07-20
responsável: matevz77
---

# ADR-06 — Observabilidade Desde o Início

## Contexto

O projeto divulga métricas de desempenho (60% redução de latência, 85% precisão de IA) como diferenciais de portefólio. Para sustentar estas alegações, é necessário que a observabilidade esteja presente desde a primeira linha de código, não como uma reflexão tardia.

## Decisão

**Integrar métricas, health checks e logging estruturado como cidadãos de primeira classe desde o MVP**, e não como funcionalidade de fase 2:

- Métricas customizadas para eventos de domínio (`issue_created_total`, `issue_classification_fallback_total`, etc.).
- Health checks para todas as dependências externas (PostgreSQL, Kafka, RabbitMQ, IA).
- Exposição via Actuator + Prometheus.
- Dashboard Grafana mínimo desde o MVP (pode ser iterado depois).

## Alternativas Consideradas

### Alternativa A: Adicionar observabilidade apenas na fase de operação (pós-MVP)

- **Prós:** menos código no MVP, foco no core business.
- **Contras:** impossível validar métricas de desempenho sem dados desde o início; risco de decisões erradas sobre concorrência sem medição real.
- **Razão para rejeitar:** as métricas são parte do produto, não um extra operacional.

### Alternativa B: Apenas logs (sem métricas ou dashboards)

- **Prós:** simplicidade.
- **Contras:** logs não permitem agregação numérica nem dashboards; inviável para sustentar as alegações de desempenho.
- **Razão para rejeitar:** insuficiente para os objetivos do projeto.

## Consequências

### Positivas

- Métricas disponíveis desde o início para validar decisões arquiteturais.
- Dashboard Grafana funcional desde o MVP.
- Capacidade de correlacionar eventos de domínio com métricas de desempenho.

### Negativas / Trade-offs

- Esforço inicial ligeiramente maior (configuração do Prometheus, criação de métricas).
- Custo de manutenção das métricas (garantir que são actualizadas quando o código muda).
