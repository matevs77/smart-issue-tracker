---
status: proposto
última-atualização: 2026-07-24
responsável: matevz77
---

# Prompts de Implementação — Fase 3 (Virtual Threads)

Este ficheiro segue a convenção já validada em `prompts-correcao-pre-fase2.md`
e em `prompts-fase2-seguranca.md`: prompts sequenciais, cada um
independentemente testável, com o raciocínio arquitetural explicado antes da
instrução de implementação, conforme `.cursorrules`, secção 7. A numeração
decimal (`3.x`) segue o padrão já adotado em `prompts-fase2-seguranca.md`.

**Âmbito da Fase 3** (conforme `README.md` e `inicial.md`, secção "Fase 3 —
Virtual Threads"): ativar a concorrência baseada em Virtual Threads
(Project Loom) como mecanismo transversal do sistema, e preparar — sem ainda
integrar — o executor reutilizável que as Fases 4 e 5 (Kafka e RabbitMQ)
injetarão nos respetivos consumidores. Fora de âmbito: a validação empírica
formal de desempenho (RNF-01, "redução de 60% de latência"), que exige
metodologia reprodutível com JMH/k6 e pertence exclusivamente à Fase 8,
conforme `docs/10-testing-strategy.md`, secção 4; e a integração efetiva do
executor nos consumidores Kafka/RabbitMQ, que só faz sentido quando esses
consumidores existirem (`KafkaConfig.java` e `RabbitMqConfig.java`
permanecem, à data deste ficheiro, vazios).

**Nota sobre decisão arquitetural:** ao contrário da Fase 2, que exigiu
registar novas decisões (ADR-07), a Fase 3 **implementa** uma decisão já
registada e aceite em `docs/adr/adr-04-virtual-threads.md`. Não é, por isso,
necessária uma secção de "Decisões Arquiteturais Desta Fase" — o Prompt 3.5
limita-se a anotar essa ADR já existente com a confirmação de implementação,
sem alterar o seu conteúdo decisório.

---

## Estado Atual (Auditado)

| Componente | Estado | Nota |
|---|---|---|
| `application.yml` | 🔧 Placeholder | `spring.threads.virtual.enabled: false` — a inverter no Prompt 3.1 |
| `VirtualThreadConfig.java` | 📋 Vazio | Ficheiro existe, 0 linhas (`docs/STATUS.md`, secção "Configuração") — a implementar no Prompt 3.2 |
| `KafkaConfig.java` / `RabbitMqConfig.java` | 📋 Vazios | Pertencem às Fases 4 e 5; o bean desta fase fica pronto para injeção futura, mas a integração efetiva não é âmbito deste ficheiro |
| `docs/adr/adr-04-virtual-threads.md` | ✅ Aceite | Já documenta a decisão e o exemplo de código de referência para o `ConcurrentKafkaListenerContainerFactory`; será anotado como implementado no Prompt 3.5 |
| `docs/08-messaging.md`, secção 1.5 | ✅ Estável | Contém o exemplo de código que o bean do Prompt 3.2 deve respeitar, para não introduzir divergência de documentação |
| `docs/11-observability-and-runbook.md`, secção 2.1 | ⚡ Parcial | Já lista `jvm_threads_live_threads` como métrica monitorizada — reutilizada como instrumento de verificação no Prompt 3.3 |
| `docs/10-testing-strategy.md`, secção 4 | 📋 Planeado | Testes de carga formais (JMH/k6) pertencem à Fase 8 — fora de âmbito aqui |
| Suite de testes existente (`mvn test`) | ✅ Disponível | `IssueTest`, `CommentTest`, `NotificationTest`, `CreateIssueUseCaseTest`, `CreateUserUseCaseTest`, `GlobalExceptionHandlerTest`, `JwtServiceTest`, `JwtAuthFilterTest`, `AuthServiceTest`, `AuthFlowIntegrationTest` — usada como rede de regressão no Prompt 3.4 |

---

## Prompt 3.1 — Ativar Virtual Threads na configuração global

### Contexto para o agente

`application.yml` define atualmente `spring.threads.virtual.enabled: false`.
A partir do Spring Boot 3.2+, esta propriedade, quando `true`, faz com que o
conetor Tomcat embebido e o executor de tarefas por omissão da aplicação
passem a usar Virtual Threads de forma automática — sem exigir nenhum *bean*
adicional para este efeito específico (o *bean* dedicado do Prompt 3.2 serve
um propósito distinto: a injeção futura nos consumidores Kafka/RabbitMQ, que
**não** são automaticamente cobertos por esta propriedade global). Esta é
precisamente a distinção que `inicial.md`, secção "Fase 3", já assinala:
"Configura o executor de Virtual Threads (...) e integra-o no Tomcat embebido
(via `application.yml`)".

**Nota técnica relevante:** uma limitação historicamente conhecida de Virtual
Threads era o *thread pinning* em blocos `synchronized` (o que anulava
parcialmente o ganho de escalabilidade em código que usa JDBC/Hibernate de
forma síncrona dentro dessas secções). Esta limitação foi resolvida a partir
do JDK 24 (JEP 491); como o projeto usa Java 25, não está exposto a este
problema. Mantém-se, no entanto, uma variável distinta e não resolvida
automaticamente: o dimensionamento do *pool* de ligações HikariCP, que
continua limitado ao número de ligações físicas disponíveis à base de dados,
independentemente do número de Virtual Threads em voo. O projeto não define
atualmente `spring.datasource.hikari.*` de forma explícita (usa os valores
por omissão do Spring Boot). Este prompt **não** exige o ajuste desse
*pool* — ADR-04 já identifica esta questão como um *trade-off* conhecido, a
reavaliar apenas se os testes de carga da Fase 8 revelarem esgotamento de
ligações sob carga concorrente elevada.

### Instrução

1. Em `application.yml`, altera `spring.threads.virtual.enabled` de `false`
   para `true`.
2. Confirma que nem `application-dev.yml` nem `application-prod.yml`
   sobrepõem esta propriedade (nenhum dos dois ficheiros a referencia
   atualmente). Não adicionar overrides desnecessários — a propriedade deve
   permanecer definida uma única vez, na configuração base.
3. Não alterar a configuração do HikariCP nesta fase. Adiciona, no entanto,
   um comentário YAML junto da propriedade alterada, referindo que o
   dimensionamento do *pool* de ligações deve ser reavaliado à luz dos
   resultados da Fase 8, caso os testes de carga revelem esgotamento de
   ligações — este comentário serve de ligação explícita à nota técnica já
   registada em `docs/adr/adr-04-virtual-threads.md`, secção "Consequências
   / Negativas".
4. Não é necessária nenhuma dependência Maven adicional — Virtual Threads são
   nativas desde o Java 21 e o `pom.xml` já declara `java.version=25`.

### Critério de teste

- A aplicação arranca sem exceções (`mvn spring-boot:run`, perfil `dev`), e
  o log de arranque confirma a inicialização do Tomcat embebido com Virtual
  Threads ativas.
- Os *endpoints* já existentes (`POST /api/v1/auth/login`,
  `GET /api/v1/issues`) continuam a responder corretamente após a ativação,
  sem qualquer alteração de comportamento funcional — apenas o mecanismo de
  concorrência subjacente muda.

---

## Prompt 3.2 — Implementar `VirtualThreadConfig.java`

### Contexto para o agente

`VirtualThreadConfig.java` existe como ficheiro vazio desde a fase de
estrutura inicial do projeto (`docs/STATUS.md`, secção "Configuração"). O seu
propósito, conforme `docs/08-messaging.md`, secção 1.5, e o exemplo de código
já documentado em `docs/adr/adr-04-virtual-threads.md`, é expor um executor
de Virtual Threads reutilizável, a injetar futuramente no
`ConcurrentKafkaListenerContainerFactory` (Fase 4) e no equivalente para
RabbitMQ (Fase 5) — **não** para substituir a ativação global já feita no
Prompt 3.1, que cobre o Tomcat e o executor de tarefas assíncronas por
omissão da aplicação de forma automática.

Como `KafkaConfig.java` e `RabbitMqConfig.java` permanecem vazios nesta fase,
este prompt limita-se a criar o *bean* reutilizável, sem o injetar em nenhum
consumidor concreto — essa integração pertence, por definição, às Fases 4 e
5, e antecipá-la aqui violaria a disciplina de âmbito já estabelecida em
`.cursorrules`, secção 7.

**Nota de ciclo de vida:** `Executors.newVirtualThreadPerTaskExecutor()`
devolve um `ExecutorService` que, desde o JDK 19, implementa `AutoCloseable`
através do método `close()`. É importante que o Spring encerre este executor
corretamente no *shutdown* do contexto da aplicação, para evitar threads
penduradas ou avisos de recursos não libertados.

### Instrução

1. Implementa `config/VirtualThreadConfig.java` com uma única definição de
   `@Bean`, do tipo `java.util.concurrent.ExecutorService`, devolvendo
   `Executors.newVirtualThreadPerTaskExecutor()` — mantendo exatamente o
   mesmo tipo de retorno já usado no exemplo de código de
   `docs/08-messaging.md`, secção 1.5, para que não exista divergência entre
   a documentação e a implementação quando a Fase 4 vier a consumir este
   *bean*.
2. Declara explicitamente `@Bean(destroyMethod = "close")` sobre o método,
   para que o Spring invoque `close()` no encerramento do contexto da
   aplicação — não depender apenas da inferência automática do Spring, para
   manter a intenção explícita e auditável no próprio código, conforme o
   estilo de resposta do agente definido em `.cursorrules`, secção 7.
3. Adiciona um comentário Javadoc na classe, explicando o propósito do
   *bean* e referenciando `docs/08-messaging.md`, secção 1.5, como o
   contrato de utilização futura.
4. Adiciona o comentário de scaffolding
   `// TODO(Fase 4/5): injetar este bean no ConcurrentKafkaListenerContainerFactory
   / no equivalente RabbitMQ quando os respetivos consumidores forem
   implementados`, seguindo a convenção já estabelecida no projeto para
   assinalar dependências futuras.
5. Não criar conteúdo em `KafkaConfig.java` ou `RabbitMqConfig.java` — ambos
   permanecem, corretamente, fora de âmbito nesta fase.
6. Este *bean* passa a ser a única instanciação de `ExecutorService`
   permitida no projeto até às Fases 4/5, em conformidade com
   `.cursorrules`, secção 4: "nunca criar `Thread` ou `ExecutorService`
   manual sem justificar" — este *bean* é, precisamente, essa instanciação
   justificada e centralizada.

### Critério de teste

- O projeto compila (`mvn compile`) com o novo *bean* disponível no contexto
  Spring.
- Um teste unitário simples confirma que uma tarefa submetida ao executor é
  executada numa Virtual Thread (`Thread.currentThread().isVirtual()` deve
  devolver `true` dentro da tarefa submetida).
- No encerramento da aplicação (`Ctrl+C` ou fim natural de um teste de
  contexto Spring), não são registados avisos ou exceções relacionados com
  o encerramento do executor.

---

## Prompt 3.3 — Validação empírica mínima da ativação (verificação qualitativa)

### Contexto para o agente

Este prompt distingue-se deliberadamente da validação formal de desempenho
da Fase 8. O objetivo aqui é puramente confirmar, de forma reprodutível mas
qualitativa, que os pedidos HTTP passam efetivamente a ser servidos por
Virtual Threads após a ativação do Prompt 3.1 — não medir ganhos de
desempenho, o que exigiria a metodologia formal (JMH/k6, dataset,
comparação com/sem VT) já reservada para `docs/10-testing-strategy.md`,
secção 4. Esta distinção é importante para não comprometer a integridade das
métricas que o portefólio pretende divulgar: qualquer número aqui observado
seria anedótico, não uma medição reprodutível, e não deve ser confundido com
a validação RNF-01.

`docs/11-observability-and-runbook.md`, secção 2.1, já lista
`jvm_threads_live_threads` como métrica monitorizada via Actuator/Prometheus
— esta métrica serve de instrumento natural para uma primeira observação,
mesmo antes de existir um dashboard Grafana dedicado (Fase 7).

### Instrução

1. Adiciona, temporariamente, uma linha de log (nível DEBUG, a remover no
   final deste prompt) num ponto de entrada HTTP já existente — sugestão:
   `IssueController.findAll` — que registe `Thread.currentThread()`.
   Confirma, por inspeção manual dos logs de um pedido real (`curl` ou
   Postman), que a *thread* que serve o pedido é uma Virtual Thread (o
   `toString()` de uma Virtual Thread inclui tipicamente o prefixo
   identificável `VirtualThread`).
2. Reverte esta instrumentação temporária assim que a confirmação for
   feita — seguindo o mesmo padrão já usado no projeto para diagnóstico de
   bugs, conforme documentado em `docs/STATUS.md`, secção "Notas de
   Scaffolding" (instrumentação temporária de log, seguida de reversão
   após diagnóstico).
