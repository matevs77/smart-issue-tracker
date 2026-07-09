---
status: estável
última-atualização: 2025-01-XX
responsável: teu nome
---

# 03 — Modelo de Domínio

## 1. Entidades

### 1.1. Issue

Entidade central do sistema.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID | Identificador único |
| title | String | Título da issue |
| description | String | Descrição detalhada |
| status | IssueStatus | Estado no ciclo de vida (OPEN, IN_PROGRESS, RESOLVED, CLOSED) |
| priority | IssuePriority | Prioridade (LOW, MEDIUM, HIGH, CRITICAL) |
| aiConfidenceScore | Double | Score de confiança devolvido pelo modelo de IA (0.0 a 1.0) |
| reporter | User | Utilizador que criou a issue |
| assignee | User | Utilizador responsável (opcional) |
| comments | List\<Comment\> | Comentários associados |
| createdAt | Instant | Data/hora de criação |
| updatedAt | Instant | Data/hora da última alteração |

### 1.2. Comment

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID | Identificador único |
| issue | Issue | Issue a que pertence |
| author | User | Autor do comentário |
| content | String | Conteúdo textual |
| createdAt | Instant | Data/hora de criação |

### 1.3. User

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID | Identificador único |
| username | String | Nome de utilizador (único) |
| email | String | Email (único) |
| passwordHash | String | Hash BCrypt da password |
| role | Role | ADMIN, DEVELOPER ou VIEWER |
| active | Boolean | Se a conta está ativa |
| createdAt | Instant | Data/hora de criação |

### 1.4. Notification

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID | Identificador único |
| recipient | User | Utilizador destinatário |
| message | String | Conteúdo da notificação |
| type | NotificationType | Tipo (ISSUE_ASSIGNED, COMMENT_ADDED, PRIORITY_SET) |
| status | NotificationStatus | PENDING, SENT, FAILED |
| createdAt | Instant | Data/hora de criação |

## 2. Value Objects / Enums

### IssueStatus

```
OPEN → IN_PROGRESS → RESOLVED → CLOSED
  ↑         ↑            ↑
  └─────────┴────────────┘ (pode saltar estados)
```

### IssuePriority

```
LOW → MEDIUM → HIGH → CRITICAL
```

### Role

```
ADMIN > DEVELOPER > VIEWER (hierarquia de permissões)
```

### NotificationStatus

```
PENDING → SENT
    ↓
  FAILED
```

## 3. Regras de Negócio

| ID | Regra | Onde se aplica |
|----|-------|---------------|
| RN-01 | Apenas ADMIN ou DEVELOPER podem alterar o status de uma issue | IssueService |
| RN-02 | A prioridade calculada pela IA pode ser sobreposta manualmente por ADMIN | UpdateIssueUseCase |
| RN-03 | Toda sobreposição manual de prioridade deve ser registada em log com timestamp e responsável | UpdateIssueUseCase |
| RN-04 | Falha na chamada ao modelo de IA não bloqueia a criação — usar fallback para MEDIUM | IssueEventConsumer |
| RN-05 | Uma issue só pode ser fechada (CLOSED) se tiver resolução associada (descrição obrigatória) | UpdateIssueUseCase |
| RN-06 | O autor de um comentário não pode ser notificado do seu próprio comentário | CommentService |
| RN-07 | Utilizadores com ROLE_VIEWER têm acesso apenas de leitura a issues | SecurityConfig / JwtAuthFilter |

## 4. Separação Domínio vs. JPA

O projeto adota uma separação explícita entre entidades de domínio e entidades JPA:

```
┌─────────────────────┐      ┌──────────────────────┐
│   Domain Entity     │      │    JPA Entity         │
│   (Issue.java)      │      │   (IssueJpaEntity)    │
├─────────────────────┤      ├──────────────────────┤
│ - id: UUID          │      │ - id: UUID           │
│ - title: String     │      │ - title: String      │
│ - status: Status    │      │ - status: String     │
│ - comments: List<>  │      │ - comments: List<>   │
│ + changeStatus()    │      │ + getters/setters    │
│ + addComment()      │      └──────────────────────┘
└─────────────────────┘
         │                       │
         └─────── IssueMapper (MapStruct) ───────┘
                     domain ↔ entity
```

**Princípios:**
- Entidades de domínio não têm anotações JPA.
- Repositórios (interfaces) pertencem ao domínio.
- Implementações JPA residem na infraestrutura.
- A conversão é feita por mapeadores (MapStruct), nunca por exposição direta.
