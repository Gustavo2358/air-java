# Evals e claims

[Catálogo estruturado](catalog.json) liga invariantes aos gates.

| Eval | Prova local | Estado |
| --- | --- | --- |
| EVAL-AIR-001 | Integridade documental e lifecycle (docs) | implemented |
| EVAL-AIR-002 | Ownership, POMs e fronteira de bytecode (architecture) | implemented |
| EVAL-AIR-003 | Contratos existentes do modelo e Validator (semantic) | implemented |
| EVAL-AIR-004 | Reactor, grafo efetivo e suíte completa (maven) | implemented |
| EVAL-AIR-005 | Contracasos do próprio harness (harness) | implemented |
| EVAL-AIR-006 | Branch, base e escopo completos (git) | implemented |
| EVAL-AIR-007 | Codec contra binding pinado (transport) | implemented |
| EVAL-AIR-008 | Equivalência arquivo/memória (integration) | planned |
| EVAL-AIR-009 | Propriedades focalizadas de escala (performance) | planned |

EVAL-AIR-003 reutiliza os 172 checks identificados no [inventário](contract-checks.json).
As regras/assertions e fixtures continuam nos arquivos originais, sem cópia de
oráculos AIR. A suíte cobre identidade, imutabilidade, unknown, premissas, ownership,
versões, limites, determinismo e cenários grandes. Veja [status](../implementation-status.md)
para cobertura e limitações; quantidade de checks não certifica perfil normativo.

EVAL-AIR-005 desafia os mecanismos com link/anchor quebrado, JSON duplicado,
referência inexistente, lifecycle incoerente, scope de produto em harness,
saída vazia/duplicada/incompleta, Java com I/O/JSON/rede/dependência inversa e
Git com mudanças staged/unstaged/untracked/renomeadas fora de escopo.

EVAL-AIR-007 está implementado no gate transport e nas duas execuções do reactor.
EVAL-AIR-008/009 permanecem planejados: execução retorna UNAVAILABLE. Os 43 checks
1A comparam golden manual/Publication independente, preservação, canonicalização,
versões e limites tipados. Não são qualificação do catálogo inteiro ou do E2E.

EVAL-AIR-002/004/005 incluem o [contrato multi-módulo](../engineering/modularization-gates.md):
dependência externa no model é recusada por POM local/efetivo e grafo resolvido,
inclusive runtime/optional sem referência em bytecode. Model/validation conservam
a allowlist JDK. JSON exige implementação, suíte nominal e golden; código sem
política/evidência e modelo copiado/shaded dentro de JSON são contracasos RED.

[Evidência e limites do transporte](../quality/air-json-implementation.md);
[inventário nominal 1A](transport-checks.json).