3. Consulta `GET /actuator/metrics/jvm.threads.live` antes e depois da
   ativação (Prompt 3.1), sob uma rajada breve de pedidos concorrentes
   (por exemplo, 20 a 30 pedidos em paralelo via `curl` ou `ab`).
   Documenta a observação — não é exigido um relatório formal, apenas
   confirmar por inspeção que o número de *threads* de plataforma não
   cresce proporcionalmente ao número de pedidos concorrentes, o que seria
   o comportamento esperado de um *pool* convencional de tamanho fixo.
4. Regista esta observação, de forma sucinta, numa nova subsecção
   "Verificação Inicial (Fase 3)" em `docs/11-observability-and-runbook.md`,
   secção 2, distinguindo-a explicitamente da validação formal de
   desempenho da Fase 8 (RNF-01).

### Critério de teste

- A inspeção manual confirma Virtual Threads a servir pedidos HTTP.
- Nenhuma instrumentação de diagnóstico permanece no código-fonte após a
  conclusão deste prompt.
- A nova subsecção em `docs/11-observability-and-runbook.md` distingue
  claramente esta verificação exploratória da validação formal RNF-01 da
  Fase 8, evitando que a observação qualitativa aqui feita seja confundida
  com a métrica de "60% de redução de latência" alegada no portefólio —
  essa alegação só pode ser sustentada pelos testes reprodutíveis da
  Fase 8.

