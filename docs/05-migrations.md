---
status: estável
última-atualização: 2026-07-19
responsável: matevz77
---

# 05 — Migrações (Flyway)

## 1. Convenções de Nomenclatura

```
src/main/resources/db/migration/
  ├── V1__create_user_table.sql
  ├── V2__create_issue_table.sql
  ├── V3__create_comment_table.sql
  ├── V4__create_notification_table.sql
  └── ...
```

Formato: `V{número}__{descrição_sem_acentos}.sql`

- Números sequenciais, sem saltos.
- Descrição em snake_case, em português ou inglês (consistente).
- Prefixo `V` maiúsculo, seguido de dois underscores.

## 2. Baseline

O projeto usa **baseline-on-migrate=true** para permitir que o Flyway seja introduzido numa base já existente sem falhar. A baseline version é `0`.

Configuração no `application.yml`:

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
```

## 3. Estratégia de Migração

| Princípio | Prática |
|-----------|---------|
| Imutabilidade | Migrações aplicadas **nunca** são alteradas. Correções são feitas em novas migrações. |
| Uma migração por alteração lógica | Cada migration ficheiro corresponde a uma alteração coesa (ex.: criar tabela, adicionar coluna, criar índice). |
| Rollback | Não há suporte a `undo` no Flyway Community. Rollbacks são feitos via nova migração ou restore de backup. |
| Testes | Migrações são validadas em testes de integração com Testcontainers antes de aplicar em produção. |

## 4. Migrações Planeadas

| Versão | Descrição | Dependências |
|--------|-----------|-------------|
| V1 | Criar tabela tb_users | Nenhuma |
| V2 | Criar tabela tb_issues | V1 (FK para tb_users) |
| V3 | Criar tabela tb_comments | V2 (FK para tb_issues, ON DELETE CASCADE), V1 (FK para tb_users) |
| V4 | Criar tabela tb_notifications | V1 (FK para tb_users) |
| V6 | (Futuro) Criar tabela de auditoria de prioridade | V2 |
| V7 | (Futuro) Adicionar coluna de refresh_token | V1 |

## 5. Exemplo: V2__create_issue_table.sql

```sql
CREATE TABLE tb_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    priority VARCHAR(20) CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    ai_confidence_score DOUBLE PRECISION CHECK (ai_confidence_score >= 0.0 AND ai_confidence_score <= 1.0),
    reporter_id UUID NOT NULL REFERENCES tb_users(id),
    assignee_id UUID REFERENCES tb_users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_issues_status ON tb_issues(status);
CREATE INDEX idx_issues_reporter ON tb_issues(reporter_id);
CREATE INDEX idx_issues_assignee ON tb_issues(assignee_id);
CREATE INDEX idx_issues_created_at ON tb_issues(created_at DESC);
```