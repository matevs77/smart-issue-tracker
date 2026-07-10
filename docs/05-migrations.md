---
status: estável
última-atualização: 2026-07-09
responsável: matevz77
---

# 05 — Migrações (Flyway)

## 1. Convenções de Nomenclatura

```
src/main/resources/db/migration/
  ├── V1__create_issue_table.sql
  ├── V2__create_comment_table.sql
  ├── V3__create_notification_table.sql
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
| V1 | Criar tabela tb_issues | Nenhuma |
| V2 | Criar tabela tb_comments | V1 (FK para tb_issues) |
| V3 | Criar tabela tb_users | Nenhuma |
| V4 | Criar tabela tb_notifications | V3 (FK para tb_users) |
| V5 | Adicionar índices | V1-V4 |
| V6 | (Futuro) Criar tabela de auditoria de prioridade | V1 |
| V7 | (Futuro) Adicionar coluna de refresh_token | V3 |

## 5. Exemplo: V1__create_issue_table.sql

```sql
CREATE TABLE tb_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    assigned_to UUID REFERENCES tb_users(id),
    created_by UUID NOT NULL REFERENCES tb_users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
```