---

## Prompt 3.4 — Smoke test de regressão funcional pós-ativação

### Contexto para o agente

A mudança do modelo de concorrência, ainda que teoricamente transparente
para código imperativo (conforme argumentado em ADR-04, "Alternativa B:
WebFlux", que rejeita o modelo reativo precisamente por Virtual Threads
preservarem a programação imperativa), atravessa **todo** o caminho de
pedido HTTP do sistema. Não existe ainda uma suite de testes de integração
exaustiva; a rede de segurança disponível é composta pelos testes unitários
da Fase 1 (Prompt E) e da Fase 2 (Prompt 2.10), mais o teste de integração
`AuthFlowIntegrationTest`. Este prompt segue o mesmo padrão já usado no
fecho da Fase 1 ("Phase closure requires end-to-end Postman validation"),
aplicado aqui especificamente à deteção de regressões introduzidas pela
mudança de modelo de concorrência — não à validação de novas
funcionalidades, que não existem nesta fase.

Merece atenção particular o comportamento de `SecurityContextHolder` (modo
`MODE_THREADLOCAL`, o padrão por omissão do Spring Security, usado
implicitamente por `JwtAuthFilter` e por `@AuthenticationPrincipal`).
`ThreadLocal` continua a funcionar corretamente sob Virtual Threads — cada
Virtual Thread possui a sua própria cópia de `ThreadLocal`, tal como uma
*platform thread* — mas, por se tratar de um padrão de implementação
sensível a mudanças no modelo de concorrência subjacente, merece
confirmação explícita, e não apenas assumida por analogia teórica.

