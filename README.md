# Smart Issue & Task Tracker · Event-Driven + AI-Powered

Sistema orientado a eventos para rastreamento inteligente de tarefas e issues,
com priorização automática assistida por IA.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Stack Tecnológica](#stack-tecnológica)
- [Estrutura de Diretórios](#estrutura-de-diretórios)
- [Pré-requisitos](#pré-requisitos)
- [Como Executar](#como-executar)
- [Roteiro de Desenvolvimento](#roteiro-de-desenvolvimento)
- [Observabilidade](#observabilidade)
- [Testes](#testes)
- [Documentação](#documentação)
- [Licença](#licença)

---

## Visão Geral

Este projeto implementa um sistema de rastreamento de issues e tarefas com as
seguintes capacidades centrais:

- **Streaming de eventos** via Apache Kafka, garantindo auditoria e histórico
  imutável de todas as mudanças de estado das issues.
- **Notificações assíncronas** via RabbitMQ, para comunicação pontual sem
  necessidade de retenção de histórico.
- **Concorrência de alta escala** com Virtual Threads (Project Loom), aplicadas
  ao processamento de consumidores de mensageria.
- **Priorização automática** com Spring AI, com base em análise semântica do título e descrição.
- **Segurança** com Spring Security e autenticação via JWT.
- **Observabilidade** completa com Prometheus e Grafana.

---

## Arquitetura

O sistema segue uma arquitetura **hexagonal modular** orientada a eventos:

```
Cliente REST → Controller → Service → Persistência (PostgreSQL)
                                 │
                                 ├─→ Kafka (eventos de domínio)
                                 │      └─→ Consumer (Virtual Threads) → Spring AI
                                 │
                                 └─→ RabbitMQ (notificações)
                                        └─→ Consumer (Virtual Threads)
```

**Justificação Kafka + RabbitMQ:** Kafka atua como *source of truth* de eventos
de domínio (persistente, reprocessável, auditável). RabbitMQ atua como mecanismo
de *fan-out* para notificações pontuais de curta duração.

---

## Stack Tecnológica

| Categoria | Tecnologia |
|-----------|-----------|
| Linguagem | Java 25+ |
| Framework | Spring Boot 3.3+ |
| IA | Spring AI |
| Streaming de eventos | Apache Kafka 3.7+ |
| Mensageria assíncrona | RabbitMQ 3.13+ |
| Concorrência | Virtual Threads (Project Loom) |
| Persistência | JPA / Hibernate + PostgreSQL 16+ |
| Segurança | Spring Security + JWT |
| Observabilidade | Prometheus + Grafana |
| Containerização | Docker / Docker Compose |
| Build | Maven 3.9+ |

---

## Estrutura de Diretórios

```
smart-issue-tracker/
├── src/main/java/com/teuprojecto/tracker/
│   ├── TrackerApplication.java
│   ├── issue/                  # Módulo de Issues (domínio principal)
│   │   ├── domain/             #   Entidades, enums, interfaces de repositório
│   │   ├── application/        #   Use cases, serviços de aplicação
│   │   ├── infrastructure/     #   JPA, Kafka producers/consumers, Spring AI
│   │   └── presentation/       #   REST controllers, DTOs
│   ├── comment/                # Módulo de Comentários
│   ├── notification/           # Módulo de Notificações (RabbitMQ)
│   ├── user/                   # Módulo de Utilizadores / Auth
│   ├── security/               # Configuração transversal (JWT, filtros)
│   ├── config/                 # Configurações gerais (Kafka, RabbitMQ, VT)
│   └── shared/                 # Código partilhado (eventos, exceções, util)
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/           # Migrações Flyway
│
├── src/test/java/...           # Testes unitários e de integração
│
├── docs/                       # Documentação completa (ver secção abaixo)
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Pré-requisitos

- JDK 25 ou superior
- Maven 3.9+
- Docker e Docker Compose
- Chave de API de um provedor suportado pelo Spring AI (ex.: OpenAI, Anthropic)

---

## Como Executar

```bash
# 1. Clonar o repositório
git clone <url-do-repositorio>
cd smart-issue-tracker

# 2. Configurar variáveis de ambiente
cp .env.example .env
# Preencher .env com as credenciais necessárias (DB, JWT secret, AI API key)

# 3. Subir a infraestrutura (PostgreSQL, Kafka, RabbitMQ, Prometheus, Grafana)
docker compose up -d

# 4. Executar a aplicação
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`.
O Grafana estará disponível em `http://localhost:3000`.

---

## Roteiro de Desenvolvimento

| Fase | Descrição |
|------|-----------|
| 1 | Fundação — CRUD básico de Issue/User com PostgreSQL |
| 2 | Segurança — Autenticação JWT e controlo de acesso por Role |
| 3 | Virtual Threads — Configuração do executor concorrente |
| 4 | Kafka — Publicação e consumo de eventos de domínio |
| 5 | RabbitMQ — Notificações assíncronas |
| 6 | Spring AI — Priorização automática de issues |
| 7 | Observabilidade — Métricas via Prometheus/Grafana |
| 8 | Validação — Testes de carga e validação de precisão da IA |
| 9 | Containerização final — Consolidação do ambiente Docker completo |

---

## Observabilidade

Métricas expostas via Spring Boot Actuator em `/actuator/prometheus`,
consumidas pelo Prometheus e visualizadas em dashboards Grafana pré-configurados
em `grafana/dashboards/`. Métricas monitorizadas incluem:

- Latência de processamento de eventos Kafka
- Taxa de throughput de mensagens RabbitMQ
- Tempo de resposta do serviço de priorização por IA
- Métricas padrão da JVM (incluindo utilização de Virtual Threads)

---

## Testes

```bash
# Testes unitários
./mvnw test

# Testes de integração (requer Docker, via Testcontainers)
./mvnw verify
```

---

## Documentação

Documentação técnica detalhada disponível em [`docs/`](docs/README.md), com:

| Documento | Descrição |
|-----------|-----------|
| `01-requirements.md` | Requisitos, escopo do MVP e fases seguintes |
| `02-architecture.md` | Arquitetura alvo, fluxo end-to-end, diagramas |
| `03-domain-model.md` | Entidades, value objects, regras de negócio |
| `04-data-model.md` | Modelo relacional, constraints, índices |
| `05-migrations.md` | Convenções Flyway e estratégia de evolução do schema |
| `06-api-contract.md` | Endpoints REST, DTOs, modelo de erro |
| `07-security.md` | JWT, hashing, autorização, checklist de segurança |
| `08-messaging.md` | Kafka, RabbitMQ, retry, DLQ, idempotência |
| `09-ai-classification.md` | Integração Spring AI, prompt, fallback |
| `10-testing-strategy.md` | Estratégia de testes e validação de métricas |
| `11-observability-and-runbook.md` | Métricas, dashboards, runbook de incidentes |
| `12-deployment-and-cicd.md` | Pipeline, branches, ambientes |
| `STATUS.md` | Estado atual de implementação |

Decisões arquiteturais registadas em [`docs/adr/`](docs/adr/README.md).

---

## Licença

Projeto pessoal de caráter demonstrativo/portefólio. Licença a definir pelo autor.
