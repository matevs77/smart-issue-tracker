---
status: aceite
última-atualização: 2026-07-20
responsável: matevz77
---

# ADR-04 — Virtual Threads para Concorrência

## Contexto

O sistema tem operações I/O-bound em vários pontos críticos:
- Consumidores Kafka (leitura de tópicos, chamada à IA, escrita na base).
- Consumidores RabbitMQ (leitura de filas, persistência de notificações).
- Requisições HTTP com operações de base de dados e mensageria.

O objetivo é demonstrar ganhos de desempenho com Virtual Threads (Project Loom) face a thread pools convencionais.

## Decisão

**Ativar Virtual Threads no Spring Boot** (`spring.threads.virtual.enabled=true`) e configurar consumidores Kafka e RabbitMQ para executarem em Virtual Threads.

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, IssueEvent> factory() {
    var factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.getContainerProperties().setConsumerTaskExecutor(
        Executors.newVirtualThreadPerTaskExecutor()
    );
    return factory;
}
```

## Alternativas Consideradas

### Alternativa A: Thread pool convencional (platform threads)

- **Prós:** comportamento previsível, bem compreendido, sem necessidade de configuração adicional.
- **Contras:** escalabilidade limitada; cada thread bloqueada numa operação I/O consome um thread do SO; para 200 requisições concorrentes com I/O, seria necessário um pool grande, consumindo muita memória.
- **Razão para rejeitar:** o projeto pretende demonstrar a vantagem de Virtual Threads como diferencial técnico.

### Alternativa B: WebFlux (reativo)

- **Prós:** alto throughput com poucos threads.
- **Contras:** stack reativo (Mono/Flux) tem curva de aprendizagem; depuração mais difícil; não se integra naturalmente com JDBC/JPA (requer drivers reativos).
- **Razão para rejeitar:** Virtual Threads oferecem escalabilidade semelhante com programação imperativa familiar.

## Consequências

### Positivas

- Maior throughput com menos consumo de memória.
- Código imperativo simples, sem paradigma reativo.
- Redução mensurável de latência (esperada: 60% face a pool convencional).

### Negativas / Trade-offs

- Depende de Java 21+ (garantido com Java 25).
- Virtual Threads não são adequadas para tarefas CPU-bound (aplicação é I/O-bound, por isso é adequado).
- Pode exigir ajuste fino de pooling de conexões (ex.: HikariCP com max pool size = número de VT não é viável).