### Instrução

1. Reexecuta manualmente (via Postman ou coleção equivalente) os casos de
   teste end-to-end já validados no fecho da Fase 1, mais os fluxos de
   autenticação introduzidos na Fase 2 (login, acesso a *endpoint*
   protegido com token válido, `401` sem token, `403` com *Role*
   insuficiente) — confirmando que todos continuam a devolver os códigos
   HTTP e os corpos de resposta documentados em `docs/06-api-contract.md`
   após a ativação de Virtual Threads.
2. Executa `mvn test`, confirmando que toda a suite de testes unitários e
   de integração existente — incluindo `AuthFlowIntegrationTest`, que sobe
   um contexto Spring completo via Testcontainers — passa sem nenhuma
   regressão face ao estado anterior a esta fase.
3. Confirma explicitamente, com pelo menos um pedido autenticado por
   controlador (`IssueController`, `CommentController`,
   `NotificationController`), que a leitura de
   `@AuthenticationPrincipal AuthenticatedPrincipal` continua correta sob
   Virtual Threads — validando, na prática, a garantia teórica de
   `ThreadLocal` descrita no Contexto acima.
4. Documenta, em `docs/STATUS.md`, uma nota confirmando a validação de
   regressão pós-Fase 3, por analogia às notas de fecho de fase já
   existentes.

### Critério de teste

- `mvn test` executa com sucesso, sem nenhuma falha nova face ao estado
  imediatamente anterior à Fase 3.
- A validação manual end-to-end (Postman) não revela nenhuma regressão
  funcional face ao comportamento documentado em `docs/06-api-contract.md`.
- A leitura do `SecurityContext` via `@AuthenticationPrincipal` continua
  correta sob Virtual Threads nos três controladores mencionados.

