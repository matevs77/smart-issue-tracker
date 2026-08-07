---
status: aceite
última-atualização: 2026-08-07
responsável: matevz77
---

# ADR-08 — Implementação do Kafka: KRaft, serialização e retry

## Contexto

O projeto já justificava *que* usa Kafka (`docs/adr/adr-01-kafka-vs-rabbitmq.md`), mas não documentava *como* o Kafka é configurado. A Fase 4 introduziu três decisões de implementação não cobertas por nenhuma ADR existente: o modo do cluster, a serialização das mensagens e a estratégia de retry.

## Decisão 1 — Modo do cluster: KRaft, sem Zookeeper

**Contexto:** `inicial.md` deixava a escolha em aberto ("juntamente com Zookeeper ou KRaft, consoante a versão").

**Decisão:** usar o modo KRaft (`confluentinc/cp-kafka:7.6.0` com `KAFKA_PROCESS_ROLES=broker,controller`, sem serviço Zookeeper).

**Razão:** o KRaft é modo de produção suportado desde o Kafka 3.3, e o Apache Kafka já assinalou o Zookeeper para remoção. Para um único *broker* de desenvolvimento/portefólio, o KRaft elimina um contentor inteiro do `docker-compose.yml` sem perda funcional.

### Alternativa considerada — Zookeeper

- **Prós:** modo legado, amplamente documentado.
- **Contras:** contentor adicional, projeto Kafka planeia removê-lo.
- **Razão para rejeitar:** complexidade acrescida sem benefício num cluster de nó único.

## Decisão 2 — Serialização das mensagens: JSON, sem Schema Registry

**Contexto:** `docs/08-messaging.md`, secção 1.2, já documenta os eventos como objetos JSON (`eventId`, `eventType`, `timestamp`, `payload`).

**Decisão:** usar `JsonSerializer`/`JsonDeserializer` do Spring Kafka, sem Avro nem Protobuf.

**Razão:** mantém coerência direta com o contrato já documentado; evita introduzir um Schema Registry — infraestrutura adicional desproporcional para um monólito modular de portefólio, sem múltiplos serviços independentes que justifiquem governança formal de esquemas.

### Alternativa considerada — Avro + Schema Registry

- **Prós:** evolução de esquema mais rigorosa, formato binário mais compacto.
- **Contras:** exige um contentor adicional e configuração de compatibilidade de esquemas.
- **Razão para rejeitar:** o benefício não se justifica sem múltiplos serviços consumidores independentes.

## Decisão 3 — Retry com `@RetryableTopic` e Dead Letter Topic

**Contexto:** `docs/08-messaging.md`, secção 3, descrevia para o Kafka: "Log + skip (evento permanece no tópico)". Esta descrição diverge do comportamento nativo e idiomático do `@RetryableTopic` do Spring Kafka, que, por omissão, publica a mensagem falhada num tópico *dead-letter* (sufixo `-dlt`) após esgotar as tentativas.

**Decisão:** adotar `@RetryableTopic`, com 4 tentativas no total (1 original + 3 retries) e *backoff* exponencial (1s, 2s, 4s), com a estratégia por omissão de Dead Letter Topic automático (`issue-events-dlt`). Isto alinha o comportamento do Kafka com o já usado para o RabbitMQ (DLQ + alerta).

### Alternativa A — manter "log + skip" sem DLT

- **Contras:** exigiria um `ErrorHandler` customizado apenas para suprimir o comportamento nativo, sem qualquer rastreabilidade de mensagens falhadas.
- **Razão para rejeitar:** complexidade acrescida sem benefício, e perda de observabilidade.

### Alternativa B — DLT automático via `@RetryableTopic` (escolhida)

- **Prós:** idiomática, observável, sem código de infraestrutura adicional.
- **Contras:** nenhuma relevante.

## Consequências

### Positivas

- Cluster Kafka simplificado (sem Zookeeper), menor footprint no `docker-compose.yml`.
- Serialização JSON mantém coerência com a documentação de eventos existente.
- Retry com DLT garante rastreabilidade de mensagens falhadas, alinhado com a estratégia RabbitMQ.
- Consumidor executa sobre Virtual Threads (via `kafkaListenerContainerFactory`), resolvendo o `// TODO(Fase 4/5)`.

### Negativas / Trade-offs

- **Problema de escrita dupla (non-atomic write):** a persistência da `Issue` no PostgreSQL e a publicação no Kafka não são atómicas (não existe, nesta fase, um padrão *transactional outbox*); uma falha entre os dois passos pode resultar numa *issue* persistida sem o evento correspondente publicado. Esta é uma limitação aceite deliberadamente para o âmbito atual do projeto e não bloqueia esta fase.
- Sem Schema Registry, a evolução de esquema depende de coordenação manual — aceitável enquanto houver um único produtor/consumidor.

## Referências

- `docs/adr/adr-01-kafka-vs-rabbitmq.md` — justifica a existência do Kafka
- `docs/08-messaging.md` — desenho-alvo de mensageria (corrigido na Fase 4)
- `config/KafkaConfig.java` — implementação do tópico e fábrica de listeners
- `issue/infrastructure/messaging/IssueEventPublisher.java` — produtor
- `issue/infrastructure/messaging/IssueEventConsumer.java` — consumidor com retry/DLT
