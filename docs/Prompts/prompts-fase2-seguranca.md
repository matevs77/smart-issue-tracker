---
status: proposto
última-atualização: 2026-07-22
responsável: matevz77
---

# Prompts de Implementação — Fase 2 (Segurança / JWT)

Este ficheiro segue a convenção já validada em `prompts-correcao-pre-fase2.md`:
prompts sequenciais, cada um independentemente testável, com o raciocínio
arquitetural explicado antes da instrução de implementação, conforme
`.cursorrules`, secção 7. O ficheiro `prompts-fase1-fundacao.md`, referido
no roteiro original, não foi encontrado no repositório; este documento
adopta, portanto, a mesma estrutura (Contexto / Instrução / Critério de
teste) já comprovada, estendendo a numeração decimal citada em
`STATUS.md` ("Prompt 1.4", etc.) para o intervalo `2.x`, correspondente a
esta fase.

**Âmbito da Fase 2** (conforme `README.md` e `docs/01-requirements.md`,
secção 3.4): autenticação via credenciais com emissão de token JWT (RF-11),
restrição de acesso a *endpoints* por `Role` (RF-12), e resolução de toda a
autorização condicional (RN-01, RN-02, RN-07, RN-08) atualmente marcada
como `// TODO(Fase 2)` no código. Fora de âmbito: *refresh tokens* com
rotação, *blacklist* de tokens revogados e *rate limiting* — estes
permanecem corretamente diferidos, conforme `docs/01-requirements.md`,
secção 6.

---

## Estado Atual (Auditado)

Antes de planear a implementação, importa registar o que já existe no
repositório e que a Fase 2 deve reaproveitar, em vez de recriar:

| Componente | Estado | Nota |
|---|---|---|
| `pom.xml` | ✅ Pronto | `spring-boot-starter-security` e `jjwt-api`/`jjwt-impl`/`jjwt-jackson` (0.12.6) já são dependências declaradas — nenhuma alteração de `pom.xml` é necessária nesta fase |
| `PasswordEncoderConfig.java` | ✅ Pronto | Bean `BCryptPasswordEncoder(10)` já implementado e já usado por `CreateUserUseCase` |
| `SecurityConfig.java` | 🔧 Placeholder | `permitAll()` em todos os pedidos, com `// TODO(Fase 2)` explícito — é o principal alvo desta fase |
| `JwtAuthFilter.java` | 📋 Vazio | A implementar (Prompt 2.5) |
| `JwtService.java` | 📋 Vazio | A implementar (Prompt 2.3) |
| `Role` enum, `User` domain | ✅ Prontos | `ADMIN`, `DEVELOPER`, `VIEWER`; `User` já expõe `getPasswordHash()` e `getRole()` |
| `UserRepository` (domínio) | 🔧 Incompleto | Tem `save`, `findById`, `existsByUsername`, `existsByEmail` — falta `findByUsername`, necessário para o *login* (Prompt 2.2) |
| `reporterId` em `CreateIssueRequest` | 🔧 Scaffolding | `// TODO(Fase 2): remover e extrair do SecurityContext após JWT` |
| `authorId` em `CreateCommentRequest` | 🔧 Scaffolding | Mesmo padrão |
| `recipientId` (query param) em `NotificationController` | 🔧 Scaffolding | Mesmo padrão |
| Todos os `PATCH`/`DELETE` de `IssueController` | 🔧 Sem autorização | Cada um com `// TODO(Fase 2): restringir por Role conforme RN-01/RN-02` |
| `POST /api/v1/users` | 🔧 Sem autorização | RF-13/RN-08 exigem `Role.ADMIN`, ainda não aplicado |
| `UpdateIssueUseCase.overridePriority` | 🔧 Auditoria incompleta | `responsible` está fixo como `"UNKNOWN"` — RN-03 exige o utilizador real |
| `docs/07-security.md` | ⚠️ Desenho-alvo, parcialmente desatualizado | Descreve o fluxo e os componentes corretamente, mas a tabela RBAC da secção 5 não cobre `/assignee`, `/details`, `DELETE`, nem `POST /users` — a corrigir no Prompt 2.8 |

---

## Decisões Arquiteturais Desta Fase

Seguindo `.cursorrules`, secção 7 ("não inventar âmbito autonomamente"),
regista-se aqui, de forma explícita, cada decisão estrutural tomada para
esta fase — para que possas confirmar ou corrigir antes de a executares
com o Cursor.

### Decisão 1 — Conteúdo dos *claims* do JWT

`docs/07-security.md` já define o *payload* base (`sub`, `roles`, `iat`,
`exp`). Propõe-se acrescentar um *claim* `uid` (UUID do utilizador).
**Razão:** sem este *claim*, o `JwtAuthFilter` teria de consultar a base
de dados em cada pedido autenticado apenas para resolver `username → id`
— necessário em várias operações (RF-08, RF-20, RN-03) que precisam do
`id`, não apenas do `username`. Incluir o `uid` no token elimina essa
consulta redundante, mantendo o filtro `stateless` e sem acesso a
repositórios, tal como já sugerido no diagrama de sequência de
`07-security.md` (o filtro só define o `SecurityContext`, sem passos
adicionais de base de dados).

### Decisão 2 — Mecanismo de autorização: `@PreAuthorize` (segurança ao nível do método)

