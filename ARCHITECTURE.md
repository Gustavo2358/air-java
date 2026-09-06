# Fronteiras

`model` não depende de `validation`; `validation` depende de `model`. Ambos usam
somente JDK. Testes/exemplos dependem de ambos. Não há porta de infraestrutura
obrigatória porque `Publication` e `ValidationResult` são valores devolvidos ao caller.

A integração inicial é:

```text
Semantic Product JSON -> adapter de entrada do cobol-lowering
-> lowerer -> Publication -> adapter AIR JSON de saída
-> arquivo -> adapter AIR JSON do analysis-cfg -> Publication -> BuildCfg
```

A integração em memória troca apenas o wiring e remove serialização intermediária.
Os adapters podem usar DTOs próprios, mix-ins ou serializers; não adicionam anotações
JSON ao modelo compartilhado. A especificação do binding é separada da implementação.

O validador constrói índices tipados uma vez. Resolução de `sameDomain` usa união de
classes de domínio e overlays por escopo estático, sem unificar lacunas de tipo.
Esse cálculo verifica um requisito de validade; não é resolução nominal, storage
analysis, reaching definitions ou análise de valores. Envelopes não são executados.

Os modelos são agrupados em classes de vocabulário (`Types`, `Operations`, `Memory`,
`Proofs` etc.) para manter nomes explícitos e contratos pequenos. Essa organização
Java não é uma nova taxonomia normativa e não deve aparecer como nome de classe num
payload JSON. Toda decisão de codificação é do binding/adapters.

A API inicial não inclui uma interface universal de extensões com payload livre.
As três extensões padronizadas são tipadas. Extensões de domínio/codec desconhecidas
conservam identidade/contrato e geram incompatibilidade de validação, não semântica
precisa inventada. Novas extensões precisas exigem contrato e slice próprio.
