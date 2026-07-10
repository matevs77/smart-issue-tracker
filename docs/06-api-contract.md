---
status: estável
última-atualização: 2026-07-09
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

## 6. Paginação e Filtros

Todos os endpoints de listagem suportam:

| Parâmetro | Tipo | Default | Descrição |
|-----------|------|---------|-----------|
| page | int | 0 | Número da página (0-based) |
| size | int | 20 | Tamanho da página (max 100) |
| sort | String | createdAt,desc | Ordenação: campo,direção |

Filtros específicos de issues: `status`, `priority`, `reporterId`, `assigneeId`.