Existem duas abordagens válidas em Spring Security: regras centralizadas
em `SecurityConfig` (`authorizeHttpRequests` com `requestMatchers` por
verbo HTTP) ou anotações `@PreAuthorize` junto de cada método de
controlador. Adota-se a segunda, porque a matriz de permissões deste
projeto tem granularidade por verbo HTTP *e* por caminho (ex.:
`PATCH /status` permite `ADMIN`+`DEVELOPER`, mas `PATCH /priority` no
mesmo recurso permite apenas `ADMIN`) — expressar isto em
`requestMatchers` obrigaria a duplicar o caminho para cada verbo, tornando
`SecurityConfig` difícil de auditar. Colocar `@PreAuthorize` junto de cada
`@PatchMapping`/`@DeleteMapping` mantém a regra visível exatamente onde a
operação é definida, o que se alinha com o princípio de "raciocínio
arquitetural explicado antes do código" já seguido neste projeto.

### Decisão 3 — Bootstrap do primeiro utilizador `ADMIN`

RN-08 exige que a criação de utilizadores seja restrita a `ADMIN`
("não existe registo público"). Isto cria uma dependência circular: sem
um `ADMIN` já existente, ninguém consegue autenticar-se para criar o
primeiro `ADMIN`. Três alternativas foram avaliadas:

**Alternativa A — Migração Flyway de *seed* (escolhida, com refinamento por *placeholder*).**
Uma nova migração `V5__seed_default_admin_user.sql` insere um único
utilizador `ADMIN`. Numa primeira formulação, o hash BCrypt seria gravado
como valor literal na própria migração. **Refinamento adotado**: em vez
disso, o hash é referenciado na migração através de um *placeholder*
Flyway `${admin_password_hash}`, resolvido em tempo de execução pela
propriedade `spring.flyway.placeholders.admin-password-hash`, com valor
por omissão de desenvolvimento definido em `application.yml` — seguindo
exatamente o mesmo padrão `${ENV_VAR:valor-por-omissão}` já usado para
`DB_PASSWORD` e `JWT_SECRET`. *Prós:* reproduzível, versionado,
consistente com a estratégia de migrações imutáveis já em uso
(`05-migrations.md`); não exige código adicional nem *flags* de arranque;
com o refinamento, o hash deixa de estar irrevogavelmente gravado no
histórico do SQL — pode ser rodado em qualquer ambiente apenas por
alteração da variável de ambiente, sem nova migração; alinha o
tratamento da *password* de arranque com o mesmo grau de exceção já
aceite no projeto para `DB_PASSWORD`/`JWT_SECRET` (um valor de
desenvolvimento conhecido, documentado como tal, nunca usado em
produção). *Contras remanescentes:* o valor por omissão de
desenvolvimento continua, ainda assim, documentado e publicamente
visível — mas este é precisamente o mesmo nível de risco já aceite para
os outros segredos de desenvolvimento do projeto, não um risco adicional
introduzido por esta decisão. *Mitigação:* documentar explicitamente a
obrigação de definir `ADMIN_BOOTSTRAP_PASSWORD_HASH` em qualquer ambiente
real, e assinalar esta decisão no ADR-07 (Prompt 2.11) e no *runbook*
(`11-observability-and-runbook.md`).

**Alternativa B — Propriedade de arranque `security.bootstrap.enabled`.**
Um `CommandLineRunner` que cria o primeiro `ADMIN` apenas se a tabela
`tb_users` estiver vazia. *Prós:* evita *password* fixa no repositório
(pode gerar uma aleatória e imprimi-la no log de arranque). *Contras:*
introduz lógica de arranque condicional, mais difícil de testar e de
auditar do que uma migração declarativa; foge à convenção já estabelecida
do projeto de tratar alterações de schema/dados exclusivamente via
Flyway.
*Razão para rejeitar agora:* maior complexidade sem benefício
proporcional para um projeto pessoal/portefólio nesta fase.

**Alternativa C — Criação manual fora da aplicação (script SQL avulso).**
*Prós:* nenhuma alteração de código. *Contras:* não reprodutível, não
versionado, contraria a filosofia de "empirically defensible" e de
metodologia reproduzível que rege todo o projeto.
*Razão para rejeitar:* incompatível com a exigência de reprodutibilidade
já aplicada às migrações e aos testes.

**Decisão final (confirmada):** Alternativa A com o refinamento por
*placeholder* Flyway. O Prompt 2.7, abaixo, já reflete esta escolha.

---

## Prompt 2.1 — Configuração de propriedades JWT

### Contexto para o agente

`docs/07-security.md` exige um segredo assinante HMAC-SHA256 e um tempo
de expiração configurável, mas `application.yml` e `application-dev.yml`
não têm ainda nenhuma propriedade relacionada com JWT.
`docs/12-deployment-and-cicd.md` já estabelece a convenção: em
desenvolvimento, um valor por omissão via variável de ambiente; em
produção, um segredo gerido externamente — o mesmo padrão já usado para
`DB_USERNAME`/`DB_PASSWORD` em `application-dev.yml`.

### Instrução

1. Em `src/main/resources/application.yml`, adiciona uma secção `security.jwt`
   com duas propriedades: `secret` (via `${JWT_SECRET:...}`, com um valor
   por omissão de desenvolvimento de pelo menos 32 caracteres — o
   algoritmo HMAC-SHA256 exige uma chave de, no mínimo, 256 bits, ou o
   `jjwt` lança `WeakKeyException` em tempo de arranque) e
   `expiration-minutes` (via `${JWT_EXPIRATION_MINUTES:60}`, refletindo o
   valor de 1 hora já documentado em `07-security.md`, secção 3).
