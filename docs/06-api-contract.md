---
status: estável
última-atualização: 2026-07-13
responsável: matevz77
---

# 06 — Contrato da API

## 1. Base URL

Todas as URLs partem de `/api/v1/`.

## 2. Autenticação

```
POST /api/v1/auth/login
```

| Campo | Tipo | Descrição |
|-------|------|-----------|
| username | String | Nome de utilizador |
| password | String | Password |

**Resposta (200):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

## 3. Issues

### 3.1. Criar Issue

```
POST /api/v1/issues
```

**Request:**
```json
{
  "title": "Falha na autenticação 2FA",
  "description": "Utilizadores reportam erro 500 ao validar código 2FA",
  "assigneeId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (201):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "title": "Falha na autenticação 2FA",
  "description": "Utilizadores reportam erro 500 ao validar código 2FA",
  "status": "OPEN",
  "priority": "HIGH",
  "aiConfidenceScore": 0.87,
  "reporter": { "id": "...", "username": "joao" },
  "assignee": { "id": "...", "username": "maria" },
  "comments": [],
  "createdAt": "2025-01-15T10:30:00Z"
}
```

### 3.2. Listar Issues

```
GET /api/v1/issues?page=0&size=20&status=OPEN&priority=HIGH&sort=createdAt,desc
```

**Response (200):**
```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

### 3.3. Obter Issue por ID

```
GET /api/v1/issues/{id}
```

### 3.4. Atualizar Estado

```
PATCH /api/v1/issues/{id}/status
```

**Request:**
```json
{
  "status": "IN_PROGRESS"
}
```

### 3.5. Sobrescrever Prioridade (ADMIN)

```
PATCH /api/v1/issues/{id}/priority
```

**Request:**
```json
{
  "priority": "CRITICAL"
}
```

### 3.6. Reatribuir Responsável (ADMIN / DEVELOPER)

```
PATCH /api/v1/issues/{id}/assignee
```

**Request:**
```json
{
  "assigneeId": "880e8400-e29b-41d4-a716-446655440003"
}
```

**Response (200):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "assignee": { "id": "880e8400-e29b-41d4-a716-446655440003", "username": "joao" },
  "updatedAt": "2025-01-15T12:00:00Z"
}
```

### 3.7. Eliminar Issue (ADMIN)

```
DELETE /api/v1/issues/{id}
```

**Response (204):** Sem conteúdo.

**Segurança:** Apenas utilizadores com Role.ADMIN podem aceder a este endpoint. Tentativas por DEVELOPER ou VIEWER resultam em `403 FORBIDDEN`. A operação é irreversível: remove a issue e todos os seus comentários em cascata. Notificações já emitidas não são afetadas.

### 3.8. Editar Título e Descrição (ADMIN / DEVELOPER)

```
PATCH /api/v1/issues/{id}/details
```

**Request:**
```json
{
  "title": "Falha na autenticação 2FA — corrigida parcialmente",
  "description": "O erro 500 foi identificado no validador de tokens. Aguarda deploy."
}
```

**Response (200):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "title": "Falha na autenticação 2FA — corrigida parcialmente",
  "description": "O erro 500 foi identificado no validador de tokens. Aguarda deploy.",
  "updatedAt": "2025-01-15T14:00:00Z"
}
```

## 4. Comentários

### 4.1. Adicionar Comentário

```
POST /api/v1/issues/{issueId}/comments
```

**Request:**
```json
{
  "content": "Já identifiquei a causa raiz."
}
```

### 4.2. Listar Comentários de uma Issue

```
GET /api/v1/issues/{issueId}/comments
```

## 5. Modelo de Erro (RFC 7807 / Problem Details)

```json
{
  "type": "https://api.issuetracker.dev/errors/issue-not-found",
  "title": "Issue não encontrada",
  "status": 404,
  "detail": "Nenhuma issue encontrada com o ID 660e8400-e29b-41d4-a716-446655440999",
  "instance": "/api/v1/issues/660e8400-e29b-41d4-a716-446655440999"
}
```

| Código | Erro | Descrição |
|--------|------|-----------|
| 400 | INVALID_REQUEST | Erro de validação ( campos obrigatórios, formato inválido) |
| 401 | UNAUTHORIZED | Token JWT ausente, expirado ou inválido |
| 403 | FORBIDDEN | Utilizador autenticado mas sem permissão para a operação |
| 404 | NOT_FOUND | Recurso não encontrado |
| 409 | CONFLICT | Conflito de estado (ex.: tentar fechar issue já closed) |
| 422 | UNPROCESSABLE_ENTITY | Regra de negócio violada (ex.: VIEWER a tentar alterar estado) |
| 500 | INTERNAL_ERROR | Erro interno do servidor |

## 6. Utilizadores

### 6.1. Criar Utilizador (ADMIN)

```
POST /api/v1/users
```

**Request:**
```json
{
  "username": "maria",
  "email": "maria@example.com",
  "password": "s3gur4#2025",
  "role": "DEVELOPER"
}
```

**Response (201):**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "username": "maria",
  "email": "maria@example.com",
  "role": "DEVELOPER",
  "active": true,
  "createdAt": "2025-01-15T11:00:00Z"
}
```

**Segurança:** Apenas utilizadores com Role.ADMIN podem aceder a este endpoint. Tentativas de acesso por DEVELOPER ou VIEWER resultam em `403 FORBIDDEN`.

## 8. Notificações

### 8.1. Listar Notificações do Utilizador Autenticado

```
GET /api/v1/notifications?page=0&size=20
```

**Response (200):**
```json
{
  "content": [
    {
      "id": "990e8400-e29b-41d4-a716-446655440010",
      "type": "ISSUE_ASSIGNED",
      "message": "Foi-lhe atribuída a issue 'Falha na autenticação 2FA'",
      "issueId": "660e8400-e29b-41d4-a716-446655440001",
      "read": false,
      "createdAt": "2025-01-15T10:31:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1
}
```

**Segurança:** O endpoint retorna apenas notificações do utilizador autenticado. O `userId` é extraído do token JWT — não é aceite como parâmetro.

## 7. Paginação e Filtros

Todos os endpoints de listagem suportam:

| Parâmetro | Tipo | Default | Descrição |
|-----------|------|---------|-----------|
| page | int | 0 | Número da página (0-based) |
| size | int | 20 | Tamanho da página (max 100) |
| sort | String | createdAt,desc | Ordenação: campo,direção |

Filtros específicos de issues: `status`, `priority`, `reporterId`, `assigneeId`.
