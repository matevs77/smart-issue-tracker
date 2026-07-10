---
status: estável
última-atualização: 2026-07-09
responsável: matevz77
---

# Documentação — Smart Issue & Task Tracker

Este diretório contém a documentação técnica completa do projeto.
O objetivo deste índice é orientar a leitura consoante o perfil e o
objetivo de quem o consulta — humano ou agente de IA.

## Convenções desta Documentação

- Todos os documentos iniciam com um bloco de metadados (`status`,
  `última-atualização`, `responsável`). Nunca considerar como fiável
  um documento com `status: desatualizado` sem confirmação prévia.
- Nomes de ficheiro em inglês; conteúdo em português.
- Diagramas são escritos em Mermaid, embutidos diretamente nos
  ficheiros `.md`, para se manterem versionáveis como texto simples.
- Diagramas mais elaborados (fontes `.puml`, `.drawio`) residem em
  `docs/diagrams/`.
- Decisões arquiteturais relevantes são registadas como ADR
  (Architecture Decision Record) em `docs/adr/`, e não neste índice
  nem nos documentos descritivos.

## Trilhos de Leitura Recomendados

**Se és novo no projeto (visão geral rápida)**
1. Este documento (`README.md`)
2. `01-requirements.md`
3. `02-architecture.md`
4. `STATUS.md`

**Se vais implementar funcionalidades de domínio**
1. `03-domain-model.md`
2. `04-data-model.md`
3. `05-migrations.md`
4. `06-api-contract.md`

**Se vais trabalhar em segurança ou mensageria**
1. `07-security.md`
2. `08-messaging.md`

**Se vais trabalhar na classificação por IA**
1. `09-ai-classification.md`
2. `10-testing-strategy.md` (secção de validação de precisão)

**Se vais avaliar qualidade, operação ou entrega**
1. `10-testing-strategy.md`
2. `11-observability-and-runbook.md`
3. `12-deployment-and-cicd.md`

**Se queres entender o porquê de uma decisão específica**
Consultar diretamente `adr/README.md`, que lista todas as decisões
arquiteturais registadas até ao momento.

## Índice Completo

| # | Documento | Descrição |
|---|-----------|-----------|
| — | `glossary.md` | Termos de domínio e definições técnicas |
| 01 | `01-requirements.md` | Requisitos, escopo do MVP e fases seguintes |
| 02 | `02-architecture.md` | Arquitetura alvo, fluxo end-to-end, diagramas |
| 03 | `03-domain-model.md` | Entidades, value objects, regras de negócio |
| 04 | `04-data-model.md` | Modelo relacional, constraints, índices |
| 05 | `05-migrations.md` | Convenções Flyway e estratégia de evolução do schema |
| 06 | `06-api-contract.md` | Endpoints REST, DTOs, modelo de erro |
| 07 | `07-security.md` | JWT, hashing, autorização, checklist de segurança |
| 08 | `08-messaging.md` | Kafka, RabbitMQ, retry, DLQ, idempotência |
| 09 | `09-ai-classification.md` | Integração Spring AI, prompt, fallback |
| 10 | `10-testing-strategy.md` | Estratégia de testes e validação de métricas |
| 11 | `11-observability-and-runbook.md` | Métricas, dashboards, runbook de incidentes |
| 12 | `12-deployment-and-cicd.md` | Pipeline, branches, ambientes |
| — | `STATUS.md` | Estado atual de implementação (documento volátil) |
| — | `adr/` | Registo de decisões arquiteturais |

## Nota para Agentes de IA

Caso estejas a usar um agente (Cursor ou outro) para auxiliar no
desenvolvimento deste projeto, consulta primeiro o `.cursorrules` na
raiz do repositório. Este ficheiro de documentação complementa aquelas
regras, mas não as substitui — em caso de conflito aparente entre um
documento desatualizado e o `.cursorrules`, prevalece sempre o
`.cursorrules`, por ser o documento de convenções vivas do projeto.