2. Confirma que `.gitignore` e `.cursorignore` já protegem
   `application-secrets.yml` e `.env` (já protegem, não é necessário
   alterar) — não colocar o valor de produção em nenhum ficheiro
   versionado.
3. Não é necessário criar `application-secrets.yml` nesta fase; o valor
   por omissão em `application.yml` é suficiente para desenvolvimento e
   testes locais.

### Critério de teste

- A aplicação arranca sem lançar `WeakKeyException` (`mvn spring-boot:run`
  com o perfil `dev`).
- `application.yml` não contém nenhum segredo em texto plano fora do
  valor por omissão de desenvolvimento, claramente identificável como tal.

---

## Prompt 2.2 — Estender `UserRepository` com `findByUsername`

### Contexto para o agente

O fluxo de autenticação (Prompt 2.4) precisa de carregar um `User` pelo
`username` submetido no `LoginRequest`. A interface de domínio
`UserRepository` não tem este método; `UserJpaRepository` também não.
Segue-se exatamente o padrão já usado para `existsByUsername`.

### Instrução

1. Em `user/domain/UserRepository.java`, adiciona
   `Optional<User> findByUsername(String username);`.
2. Em `user/infrastructure/persistence/UserJpaRepository.java`, adiciona
   `Optional<UserJpaEntity> findByUsername(String username);` (*query
   method* derivado, sem necessidade de `@Query`).
3. Em `user/infrastructure/persistence/UserRepositoryAdapter.java`,
   implementa `findByUsername`, delegando em `jpaRepository.findByUsername`
   e mapeando com `userMapper::toDomain`, por analogia direta com
   `findById`.

### Critério de teste

- O projeto compila (`mvn compile`).
- Teste unitário simples (pode ser incluído já aqui ou adiado para o
  Prompt 2.10): `UserRepositoryAdapter` devolve `Optional.empty()` para
  um `username` inexistente e o `User` correto para um existente.

---

## Prompt 2.3 — Implementar `JwtService`

### Contexto para o agente

Este é o componente central da Fase 2: gera e valida os tokens JWT.
Segundo a Decisão 1, o *payload* inclui `sub` (username), `uid` (UUID),
`roles` (lista de nomes de `Role`), `iat` e `exp`. Usa a API `jjwt`
0.12.6, já declarada em `pom.xml`.

### Instrução

1. Implementa `security/JwtService.java` com, no mínimo:
   - `String generateToken(UUID userId, String username, Role role)` —
     assina com HMAC-SHA256, usando o segredo de
     `security.jwt.secret` e expiração de `security.jwt.expiration-minutes`
     minutos a partir do momento da chamada.
   - `Claims parseAndValidate(String token)` (ou equivalente) — valida a
     assinatura e a expiração; lança uma exceção específica (não a
     exceção genérica de `jjwt`) em caso de token inválido ou expirado,
     para que `JwtAuthFilter` (Prompt 2.5) a capture de forma previsível
     sem depender de tipos da biblioteca externa em código de
     infraestrutura própria.
   - Métodos de extração dos *claims* (`extractUsername`, `extractUserId`,
     `extractRoles`) a partir de um `Claims` já validado.
2. Injeta o segredo e a expiração via `@Value` ou via uma classe de
   propriedades dedicada (`@ConfigurationProperties(prefix = "security.jwt")`)
   — preferir a segunda opção, por ser mais testável e por seguir a
   convenção de injeção por construtor obrigatória em `.cursorrules`,
   secção 4.
3. Não uses `Thread`/`ExecutorService` manuais — esta é uma operação
   síncrona e leve, não se aplica a regra de Virtual Threads aqui.

### Critério de teste

- Teste unitário (pode ser escrito já aqui, adiantando parte do
  Prompt 2.10): gerar um token e validá-lo de imediato devolve os
  *claims* corretos; um token adulterado (assinatura inválida) e um
  token expirado (usar uma expiração de milissegundos negativa no teste)
  lançam a exceção esperada.

---

## Prompt 2.4 — `UserDetailsServiceImpl` e princípio de utilizador autenticado

### Contexto para o agente

Conforme o fluxo documentado em `07-security.md`, secção 1, o *login*
passa por um `UserDetailsServiceImpl` que carrega o `User` da base de
dados e o converte para `UserDetails` do Spring Security. Este componente
só é usado no momento do *login* (via `AuthenticationManager`); os
pedidos subsequentes não voltam a consultá-lo — o `JwtAuthFilter`
(Prompt 2.5) reconstrói a autenticação diretamente a partir dos *claims*
do token (Decisão 1), sem nova consulta à base de dados.

### Instrução

1. Cria `security/AuthenticatedUserDetails.java`, uma classe que
   implementa `org.springframework.security.core.userdetails.UserDetails`,
   envolvendo o `User` de domínio e expondo adicionalmente `UUID getId()`
   (o contrato `UserDetails` não tem `getId()`; este projeto precisa
   dele para gerar o *claim* `uid` no momento do *login*).
2. Implementa `security/UserDetailsServiceImpl.java`, que implementa
   `UserDetailsService` e usa `UserRepository.findByUsername` (Prompt 2.2),
   lançando `UsernameNotFoundException` se não encontrado, e devolvendo
   um `AuthenticatedUserDetails` com as autoridades mapeadas a partir de
   `Role` (por convenção Spring Security, prefixadas com `ROLE_`, ex.:
   `ROLE_ADMIN`).
