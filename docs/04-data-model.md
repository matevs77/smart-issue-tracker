---
status: estável
última-atualização: 2026-07-09
responsável: matevz77
---

# 04 — Modelo de Dados

## 1. Modelo Relacional

```mermaid
erDiagram
    tb_users {
        uuid id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        varchar role
        boolean active
        timestamp created_at
    }

    tb_issues {
        uuid id PK
        varchar title
        text description
        varchar status
        varchar priority
        double precision ai_confidence_score
        uuid reporter_id FK
        uuid assignee_id FK
        timestamp created_at
        timestamp updated_at
    }

    tb_comments {
        uuid id PK
        uuid issue_id FK
        uuid author_id FK
        text content
        timestamp created_at
    }

    tb_notifications {
        uuid id PK
        uuid recipient_id FK
        varchar message
        varchar type
        varchar status
        timestamp created_at
    }

    tb_issues ||--o{ tb_comments : "has"
    tb_users ||--o{ tb_issues : "reporter"
    tb_users ||--o{ tb_issues : "assignee"
    tb_users ||--o{ tb_comments : "author"
    tb_users ||--o{ tb_notifications : "recipient"
```

## 2. Definição das Tabelas

### tb_users

| Coluna | Tipo | Constraints |
|--------|------|------------|
| id | UUID | PK, DEFAULT gen_random_uuid() |
| username | VARCHAR(50) | NOT NULL, UNIQUE |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| password_hash | VARCHAR(255) | NOT NULL |
| role | VARCHAR(20) | NOT NULL, CHECK IN ('ADMIN','DEVELOPER','VIEWER') |
| active | BOOLEAN | NOT NULL, DEFAULT true |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() |

### tb_issues

| Coluna | Tipo | Constraints |
|--------|------|------------|
| id | UUID | PK, DEFAULT gen_random_uuid() |
| title | VARCHAR(200) | NOT NULL |
| description | TEXT | NOT NULL |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED') |
| priority | VARCHAR(20) | CHECK IN ('LOW','MEDIUM','HIGH','CRITICAL') |
| ai_confidence_score | DOUBLE PRECISION | CHECK (0.0 <= score <= 1.0) |
| reporter_id | UUID | NOT NULL, FK → tb_users(id) |
| assignee_id | UUID | FK → tb_users(id) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT now() |

### tb_comments

| Coluna | Tipo | Constraints |
|--------|------|------------|
| id | UUID | PK, DEFAULT gen_random_uuid() |
| issue_id | UUID | NOT NULL, FK → tb_issues(id) ON DELETE CASCADE |
| author_id | UUID | NOT NULL, FK → tb_users(id) |
| content | TEXT | NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() |

### tb_notifications

| Coluna | Tipo | Constraints |
|--------|------|------------|
| id | UUID | PK, DEFAULT gen_random_uuid() |
| recipient_id | UUID | NOT NULL, FK → tb_users(id) |
| message | TEXT | NOT NULL |
| type | VARCHAR(30) | NOT NULL, CHECK IN ('ISSUE_ASSIGNED','COMMENT_ADDED','PRIORITY_SET') |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING', CHECK IN ('PENDING','SENT','FAILED') |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() |

## 3. Índices

```sql
CREATE INDEX idx_issues_status ON tb_issues(status);
CREATE INDEX idx_issues_reporter ON tb_issues(reporter_id);
CREATE INDEX idx_issues_assignee ON tb_issues(assignee_id);
CREATE INDEX idx_issues_created_at ON tb_issues(created_at DESC);

CREATE INDEX idx_comments_issue ON tb_comments(issue_id);
CREATE INDEX idx_comments_author ON tb_comments(author_id);

CREATE INDEX idx_notifications_recipient ON tb_notifications(recipient_id);
CREATE INDEX idx_notifications_status ON tb_notifications(status);
```

## 4. Regras de Consistência

- A FK `assignee_id` em `tb_issues` é opcional (pode ser NULL).
- O `ai_confidence_score` só é preenchido após classificação pela IA; pode ser NULL.
- A constraint CHECK de `priority` permite NULL (issue aguardando classificação).
- A constraint CHECK de `status` não permite NULL (toda issue nasce com status definido).
- `ON DELETE CASCADE` em `tb_comments.issue_id`: remover uma issue remove todos os comentários associados.
- `ON DELETE RESTRICT` em FKs de `tb_users`: não é possível remover um utilizador com issues ou comentários associados.
- As notificações (`tb_notifications`) não têm FK direta para `tb_issues`; a eliminação de uma issue não afeta notificações já emitidas.

## 5. Estratégia de Eliminação

Adota-se **hard delete** como estratégia única para todas as entidades:

| Entidade | Estratégia | Comportamento |
|----------|-----------|---------------|
| Issue | Hard delete (`DELETE FROM tb_issues`) | Remove a issue e cascateia para `tb_comments` via `ON DELETE CASCADE`; notificações existentes são preservadas |
| Comment | Hard delete (via cascade da issue ou `DELETE FROM tb_comments`) | Não há eliminação isolada de comentários — seguem o ciclo de vida da issue |
| Notification | Hard delete (apenas administrativo, sem endpoint público) | Sem FK para issue, não sofre cascata; pode ser limpa periodicamente por script de retenção (fase 2) |

**Nota:** Não se usa `deleted_at` ou soft delete em nenhuma tabela. A decisão baseia-se na simplicidade operacional e na ausência de requisitos de recuperação de issues eliminadas no MVP. Se a auditoria completa (histórico de alterações) vier a ser necessária na fase 2, deve ser implementada com uma tabela de eventos separada, não com soft delete nas entidades de domínio.
