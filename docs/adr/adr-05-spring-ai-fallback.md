---
status: proposto
última-atualização: 2026-07-09
responsável: matevz77
---

# ADR-05 — Fallback na Classificação por IA

## Contexto

A classificação de prioridade via Spring AI é um requisito funcional central (RF-02). No entanto, a chamada a um LLM externo (OpenAI, Anthropic) está sujeita a:
- Timeouts de rede.
- Rate limits do provedor.
- Indisponibilidade temporária do serviço.
- Respostas mal formatadas.

A falha na classificação não deve impedir a criação da issue.

## Decisão

Implementar uma **estratégia de fallback em cascata**:

1. Tentar classificação com Spring AI (timeout de 5s).
2. Se falhar, retentar 1x.
3. Se persistir, usar `MEDIUM` como prioridade padrão com `confidence = 0.0`.
4. Registar em log e na métrica `issue_classification_fallback_total`.

```java
try {
    return aiService.classify(title, description);
} catch (AiException e) {
    log.warn("IA classification failed, using fallback: {}", e.getMessage());
    metrics.incrementFallbackCounter();
    return new PriorityClassification(MEDIUM, 0.0);
}
```

## Alternativas Consideradas

### Alternativa A: Bloquear a criação da issue se a IA falhar

- **Prós:** consistência dos dados (todas as issues têm prioridade classificada).
- **Contras:** degradação grave da experiência do utilizador; viola RNF-03.
- **Razão para rejeitar:** inaceitável para um sistema que se pretende resiliente.

### Alternativa B: Fallback para regras heurísticas (analisar palavras-chave no título)

- **Prós:** não depende de serviço externo; baixa latência.
- **Contras:** complexidade adicional; resultados menos precisos; duplicação de lógica.
- **Razão para rejeitar:** o fallback para MEDIUM é simples e eficaz; heurísticas representam outro sistema de classificação a manter.

## Consequências

### Positivas

- Criação de issue nunca é bloqueada por falha externa.
- Métrica de fallback permite monitorizar a saúde do provedor de IA.
- Comportamento simples e previsível.

### Negativas / Trade-offs

- Issues criadas durante uma falha da IA ficam com prioridade MEDIUM genérica.
- Um ADMIN precisa de reclassificar manualmente essas issues após recuperação.
- A métrica de precisão global pode ser ligeiramente distorcida (issues com fallback não são "classificações reais").
