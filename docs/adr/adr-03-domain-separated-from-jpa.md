---
status: aceite
última-atualização: 2026-07-20
responsável: matevz77
---

# ADR-03 — Separação entre Entidades de Domínio e JPA

## Contexto

Na arquitetura Spring convencional, as entidades JPA (`@Entity`) servem simultaneamente como modelo de domínio e modelo de persistência. Isto cria acoplamento entre o domínio e a infraestrutura de base de dados.

## Decisão

**Manter entidades de domínio puras** (POJOs sem anotações JPA) e **entidades JPA separadas** na camada de infraestrutura, com conversão via mapeadores (MapStruct).

```
domínio: Issue.java  ←→  infraestrutura: IssueJpaEntity.java
                              através de IssueMapper.java (MapStruct)
```

## Alternativas Consideradas

### Alternativa A: Entidades JPA como entidades de domínio (abordagem padrão Spring)

- **Prós:** menos ficheiros, menos código de mapeamento, abordagem familiar.
- **Contras:** entidades contaminadas com anotações de persistência; lógica de domínio misturada com detalhes de ORM; difícil de testar o domínio sem carregar o contexto JPA; mudanças no schema afetam diretamente o modelo de domínio.
- **Razão para rejeitar:** viola o princípio da arquitetura hexagonal (o domínio não deve depender de infraestrutura).

### Alternativa B: MapStruct como alternativa a mapeamento manual

- **Prós:** geração automática de código de mapeamento, zero boilerplate.
- **Contras:** dependência de processador de anotações; configuração adicional no pom.xml.
- **Escolhida:** MapStruct é leve e amplamente adotado; o custo de configuração é mínimo.

## Consequências

### Positivas

- Domínio completamente desacoplado da persistência.
- Entidades de domínio podem usar construtores, fábricas e regras sem interferência JPA.
- Testes unitários do domínio não precisam de carregar contexto Spring.

### Negativas / Trade-offs

- Duplicação parcial de atributos (Issue.java e IssueJpaEntity.java).
- Necessidade de manter mapeadores sincronizados com alterações em ambas as classes.
- Ligeiro overhead de performance no mapeamento (negligenciável para o volume esperado).