---

## Prompt 3.5 — Fecho documental da Fase 3

### Contexto para o agente

Fecho da fase, seguindo o mesmo padrão já usado no Prompt E (Fase 1) e no
Prompt 2.11 (Fase 2): atualizar `docs/STATUS.md`, a ADR relevante e o
roteiro em `README.md`, para que a documentação reflita fielmente o estado
real do código. Note-se que, neste repositório, o campo `status` no
cabeçalho de metadados de uma ADR reflete a **aceitação da decisão**, não o
seu estado de **implementação** — `adr-04-virtual-threads.md` já tem
`status: aceite` desde antes desta fase, e esse valor não deve ser alterado;
quem regista o estado de implementação é, precisamente, `docs/STATUS.md`.

### Instrução

1. Em `docs/adr/adr-04-virtual-threads.md`, secção "Consequências" >
   "Positivas", acrescenta uma entrada confirmando a implementação efetiva
   (referenciando os Prompts 3.1 a 3.4 deste ficheiro), sem alterar o valor
   do campo `status` do cabeçalho de metadados. Atualiza o campo
   `última-atualização` para a data de execução deste prompt.
2. Em `docs/STATUS.md`, secção "Configuração": atualiza a linha
   `VirtualThreadConfig` de `📋 Ficheiro vazio (0 linhas)` para
   `⚡ Implementado (bean ExecutorService com Virtual Threads, preparado
   para injeção futura em Kafka/RabbitMQ — Fases 4/5)`. Atualiza também, na
   secção "Infraestrutura", a linha de `application.yml`, referindo a
   ativação de `spring.threads.virtual.enabled=true`.
3. Revê `README.md`, secção "Roteiro de Desenvolvimento": confirma que a
   descrição da Fase 3 ("Virtual Threads — Configuração do executor
   concorrente") permanece precisa após esta implementação; nenhuma
   alteração estrutural é esperada.
4. Não alteres `docs/10-testing-strategy.md` nem `docs/adr/README.md`
   nesta fase — a validação formal de desempenho (RNF-01) permanece
   corretamente associada, em exclusivo, à Fase 8, sem antecipação de
   conclusões.

### Critério de teste

- `docs/STATUS.md` reflete fielmente o estado real do código; nenhum item
  relativo à Fase 3 permanece classificado como `📋`.
- `docs/adr/adr-04-virtual-threads.md` regista a implementação sem
  comprometer a distinção entre "decisão aceite" e "validação de
  desempenho formal" — que permanece, corretamente, pendente da Fase 8.
- Revisão cruzada confirma que a referência a `VirtualThreadConfig` em
  `docs/08-messaging.md`, secção 1.5, continua tecnicamente coerente com a
  implementação real, para que a Fase 4 a possa reutilizar sem
  necessidade de correção adicional.

---

## Resumo de Sequenciamento

| Prompt | Depende de | Bloqueia o fecho da Fase 3? |
|---|---|---|
| 3.1 — Ativação global (`application.yml`) | Nenhum | Sim |
| 3.2 — `VirtualThreadConfig.java` | Nenhum (pode correr em paralelo com 3.1) | Sim |
| 3.3 — Validação empírica mínima | 3.1 | Sim |
| 3.4 — Smoke test de regressão funcional | 3.1, 3.2 | Sim |
| 3.5 — Fecho documental | 3.1, 3.2, 3.3, 3.4 | Sim |

---

## Critérios de Saída da Fase 3

Espelhando `docs/01-requirements.md`, secção 8, e o padrão de fecho já
aplicado à Fase 2, a Fase 3 considera-se concluída quando:

1. `spring.threads.virtual.enabled=true` está ativo e confirmado por
   observação direta (Prompt 3.3).
2. `VirtualThreadConfig.java` expõe um *bean* `ExecutorService` de Virtual
   Threads, com ciclo de vida corretamente gerido pelo contexto Spring,
   pronto para reutilização pelas Fases 4 e 5.
3. Nenhuma regressão funcional foi introduzida nos fluxos já validados nas
   Fases 1 e 2 (Prompt 3.4).
4. `docs/STATUS.md` e `docs/adr/adr-04-virtual-threads.md` refletem
   fielmente o estado real do código.
5. A validação empírica formal das métricas de desempenho (RNF-01, "60% de
   redução de latência") permanece — corretamente — associada à Fase 8, não
   antecipada nem parcialmente reivindicada nesta fase.