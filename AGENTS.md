# AGENTS.md

## Missão e autoridade

Biblioteca Java compartilhada do modelo imutável e Validator estrutural AIR.
[docs/sources.lock.json](docs/sources.lock.json) fixa a autoridade normativa em
`Gustavo2358/analysis-ir`. A implementação não redefine AIR nem certifica por si
só a verdade do produtor ou um perfil de conformidade.

## Comece pelo índice

Leia [trabalho atual](docs/work/index.md), manifesto e `state.md` do item autorizado.
Carregue seu `must_read` e amplie por dependência concreta. O [índice geral](docs/index.md)
é mapa, não leitura integral obrigatória. História e evidências antigas não são
contexto padrão nem instruções atuais.

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
- Regra → classes → oracle independente → RED → implementação → GREEN → challenge
  → segundo GREEN. Testes negativos falham pela regra identificável; expected não
  deriva do Validator sob teste. Preserve falsificações como regressão.
- Backlog e roadmap descrevem futuro; não autorizam implementação. A autorização
  da sessão governa o trabalho; registre-a sem inventar aprovações intermediárias.
- Preserve mudanças alheias. Sem reset/stash/discard, force-push, merge, auto-merge
  ou publicação Maven sem autorização aplicável.

## Verificação e handoff

Entrypoint: `python3 -B scripts/harness/run.py <gate>`.
Use `fast` durante edição documental; `architecture` e `semantic` para núcleo;
`full` no handoff (inclui o `mvn clean verify`). `git --work <ID>` verifica branch,
base e escopo. Os comandos existentes `./scripts/check.sh` e `mvn verify` continuam
válidos. Reporte somente comandos realmente executados e seus limites.

Atualize `state.md` quando houver mudança material. Antes de commit/push, revise
staged diff, dependências, escopo, lifecycle e Git. No handoff informe commit/PR,
gates executados e não executados, limitações e próximo passo não iniciado.
Pare no PR para revisão humana quando esse for o limite autorizado.
