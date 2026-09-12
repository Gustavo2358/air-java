# AGENTS.md

## Política de engenharia vigente

[LEAN HARNESS / GIT-IS-THE-RECORD](docs/engineering/lean-harness.md) governa o trabalho.
Git, commits, Pull Requests, testes e merge são a fonte de verdade do desenvolvimento.
Remote FAST only; full local/on-demand. Receipts e certificados CP não são requisitos.
História é READ_ONLY / BEST_EFFORT; registry/index servem à navegação.
Work items novos usam id/title/status/scope, com TODO/IN_PROGRESS/BLOCKED/DONE.
PR merged + required technical tests passed = DONE. Pins cross-repo permanecem estritos.
Mudanças semânticas importantes exigem revisão humana; metadata não exige cerimônia.
Execute `python3 -B scripts/harness/lean.py fast`; full local quando tecnicamente
necessário: `python3 -B scripts/harness/lean.py qualification-local`.
Preserve branches dedicadas, escopo, mudanças alheias e isolamento entre repositórios.
Não faça merge/auto-merge sem autorização.


## Missão e autoridade

Biblioteca Java compartilhada do modelo imutável e Validator estrutural AIR.
[docs/sources.lock.json](docs/sources.lock.json) fixa a autoridade normativa em
`Gustavo2358/analysis-ir`. A implementação não redefine AIR nem certifica por si
só a verdade do produtor ou um perfil de conformidade.

## Comece pelo índice

[Trabalho atual](docs/work/index.md) e [índice geral](docs/index.md) são navegação.
Carregue contexto por dependência concreta. História não é instrução atual.

| Decisão | Contexto mínimo |
| --- | --- |
| Uso e cobertura atual | [README](README.md) e [status](docs/implementation-status.md) |
| Fronteiras e dependências | [arquitetura](ARCHITECTURE.md), [boundary atual](docs/architecture/boundaries.md) e [invariantes](docs/architecture/invariants.md) |
| Identidade, tipos, premissas, incerteza | [contrato do domínio](docs/domain/model-and-validation.md) → seção normativa necessária |
| Nova variante / evolução de API | [extensibilidade](docs/architecture/extensibility.md) e [controle de mudanças](docs/engineering/change-control.md) |
| Algoritmo ou diagnóstico | [política semântica](docs/engineering/semantic-policy.md) e [testes](docs/engineering/testing.md) |
| Escala e terminação | [performance](docs/engineering/performance.md) |
| Gate, CI e documentação | [gates](docs/engineering/gates.md) e [evals](docs/evals/index.md) |
| Iniciar/retomar/encerrar trabalho | [lifecycle](docs/engineering/work-item-protocol.md), [sessão e Git](docs/engineering/session-and-review.md) e [templates](docs/templates/README.md) |
| Roadmap / codec futuro | [missão e roadmap](docs/product/mission-and-roadmap.md) e [baseline](docs/sources/baseline.md) |

## Regras universais

- Preserve imutabilidade transitiva, identidades completas e versões distintas.
- `air-model` produz o artefato `air-java` com model + validation; `air-json` contém
  o codec 1A e depende diretamente dele. [Política/suíte JSON](docs/engineering/air-json.md) obrigatórias.
- `model` não depende de `validation`; ambos dependem apenas de `java.base`, dentro
  da política de packages do gate. JSON/Jackson/Gson, arquivo, rede, CLI, frameworks,
  COBOL, CFG, effects calculados, RD e values calculados ficam fora do núcleo.
- `unknown_type` não é wildcard; incerteza compartilhada não prova `sameDomain`.
  Não escolha candidatos, storage independente, zero, nomes normalizados ou
  fallthrough intersequence por conveniência.
- Novas variantes exigem autoridade, traversal, Validator, testes e catálogo.
  A biblioteca valida estruturas; não repara publicações nem executa envelopes.
- Checks não implementados, obrigações semânticas e limites precisam ser explícitos.
  Nunca promova `INCOMPLETE_VALIDATION` a validade para acomodar um caller.
- Capacidade limita formas, não cardinalidade. Indexe joins; evite varrer inventário
  ou todas as premissas novamente para cada operação.
- Regra → classes → oracle independente → RED → implementação → GREEN; challenge local sob demanda. Testes negativos falham pela regra identificável; expected não
  deriva do Validator sob teste. Preserve falsificações como regressão.
- Backlog e roadmap descrevem futuro; não autorizam implementação. A autorização
  da sessão governa o trabalho; registre-a sem inventar aprovações intermediárias.
- Preserve mudanças alheias. Sem reset/stash/discard, force-push, merge, auto-merge
  ou publicação Maven sem autorização aplicável.

## Verificação e handoff

Execute `python3 -B scripts/harness/lean.py fast`: compile offline, contratos de
modelo/codec e fronteiras de módulos. Full local quando tecnicamente necessário:
`python3 -B scripts/harness/lean.py qualification-local`.

Reporte diff, commit/PR, testes executados e resultados, limites e próximo passo.
Pare no PR para revisão humana. Não há gate de histórico, manifesto ou receipt.
