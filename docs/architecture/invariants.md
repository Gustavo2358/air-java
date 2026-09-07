# Invariantes de implementação

Estes IDs roteiam as regras; não criam novos invariantes normativos AIR.
A autoridade segue o [source lock](../sources.lock.json).

| ID | Regra |
| --- | --- |
| INV-AIR-001 | Autoridade e versões: AIR normativa pinada; versão Java e binding independentes. |
| INV-AIR-002 | Fronteira do núcleo: model não depende de validation ou infraestrutura; validation depende de model. |
| INV-AIR-003 | Identidades e imutabilidade: Owners completos, snapshots defensivos, sem normalização de IDs. |
| INV-AIR-004 | Incerteza e obrigações: Unknown/partial/limites não viram certeza, vazio ou certificação. |
| INV-AIR-005 | Validação sem reparo: Validator não escolhe storage/candidatos/retorno nem infere controle. |
| INV-AIR-006 | Capacidade e escala: Capacidade limita formas; indexação e limites explícitos preservam inventários. |
| INV-AIR-007 | Evidência e escopo: Expected independente, gates executados, branch e mudança autorizada verificáveis. |

[Catálogo estruturado](invariants.json) permite checar referências de evals e work
items. [Domínio](../domain/model-and-validation.md) aponta às regras AIR pertinentes;
[evals](../evals/index.md) distinguem proteção existente de futura.
