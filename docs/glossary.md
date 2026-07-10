---
status: estável
última-atualização: 2026-07-09
responsável: matevz77
---

# Glossário

Termos de domínio e definições técnicas utilizadas ao longo do projeto.

| Termo | Definição |
|-------|-----------|
| **Issue** | Ticket ou tarefa a ser acompanhada. Entidade central do sistema, composta por título, descrição, estado, prioridade e responsáveis. |
| **Comment** | Anotação textual associada a uma Issue. Pertence a um autor e a uma issue específica. |
| **Notification** | Mensagem gerada assincronamente para informar um utilizador sobre um evento do sistema (ex.: issue atribuída, novo comentário). |
| **User** | Utilizador registado no sistema. Possui credenciais, um role e pode ser reporter ou assignee de issues. |
| **Priority** | Classificação de importância de uma issue: LOW, MEDIUM, HIGH, CRITICAL. Pode ser definida manualmente ou por IA. |
| **IssueStatus** | Ciclo de vida de uma issue: OPEN, IN_PROGRESS, RESOLVED, CLOSED. |
| **Role** | Nível de permissão de um utilizador: ADMIN, DEVELOPER, VIEWER. |
| **Virtual Threads** | Mecanismo de concorrência leve do Java (Project Loom), gerido pela JVM, que permite escalar tarefas I/O-bound sem bloqueio de threads do SO. |
| **Spring AI** | Abstraction layer do ecossistema Spring para integração com modelos de linguagem (LLMs) como OpenAI e Anthropic. |
| **Kafka** | Plataforma de streaming de eventos distribuída. Usada no projeto como event log imutável para eventos de domínio. |
| **RabbitMQ** | Message broker baseado no protocolo AMQP. Usado no projeto para notificações assíncronas de baixa latência. |
| **DLQ** | Dead Letter Queue — fila onde mensagens não processáveis são redirecionadas após excederem tentativas de retry. |
| **ADR** | Architecture Decision Record — documento conciso que regista uma decisão arquitetural, seu contexto, alternativa(s) considerada(s) e justificação. |
| **Flyway** | Ferramenta de versionamento e migração de schema de base de dados. |
| **Actuator** | Módulo do Spring Boot que expõe endpoints de monitorização e gestão (health, metrics, info, etc.). |
| **Micrometer** | Facade de métricas usada pelo Spring Boot para expor dados a sistemas como Prometheus. |
| **Ports & Adapters** | Padrão arquitetural (também conhecido como Arquitetura Hexagonal) que isola o domínio de detalhes de infraestrutura através de interfaces (ports) e implementações (adapters). |
