---
status: estável
última-atualização: 2025-01-XX
responsável: teu nome
---

# 11 — Observabilidade e Runbook

## 1. Actuator

Endpoints expostos (configurados em `application.yml`):

| Endpoint | Função | Público? |
|----------|--------|---------|
| `/actuator/health` | Health check (liveness + readiness) | Sim |
| `/actuator/info` | Informações do build (versão, timestamp) | Sim |
| `/actuator/metrics` | Métricas Micrometer (JVM, threads, cache, etc.) | Não (monitoring network) |
| `/actuator/prometheus` | Métricas no formato Prometheus | Não (scrape target) |
| `/actuator/loggers` | Gestão dinâmica de níveis de log | Não (admin) |

### Health Checks Customizados

- **PostgreSQL**: via `DataSourceHealthIndicator` (automático)
- **Kafka**: via `KafkaHealthIndicator` (automático)
- **RabbitMQ**: via `RabbitHealthIndicator` (automático)
- **Spring AI**: health check customizado que valida conectividade com o provedor LLM (a implementar)

## 2. Prometheus

### 2.1. Métricas Expostas

| Métrica | Tipo | Descrição |
|---------|------|-----------|
| `issue_created_total` | Counter | Total de issues criadas |
| `issue_classified_total` | Counter | Issues classificadas por IA |
| `issue_classification_fallback_total` | Counter | Classificações que usaram fallback |
| `notification_sent_total` | Counter | Notificações enviadas |
| `notification_failed_total` | Counter | Notificações com falha |
| `kafka_consumer_records_lag_max` | Gauge | Lag máximo do consumidor Kafka |
| `rabbitmq_queued_messages` | Gauge | Mensagens pendentes nas filas RabbitMQ |
| `jvm_threads_live_threads` | Gauge | Threads ativas (útil para validar VT) |
| `http_server_requests_seconds_sum` | Summary | Latência das requisições HTTP |

### 2.2. Configuração Prometheus

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'smart-issue-tracker'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
```

## 3. Grafana

Dashboard mínimo (JSON em `grafana/dashboards/`):

- **Painel 1**: Taxa de criação e classificação de issues (série temporal)
- **Painel 2**: Latência de criação de issues (P50, P95, P99)
- **Painel 3**: Métricas Kafka (lag, taxa de produção/consumo)
- **Painel 4**: Métricas RabbitMQ (deepness de filas, taxa de entrega)
- **Painel 5**: Threads (ativas vs. virtuais) e memória JVM

## 4. Logging

| Nível | Package | Justificação |
|-------|---------|-------------|
| DEBUG | com.teuprojecto.tracker | Desenvolvimento local |
| INFO | com.teuprojecto.tracker | Produção |
| WARN | org.springframework.kafka | Produção (alertas de consumo) |
| INFO | org.springframework.boot.actuate | Produção (health checks) |

Logs estruturados (formato JSON) em produção para integração com ferramentas de log centralizado.

## 5. Runbook de Incidentes

### DB Down

| Passo | Ação |
|-------|------|
| 1 | Verificar `GET /actuator/health` — `{"status":"DOWN","components":{"db":{"status":"DOWN"}}}` |
| 2 | `docker compose logs postgres` — verificar se o container está a correr |
| 3 | Verificar espaço em disco: `df -h` |
| 4 | Se container parou: `docker compose restart postgres` |
| 5 | Verificar logs de migração Flyway (podem ter falhado) |

### Kafka Offline

| Passo | Ação |
|-------|------|
| 1 | `GET /actuator/health` → `kafka: DOWN` |
| 2 | `docker compose logs kafka` — verificar erros |
| 3 | `docker compose restart kafka` |
| 4 | Verificar lag dos consumidores via `/actuator/metrics/kafka.consumer.*` |
| 5 | Eventos não perdidos — consumidores retomam do offset atual |

### RabbitMQ Unreachable

| Passo | Ação |
|-------|------|
| 1 | `GET /actuator/health` → `rabbitmq: DOWN` |
| 2 | `docker compose logs rabbitmq` |
| 3 | Verificar management UI em `http://localhost:15672` |
| 4 | `docker compose restart rabbitmq` |
| 5 | Mensagens não entregues acumulam-se na fila; consumidor retoma após restauro |

### IA Provider Fails

| Passo | Ação |
|-------|------|
| 1 | Métrica `issue_classification_fallback_total` aumenta |
| 2 | Verificar log: `WARN ... IA classification failed` |
| 3 | Verificar status do provedor (OpenAI/Anthropic status page) |
| 4 | Se falha intermitente: aguardar e monitorizar fallback |
| 5 | Se prolongada: considerar alterar provedor ou desativar classificação automática |

### Cache Suspeito (Fase 2)

*(A implementar com Caffeine em fase de reports)*

| Passo | Ação |
|-------|------|
| 1 | `GET /actuator/caches` — verificar taxas de hit/miss |
| 2 | Se hit rate < 50%, possivelmente chave mal construída |
| 3 | Forçar invalidação via endpoint de admin |
| 4 | Se evictions excessivas, aumentar tamanho máximo do cache |

### Migração Flyway Falhou

| Passo | Ação |
|-------|------|
| 1 | Verificar log de erro: `FlywayMigrationException` |
| 2 | Consultar `flyway_schema_history` na base: `SELECT * FROM flyway_schema_history WHERE success = false;` |
| 3 | Corrigir manualmente o SQL da migração com falha |
| 4 | Executar `mvn flyway:repair` para marcar a migração como falhada |
| 5 | Reaplicar migrações pendentes |
