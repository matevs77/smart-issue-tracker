---
status: aceite
última-atualização: 2026-07-22
responsável: matevz77
---

# ADR-07 — Bootstrap de acesso e claims JWT

## Contexto

A Fase 2 (Segurança) introduz autenticação via JWT no sistema. Duas decisões
arquiteturais com alternativas rejeitadas e consequências identificadas exigem
registo formal:

1. **Conteúdo dos *claims* do JWT**: o *payload* do token deve incluir apenas
   `sub` (username) e `roles`, ou deve também incluir o `uid` (UUID do
   utilizador)?
2. **Bootstrap do primeiro `ADMIN`**: como criar o primeiro utilizador com
   `Role.ADMIN` num sistema onde a criação de utilizadores é restrita a
   administradores (RN-08), sem que exista ainda qualquer administrador?

## Decisão 1 — *Claim* `uid` no JWT

O token JWT inclui um *claim* `uid` com o UUID do utilizador, além dos *claims*
base (`sub`, `roles`, `iat`, `exp`). Esta decisão segue o *payload* base
definido em `docs/07-security.md`, secção 3, com o acréscimo do `uid`.

**Razão:** sem este *claim*, o `JwtAuthFilter` teria de consultar a base de
dados em cada pedido autenticado apenas para resolver `username → id` —
necessário em várias operações (RF-08, RF-20, RN-03) que precisam do `id`, não
apenas do `username`. Incluir o `uid` no token elimina essa consulta redundante,
mantendo o filtro `stateless` e sem acesso a repositórios.

## Decisão 2 — Bootstrap do primeiro `ADMIN` via Flyway com *placeholder*

Adota-se uma migração Flyway de *seed* (`V5__seed_default_admin_user.sql`) que
insere um único utilizador `ADMIN`. O hash BCrypt da *password* é referenciado
através de um *placeholder* Flyway `${admin_password_hash}`, resolvido em tempo
de execução pela propriedade `spring.flyway.placeholders.admin-password-hash`,
com valor por omissão de desenvolvimento definido em `application.yml` — o mesmo
padrão `${ENV_VAR:valor-por-omissão}` já usado para `DB_PASSWORD` e `JWT_SECRET`.

## Alternativas Consideradas (Bootstrap)

### Alternativa A — Migração Flyway com *placeholder* (escolhida)

- **Prós:** reproduzível, versionado, consistente com a estratégia de migrações
  imutáveis já em uso (`05-migrations.md`); o hash não fica irrevogavelmente
  gravado no histórico do SQL — pode ser rodado em qualquer ambiente apenas por
  alteração da variável de ambiente, sem nova migração.
- **Contras:** o valor por omissão de desenvolvimento continua documentado e
  publicamente visível — mas este é o mesmo nível de risco já aceite para
  `DB_PASSWORD`/`JWT_SECRET` no projeto.
- **Razão para escolher:** resolve o problema sem código adicional, segue a
  convenção Flyway já estabelecida, e alinha o tratamento da *password* de
  arranque com o mesmo grau de exceção já aceite para outros segredos de
  desenvolvimento.

### Alternativa B — Propriedade de arranque `security.bootstrap.enabled`

- **Prós:** evita *password* fixa no repositório (pode gerar uma aleatória e
  imprimi-la no log de arranque).
- **Contras:** introduz lógica de arranque condicional, mais difícil de testar
  e de auditar do que uma migração declarativa; foge à convenção já estabelecida
  do projeto de tratar alterações de *schema*/dados exclusivamente via Flyway.
- **Razão para rejeitar:** maior complexidade sem benefício proporcional para
  um projeto pessoal/portefólio nesta fase.

### Alternativa C — Criação manual fora da aplicação (script SQL avulso)

- **Prós:** nenhuma alteração de código.
- **Contras:** não reprodutível, não versionado.
- **Razão para rejeitar:** incompatível com a exigência de reprodutibilidade já
  aplicada às migrações e aos testes.

## Consequências

### Positivas

- O `JwtAuthFilter` é completamente *stateless*: não precisa de aceder à base de
  dados em nenhum momento da validação do token.
- O bootstrap do `ADMIN` é transparente para o operador: ao arrancar com
  `docker compose up`, o utilizador `admin` está imediatamente disponível.
- A *password* do `admin` pode ser alterada em qualquer ambiente apenas
  definindo a variável `ADMIN_BOOTSTRAP_PASSWORD_HASH`, sem necessidade de nova
  migração ou alteração de código.

### Negativas / Trade-offs

- O valor por omissão do hash em `application.yml` é um segredo de
  desenvolvimento publicamente conhecido — mitigado pela documentação explícita
  da obrigação de definir `ADMIN_BOOTSTRAP_PASSWORD_HASH` em qualquer ambiente
  real (`11-observability-and-runbook.md`, secção 3).
- Não existe atualmente um endpoint `PATCH` para alterar a *password* do
  utilizador `admin` após o arranque — a rotação pós-bootstrap está
  identificada como lacuna para uma fase futura.

## Referências

- `docs/07-security.md` — secção 3 (estrutura do token JWT) e secção 9 (checklist OWASP)
- `docs/05-migrations.md` — secção 4 (migrações planeadas, V5)
- `docs/11-observability-and-runbook.md` — secção 3 (Bootstrap de Acesso)
- `docs/Prompts/prompts-fase2-seguranca.md` — Decisões Arquiteturais Desta Fase
- Código: `JwtService.java`, `JwtAuthFilter.java`, `V5__seed_default_admin_user.sql`