3. Cria `security/AuthenticatedPrincipal.java` como um `record` simples
   (`UUID id, String username`) — este é o objeto que **efetivamente**
   fica no `SecurityContext` após a validação do token em cada pedido
   (distinto de `AuthenticatedUserDetails`, que só existe durante o
   *login*). Esta distinção evita confundir "o que valida a password"
   com "o que identifica o pedido autenticado".

### Critério de teste

- O projeto compila.
- Teste unitário de `UserDetailsServiceImpl`: `username` existente
  devolve `AuthenticatedUserDetails` com as `authorities` corretas;
  `username` inexistente lança `UsernameNotFoundException`.

---

## Prompt 2.5 — `JwtAuthFilter`, `SecurityConfig` definitivo e tratamento de 401/403

### Contexto para o agente

Este prompt substitui o `SecurityConfig` temporário e liga todas as
peças construídas nos prompts anteriores. Inclui também um aspeto não
coberto por `07-security.md`: exceções de segurança (`401`, `403`) são
lançadas na cadeia de filtros ou na camada de AOP do Spring Security,
**antes** de o pedido chegar ao `DispatcherServlet** — por isso,
`GlobalExceptionHandler` (um `@RestControllerAdvice`) não as intercepta
automaticamente. RNF-08 exige que **toda** resposta de erro siga RFC 7807
"sem exceções"; sem tratamento explícito, um `401`/`403` sairia com o
corpo por omissão do Spring Security, violando esse requisito.

### Instrução

1. Implementa `security/JwtAuthFilter.java`, estendendo
   `OncePerRequestFilter`:
   - Extrai o token do cabeçalho `Authorization: Bearer <token>`; se
     ausente, continua a cadeia sem definir o `SecurityContext` (deixa a
     decisão de bloquear para `SecurityConfig`).
   - Se presente, chama `JwtService.parseAndValidate`; em caso de
     sucesso, constrói um `AuthenticatedPrincipal` (Prompt 2.4) e um
     `UsernamePasswordAuthenticationToken(principal, null, authorities)`,
     definindo-o no `SecurityContextHolder`.
   - Em caso de token inválido/expirado, **não** relança a exceção nem
     escreve a resposta diretamente — regista um aviso (sem expor
     detalhes internos, conforme `.cursorrules`, secção 4) e deixa o
     `SecurityContext` vazio; a rejeição final (401) é responsabilidade
     do `AuthenticationEntryPoint` configurado no passo 3.
2. Reescreve `security/SecurityConfig.java`:
   - Remove o `permitAll()` temporário.
   - `sessionManagement` com `SessionCreationPolicy.STATELESS`.
   - `authorizeHttpRequests`: `permitAll()` apenas para
     `/api/v1/auth/**`; `authenticated()` para tudo o resto (a
     granularidade por `Role` fica a cargo de `@PreAuthorize`, Decisão 2).
   - Regista `JwtAuthFilter` com
     `.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`.
   - Configura CORS a partir de `CORS_ALLOWED_ORIGINS` (já referenciado
     em `07-security.md`, secção 6, mas nunca ligado ao `SecurityConfig`
     real) através de um `CorsConfigurationSource`.
   - Desativa `formLogin()` e `httpBasic()` explicitamente.
   - Adiciona `@EnableMethodSecurity` (Spring Security 6; substitui o
     antigo `@EnableGlobalMethodSecurity`) para habilitar `@PreAuthorize`.
3. Cria `security/RestAuthenticationEntryPoint.java`, implementando
   `AuthenticationEntryPoint`, que escreve diretamente uma resposta JSON
   no formato RFC 7807 (mesma forma usada por `GlobalExceptionHandler`,
   incluindo `type`, `title: "UNAUTHORIZED"`, `status: 401`, `detail`,
   `instance`) — não pode reutilizar `ProblemDetail` do Spring MVC
   diretamente porque este componente escreve a resposta fora do ciclo
   do `DispatcherServlet`; usa `ObjectMapper` para serializar
   manualmente uma estrutura equivalente.
4. Regista este `AuthenticationEntryPoint` em `SecurityConfig` via
   `.exceptionHandling(ex -> ex.authenticationEntryPoint(...))`.
5. Em `GlobalExceptionHandler.java`, adiciona um
   `@ExceptionHandler(AccessDeniedException.class)` devolvendo
   `403 FORBIDDEN` no mesmo formato RFC 7807 — este handler cobre as
   negações vindas de `@PreAuthorize`, que **sim** chegam ao
   `DispatcherServlet` (ao contrário da falta de autenticação, que é
   interceptada mais cedo na cadeia).

### Critério de teste

- Um pedido a qualquer *endpoint* protegido sem cabeçalho `Authorization`
  devolve `401`, corpo RFC 7807, `title: "UNAUTHORIZED"`.
- Um pedido com um token JWT válido mas de um utilizador sem a `Role`
  exigida por um `@PreAuthorize` (a aplicar no Prompt 2.8) devolve `403`,
  corpo RFC 7807, `title: "FORBIDDEN"`.
- `POST /api/v1/auth/login` permanece acessível sem token (validar após
  o Prompt 2.6 estar concluído).
- Nenhuma resposta de erro de segurança expõe *stack trace* ou mensagem
  interna do `jjwt`.

---

## Prompt 2.6 — DTOs de autenticação, `AuthService` e `AuthController`

### Contexto para o agente

`docs/06-api-contract.md`, secção 2, já documenta o contrato de
`POST /api/v1/auth/login`. Este prompt implementa esse contrato, seguindo
a mesma separação de camadas (domain/application/infrastructure/
presentation) já aplicada aos restantes módulos — aqui aplicada dentro do
próprio pacote `security/`, criando `security/presentation/`,
`security/presentation/dto/` e `security/application/` como novos
subpacotes. **Nota:** os ficheiros já existentes em `security/`
(`JwtService`, `JwtAuthFilter`, `PasswordEncoderConfig`, `SecurityConfig`)
permanecem no pacote plano `security/` — não os movas nesta fase, para
evitar uma refatoração ampla e não testável (`.cursorrules`, secção 7);
regista-se esta inconsistência estrutural como um item de limpeza futura
opcional (ver Prompt 2.11).

### Instrução

1. Cria `security/presentation/dto/LoginRequest.java` — `record` com
   `@NotBlank String username` e `@NotBlank String password`, conforme
   `06-api-contract.md`, secção 2.
2. Cria `security/presentation/dto/AuthResponse.java` — `record` com
   `String token`, `String tokenType` (fixo em `"Bearer"`) e
   `long expiresIn` (segundos, calculado a partir de
   `security.jwt.expiration-minutes`).
3. Implementa `security/application/AuthService.java`:
   - Injeta `AuthenticationManager`, `JwtService` e `UserRepository`
     (ou reaproveita o principal já autenticado — preferir obter o
     `AuthenticatedUserDetails` diretamente do resultado de
     `authenticationManager.authenticate(...)`, evitando uma segunda
     consulta a `UserRepository`).
   - Expõe `AuthResponse login(LoginRequest request)`: invoca
     `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()))`
     — em caso de credenciais inválidas, o Spring Security já lança
     `BadCredentialsException` (subclasse de `AuthenticationException`);
     **não** capturar esta exceção aqui — deve propagar-se e ser tratada
     pelo mecanismo de segurança padrão (o `AuthenticationEntryPoint` do
     Prompt 2.5 só trata falhas na validação do token; uma falha de
     `login` com credenciais erradas deve ser mapeada separadamente —
     adiciona um `@ExceptionHandler(BadCredentialsException.class)` em
     `GlobalExceptionHandler`, devolvendo `401` RFC 7807 com
     `detail: "Credenciais inválidas"`, nunca indicando se foi o
     `username` ou a `password` que falhou).
   - Em caso de sucesso, extrai o `AuthenticatedUserDetails` do
     resultado, gera o token via `JwtService.generateToken(id, username, role)`
     e devolve o `AuthResponse`.
4. É necessário expor um *bean* `AuthenticationManager` — em Spring Boot
   3.x, obtém-se a partir de `AuthenticationConfiguration`
   (`authenticationConfiguration.getAuthenticationManager()`), declarado
   em `SecurityConfig` ou numa classe de configuração dedicada.
5. Implementa `security/presentation/AuthController.java`:
   `POST /api/v1/auth/login`, `@Valid @RequestBody LoginRequest`,
   devolve `200 OK` com `AuthResponse` (não `201`, por não se tratar da
   criação de um recurso).

### Critério de teste

- `POST /api/v1/auth/login` com credenciais válidas de um utilizador
  existente devolve `200` com `token`, `tokenType: "Bearer"` e
  `expiresIn: 3600` (assumindo o valor por omissão de 60 minutos).
- Credenciais inválidas devolvem `401` RFC 7807, sem indicar qual campo
  falhou.
- O token devolvido é aceite por um *endpoint* protegido (testar
  manualmente via Postman, encadeando com o Prompt 2.5 já concluído).

---

## Prompt 2.7 — Bootstrap do primeiro utilizador `ADMIN`

### Contexto para o agente

Conforme a Decisão 3 (confirmada com o refinamento por *placeholder*),
resolve-se o problema de arranque com uma migração Flyway de *seed* cujo
hash de *password* não fica gravado como literal, mas resolvido em tempo
de execução a partir de uma propriedade Spring Boot — o mesmo padrão já
usado para `DB_PASSWORD`/`JWT_SECRET` (Prompt 2.1). `V5` está livre
(confirmado em `docs/05-migrations.md`, secção 4, após a correção do
Prompt A da fase anterior); `V6` e `V7` continuam reservadas para a
auditoria de prioridade e o *refresh token*, respetivamente — não as
reutilizes.

### Instrução

1. Gera, fora do repositório (ex.: consola local com
   `BCryptPasswordEncoder`, ou um pequeno *main* descartável), o hash
   BCrypt de uma *password* de desenvolvimento conhecida (sugestão:
   documentar como `ChangeMe123!`, apenas para ambiente local).
2. Cria `src/main/resources/db/migration/V5__seed_default_admin_user.sql`,
   inserindo um único registo em `tb_users`: `username = 'admin'`,
   `email` de desenvolvimento, `role = 'ADMIN'`, `active = true`, e
   `password_hash = '${admin_password_hash}'` — o *placeholder* Flyway,
   **não** o hash literal.
3. Em `src/main/resources/application.yml`, adiciona a propriedade
   `spring.flyway.placeholders.admin-password-hash`, com valor
   `${ADMIN_BOOTSTRAP_PASSWORD_HASH:<hash-gerado-no-passo-1>}` — o
   mesmo padrão `${ENV_VAR:valor-por-omissão}` já usado em
   `application-dev.yml` para `DB_USERNAME`/`DB_PASSWORD` e em
   `application.yml` para `security.jwt.secret` (Prompt 2.1). Confirma
   que o Flyway resolve corretamente o *placeholder* durante o arranque
   (`spring.flyway.placeholders.*` é lido automaticamente pela
   autoconfiguração do Spring Boot; não é necessário código adicional).
4. Atualiza `docs/05-migrations.md`, secção 4: adiciona a linha
   `| V5 | Seed do utilizador ADMIN inicial, com hash parametrizado (bootstrap RN-08) | V1 |`,
   preenchendo a coluna de dependências corretamente.
5. Regista, em `docs/11-observability-and-runbook.md` (nova secção
   "Bootstrap de Acesso") ou em `README.md`, uma nota explícita: o valor
   por omissão de `ADMIN_BOOTSTRAP_PASSWORD_HASH` é apenas para
   desenvolvimento local; qualquer ambiente exposto fora da máquina do
   programador **deve** definir esta variável com um hash próprio antes
   do primeiro arranque, tal como já se recomenda para `JWT_SECRET`.
   Como não existe ainda `PATCH` de *password* de utilizador no âmbito
   atual do projeto, assinala a rotação pós-arranque como lacuna a
   considerar numa fase futura (fora do âmbito da Fase 2).

### Critério de teste

- `mvn flyway:migrate` (ou o arranque normal da aplicação, com
  `baseline-on-migrate` já ativo) aplica `V5` sem erros, com o
  *placeholder* corretamente substituído (confirmar por inspeção da
  tabela `tb_users` após o arranque — o `password_hash` gravado deve
  corresponder ao hash BCrypt, nunca à literal `${admin_password_hash}`).
- `POST /api/v1/auth/login` com `username: "admin"` e a *password* de
  desenvolvimento devolve um token válido cujo *claim* `roles` contém
  `ADMIN`.
- Alterar `ADMIN_BOOTSTRAP_PASSWORD_HASH` no ambiente e recriar a base de
  dados local (`docker compose down -v && docker compose up -d`) resulta
  num `admin` com a nova *password*, sem qualquer alteração ao ficheiro
  `V5__seed_default_admin_user.sql`.

---

## Prompt 2.8 — Autorização por `Role` em todos os *endpoints* existentes

### Contexto para o agente

Com o *login* funcional e um `ADMIN` de arranque disponível, este prompt
fecha RF-12, RN-01, RN-02, RN-07 e RN-08, removendo todos os comentários
`// TODO(Fase 2): restringir por Role...` ainda pendentes. A tabela de
`07-security.md`, secção 5, está incompleta face ao CRUD real (criado na
Fase 1, Prompt C); a tabela consolidada abaixo é a referência a usar
nesta implementação — e deve substituir a tabela atual desse documento.

| *Endpoint* | ADMIN | DEVELOPER | VIEWER | Origem |
|---|---|---|---|---|
| `GET /api/v1/issues`, `GET /{id}` | ✅ | ✅ | ✅ | RN-07 |
| `POST /api/v1/issues` | ✅ | ✅ | ❌ | `07-security.md` |
| `PATCH /{id}/status` | ✅ | ✅ | ❌ | RN-01 |
| `PATCH /{id}/priority` | ✅ | ❌ | ❌ | RN-02 |
| `PATCH /{id}/assignee` | ✅ | ✅ | ❌ | RF-16 |
| `PATCH /{id}/details` | ✅ | ✅ | ❌ | RF-19 |
| `DELETE /{id}` | ✅ | ❌ | ❌ | `06-api-contract.md`, 3.7 |
| `POST /{issueId}/comments` | ✅ | ✅ | ❌ | `07-security.md` |
| `GET /{issueId}/comments` | ✅ | ✅ | ✅ | `07-security.md` |
| `GET /api/v1/notifications` | ✅ | ✅ | ✅ | qualquer utilizador autenticado, apenas as suas próprias |
| `POST /api/v1/users` | ✅ | ❌ | ❌ | RN-08 |

### Instrução

1. Em `IssueController`, substitui cada `// TODO(Fase 2)` por
   `@PreAuthorize(...)` correspondente à tabela acima (ex.:
   `@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")` para `/status`;
   `@PreAuthorize("hasRole('ADMIN')")` para `/priority` e para `DELETE`).
2. Em `CommentController`, aplica `@PreAuthorize` ao `POST` (`ADMIN`,
   `DEVELOPER`); o `GET` fica apenas `authenticated()` (já garantido por
   `SecurityConfig`, sem `Role` adicional).
3. Em `UserController.create`, aplica
   `@PreAuthorize("hasRole('ADMIN')")`.
4. Atualiza a tabela da secção 5 de `docs/07-security.md` com a versão
   consolidada acima, e atualiza o cabeçalho `última-atualização`.
5. Atualiza `docs/STATUS.md`: remove as notas de scaffolding relativas a
   "SecurityFilterChain temporário" e "Autorização de Role em endpoints
   de Issue", substituindo-as por uma entrada única confirmando a
   conclusão da Fase 2 nesse aspeto.

### Critério de teste

- Autenticado como `admin` (`V5`), todos os *endpoints* da tabela
  respondem com sucesso.
- Um utilizador `VIEWER` (criar um via `POST /users` autenticado como
  `admin`) recebe `403` em `POST /issues`, `PATCH /status`,
  `PATCH /priority`, `POST /comments` e `POST /users`; recebe `200` em
  `GET /issues` e `GET /comments`.
- Um utilizador `DEVELOPER` recebe `403` em `PATCH /priority` e
  `DELETE /{id}`, mas `200`/`204` nos restantes.

---

## Prompt 2.9 — Remover *scaffolding* temporário e corrigir a auditoria RN-03

### Contexto para o agente

Com `AuthenticatedPrincipal` disponível em cada pedido autenticado
(Prompt 2.5), os campos `reporterId`, `authorId` e o parâmetro de
consulta `recipientId`, todos marcados como scaffolding temporário,
deixam de ser necessários — o utilizador autenticado passa a ser extraído
diretamente do `SecurityContext` via `@AuthenticationPrincipal`.
Simultaneamente, corrige-se `UpdateIssueUseCase.overridePriority`, cujo
`responsible` está fixo em `"UNKNOWN"` — uma lacuna já assinalada no
próprio código com `// TODO(Fase 2)`.

### Instrução

1. Em `CreateIssueRequest`, remove o campo `reporterId` e o respetivo
   comentário `TODO`. Em `IssueController.create`, adiciona o parâmetro
   `@AuthenticationPrincipal AuthenticatedPrincipal principal` e passa
   `principal.id()` para `CreateIssueUseCase.execute`, que passa a
   receber o `reporterId` como argumento separado (em vez de o ler do
   DTO) — ajusta a assinatura de `CreateIssueUseCase.execute` em
   conformidade.
2. Aplica o mesmo padrão a `CreateCommentRequest`/`authorId` e a
   `CommentController`/`CreateCommentUseCase`.
3. Em `NotificationController.listForUser`, remove o parâmetro de
   consulta `recipientId` e usa `principal.id()` diretamente.
4. Em `UpdateIssueUseCase.overridePriority`, adiciona um parâmetro
   `String responsibleUsername` (ou `UUID responsibleId`, a preferir
   `username` por ser mais legível no log de auditoria) e substitui a
   variável fixa `"UNKNOWN"` pelo valor recebido. Em
   `IssueController.overridePriority`, extrai `principal.username()` e
   passa-o ao caso de uso.
5. Atualiza `docs/STATUS.md`, removendo as quatro notas de scaffolding
   correspondentes (`reporterId`, `authorId`, `recipientId`,
   "Autorização ADMIN em POST /users" — esta última já coberta pelo
   Prompt 2.8) e substituindo-as por uma referência à Fase 2 como
   concluída nestes pontos.

### Critério de teste

- `POST /api/v1/issues` sem `reporterId` no corpo (campo removido do
  DTO) cria a *issue* com o `reporter` correspondente ao utilizador
  autenticado.
- O mesmo para comentários (`author`) e para a listagem de notificações
  (`recipient`).
- `PATCH /{id}/priority` como `admin` regista, no log, o `responsible`
  como `"admin"` (ou o `username` efetivo), não `"UNKNOWN"`.
- Nenhum destes *endpoints* aceita mais um `UUID` de outro utilizador
  fornecido no corpo ou na consulta — confirma que não é possível
  criar uma *issue* "em nome de" outro utilizador.

---

## Prompt 2.10 — Testes unitários da camada de segurança

### Contexto para o agente

`.cursorrules`, secção 5, exige teste unitário para toda nova
funcionalidade de serviço; `10-testing-strategy.md` já reserva
`security/JwtServiceTest.java` na estrutura planeada. Este prompt cobre a
lógica nova introduzida na Fase 2, sem depender de Spring context
(JUnit 5 + Mockito, tal como os testes já existentes de `IssueTest` e
`CreateIssueUseCaseTest`), à exceção do teste de integração mínimo do
ponto 3, que pode usar `@SpringBootTest` com Testcontainers do
PostgreSQL, seguindo o padrão já enunciado em `10-testing-strategy.md`,
secção 3.

### Instrução

1. `security/JwtServiceTest.java`: gerar e validar um token com sucesso;
   token com assinatura adulterada lança a exceção esperada; token
   expirado lança a exceção esperada; os *claims* extraídos (`uid`,
   `username`, `roles`) correspondem exatamente ao que foi gerado.
2. `security/JwtAuthFilterTest.java` (Mockito puro, sem contexto Spring):
   pedido com token válido define o `SecurityContext` com o
   `AuthenticatedPrincipal` correto; pedido sem cabeçalho `Authorization`
   não define autenticação e continua a cadeia; pedido com token inválido
   idem, sem lançar exceção para fora do filtro.
3. `security/application/AuthServiceTest.java`: credenciais válidas
   devolvem `AuthResponse` com token não vazio; credenciais inválidas
   propagam `BadCredentialsException` (verificar apenas que é lançada,
   não testar aqui o mapeamento HTTP, que pertence à camada de
   `GlobalExceptionHandler`).
4. (Opcional, mas recomendado) Um teste de integração mínimo em
   `src/test/java/.../security/AuthFlowIntegrationTest.java`, com
   `@SpringBootTest` e Testcontainers PostgreSQL, validando o fluxo
   completo: `POST /auth/login` com o `admin` do *seed* (Prompt 2.7) →
   token válido → `GET /api/v1/issues` com esse token → `200`.
5. Atualiza `docs/STATUS.md`: adiciona uma nova secção "Módulo:
   Segurança" com o estado `✅`/`⚡` de cada componente, análogo às
   secções já existentes para Issue/Comment/Notification/User.

### Critério de teste

- `mvn test` executa todos os testes com sucesso, incluindo os já
  existentes (sem regressão).
- Cobertura da nova lógica de `JwtService` e `JwtAuthFilter` cobre, no
  mínimo, os cenários de sucesso e de falha listados acima.

---

## Prompt 2.11 — Documentação final da Fase 2 e ADR-07

### Contexto para o agente

Encerramento da fase, seguindo o mesmo padrão de fecho já usado na Fase 1
("Phase closure requires end-to-end Postman validation"). Regista-se
formalmente a Decisão 3 (bootstrap do `ADMIN`) e a Decisão 1 (*claims* do
JWT) como ADR, por serem decisões arquiteturais com alternativas
rejeitadas e consequências — exatamente o critério já usado para os
ADR-01 a ADR-06 existentes.

### Instrução

1. Cria `docs/adr/adr-07-jwt-bootstrap-e-claims.md`, a partir de
   `docs/adr/template.md`, documentando as Decisões 1 e 3 desta fase
   (contexto, decisão, alternativas consideradas — reaproveitar o texto
   já elaborado na secção "Decisões Arquiteturais Desta Fase" deste
   ficheiro — e consequências, incluindo o refinamento por *placeholder*
   Flyway e a obrigação de definir `ADMIN_BOOTSTRAP_PASSWORD_HASH` fora
   de ambiente de desenvolvimento local).
2. Atualiza `docs/adr/README.md`, adicionando a entrada
   `| 07 | [Bootstrap de acesso e claims JWT](adr-07-jwt-bootstrap-e-claims.md) | Aceite |`.
3. Marca, em `docs/07-security.md`, secção 9 (Checklist OWASP), os itens
   agora cumpridos: segredo JWT externo via `JWT_SECRET` (Prompt 2.1).
   Os itens de *rate limiting* e *Content Security Policy* permanecem
   corretamente pendentes — não os marques.
4. Atualiza `README.md`, secção "Roteiro de Desenvolvimento": nenhuma
   alteração estrutural é necessária (a tabela já lista a Fase 2
   corretamente), mas confirma que a descrição continua precisa após
   esta implementação.
5. Regista, como item de limpeza opcional para uma fase futura (não
   executar agora): reorganizar `security/` em subpacotes
   `domain/application/infrastructure/presentation`, alinhando os
   ficheiros pré-existentes (`JwtService`, `JwtAuthFilter`,
   `PasswordEncoderConfig`) com a estrutura já criada neste prompt para
   `AuthController`/`AuthService` (ver nota do Prompt 2.6).
6. (Opcional) Considera renomear a secção 6 de
   `docs/01-requirements.md` ("Fase 2 — Funcionalidades Diferidas") para
   evitar colisão de nomenclatura com a Fase 2 do roteiro de
   desenvolvimento (Segurança) — são conceitos distintos (um é um
   agrupamento de funcionalidades adiadas para além do MVP; o outro é a
   fase de segurança do roteiro de 9 fases). Um nome como "Funcionalidades
   Diferidas Pós-MVP" elimina a ambiguidade. Sinaliza esta sugestão, mas
   não a apliques sem confirmação, por ser uma alteração de nomenclatura
   que atravessa referências cruzadas noutros documentos.

### Critério de teste

- Validação manual completa via Postman (ou coleção equivalente) de
  todos os fluxos afetados pela Fase 2: *login*, criação de *issue* sem
  `reporterId` no corpo, `PATCH` de prioridade com auditoria correta,
  `403` para `Role` insuficiente, `401` para token ausente/inválido —
  espelhando a exigência de "validação end-to-end via Postman" já
  aplicada ao fecho da Fase 1.
- `docs/STATUS.md` reflete fielmente o estado real do código, sem
  nenhuma nota de scaffolding relativa a JWT/Role ainda pendente,
  exceto as explicitamente fora de âmbito (Alternativa B do bootstrap,
  se não adotada; rotação de *password* do `admin` de *seed*).

---

## Resumo de Sequenciamento

| Prompt | Depende de | Bloqueia o fecho da Fase 2? |
|---|---|---|
| 2.1 — Propriedades JWT | Nenhum | Sim — todos os seguintes dependem da configuração |
| 2.2 — `findByUsername` | Nenhum | Sim — necessário para o *login* |
| 2.3 — `JwtService` | 2.1 | Sim |
| 2.4 — `UserDetailsServiceImpl` | 2.2 | Sim |
| 2.5 — Filtro + `SecurityConfig` + 401/403 | 2.3, 2.4 | Sim |
| 2.6 — `AuthController`/`AuthService` | 2.4, 2.5 | Sim |
| 2.7 — Bootstrap `ADMIN` | Nenhum (pode correr em paralelo com 2.1–2.6) | Sim — necessário para testar 2.8 |
| 2.8 — Autorização por Role | 2.5, 2.6, 2.7 | Sim |
| 2.9 — Remover scaffolding + auditoria RN-03 | 2.5, 2.8 | Sim |
| 2.10 — Testes unitários | 2.3, 2.4, 2.6, 2.9 | Sim — rede de segurança antes da Fase 3 |
| 2.11 — Documentação e ADR-07 | Todos os anteriores | Sim — fecho formal da fase |

---

## Critérios de Saída da Fase 2

Espelhando `docs/01-requirements.md`, secção 8 ("Critérios de Sucesso do
Projeto"), a Fase 2 considera-se concluída quando:

1. Nenhum `// TODO(Fase 2)` permanece no código-fonte.
2. Todos os *endpoints* protegidos exigem um JWT válido e respeitam a
   tabela RBAC consolidada no Prompt 2.8.
3. Toda resposta de erro de segurança (`401`, `403`) segue RFC 7807, sem
   exceção — conforme RNF-08.
4. `docs/STATUS.md`, `docs/07-security.md` e `docs/05-migrations.md`
   refletem fielmente o estado real do código.
5. A decisão de bootstrap do `ADMIN` está registada em ADR-07, incluindo
   o risco assumido e a mitigação documentada.