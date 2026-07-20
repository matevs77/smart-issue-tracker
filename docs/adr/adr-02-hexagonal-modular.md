---
status: aceite
última-atualização: 2026-07-20
responsável: matevz77
---

# ADR-02 — Arquitetura Hexagonal Modular por Funcionalidade

## Contexto

O projeto precisa de uma organização de código que:
- Separe claramente domínio, aplicação, infraestrutura e apresentação.
- Permita que cada funcionalidade (issue, comment, notification) evolua de forma independente.
- Facilite testes unitários (domínio puro, sem dependências de infraestrutura).

## Decisão

Adotar **arquitetura hexagonal (ports & adapters)**, organizada por **módulos funcionais**, onde cada módulo contém as suas próprias camadas:

```
issue/
├── domain/          # Entidades, interfaces de repositório, eventos de domínio
├── application/     # Use cases, serviços de aplicação, DTOs de entrada/saída
├── infrastructure/  # Implementações JPA, Kafka producers/consumers, RabbitMQ
└── presentation/    # REST controllers
```

## Alternativas Consideradas

### Alternativa A: Arquitetura em camadas tradicional (controller → service → repository)

- **Prós:** familiar à maioria dos programadores Spring.
- **Contras:** acoplamento entre camadas, difícil de isolar domínio para testes, tendência a misturar responsabilidades.
- **Razão para rejeitar:** não suporta o nível de isolamento que o projeto exige para o domínio.

### Alternativa B: Arquitetura monolítica com separação por pacote técnico (domain/, service/, controller/)

- **Prós:** estrutura simples.
- **Contras:** à medida que o projeto cresce, a pasta `domain/` acumula entidades não relacionadas; a pasta `service/` torna-se god class.
- **Razão para rejeitar:** não escala bem com múltiplas funcionalidades.

## Consequências

### Positivas

- Domínio puro e testável sem infraestrutura.
- Módulos desacoplados (cada um com as suas dependencies).
- Facilita a navegação do código (cada funcionalidade está num sítio).

### Negativas / Trade-offs

- Maior número de ficheiros e pacotes.
- Pode parecer excessivo para domínios pequenos (ex.: comment, notification).
- Requer mapeadores (MapStruct) entre domínio e JPA.
