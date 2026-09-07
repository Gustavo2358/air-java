# Backlog local — sem autorização de execução

| ID | Resultado futuro | Dependência / fronteira |
| --- | --- | --- |
| BACKLOG-AIR-001 | 0C-D: discovery mergeado no [WORK-AIR-MODULARIZATION-001](history/WORK-AIR-MODULARIZATION-001.md) | [Opção B](../architecture/decisions/ADR-0002.md) aprovada; histórico preservado |
| BACKLOG-AIR-002 | 0C-I: mergeado PR #4 | topologia preservada na implementação 1A |
| BACKLOG-AIR-003 | 1A: implementação no [item atual](index.md) | API e erros de transporte fora do modelo; parar no PR humano |
| BACKLOG-AIR-004 | integração e conformance do transporte | codec real; lower/CFG e golden frontend em seus próprios escopos |
| BACKLOG-AIR-005 | hardening de escala e interoperabilidade | propriedade/oracle justificados; fora do primeiro harness |
| BACKLOG-AIR-006 | AIR-MODEL-DRIFT: reconciliar restrições Java sem suporte normativo identificado | dívida registrada na remediação de PR #5; execução futura separada |

Detalhes e distinção entre estado atual/futuro em [missão](../product/mission-and-roadmap.md).
0C-D e 0C-I foram mergeados. 1A está autorizado no item atual.
Integração, 2A/2B e E2E exigem trabalho posterior; nenhuma adoção de pins downstream
é realizada neste checkpoint.

## BACKLOG-AIR-006 — AIR-MODEL-DRIFT

Reconciliar restrições Java não sustentadas pelo AIR 2.0.0 / binding no pin
`122ce54e1b9ef9b00646f93ece409ca8b63bc933`:

- `Origins.Span`: limita lineBase/columnBase a 0/1; binding §10.3 admite Natural.
- `Require.text`: impõe String.isBlank genericamente; binding §§3/4/10.3 transporta
  Text de escalares Unicode, sem esse predicado lexical para os campos auditados.
- `Scopes.EntityScope`: exige lista não vazia; binding §10.3 declara Id[] sem mínimo.

Cada caso deverá decidir entre remover a restrição Java, encontrar norma AIR
existente que a sustente ou propor alteração normativa futura em trabalho separado.
A decisão humana para PR #5 autoriza somente reconhecer os três limites de
representabilidade como IMPLEMENTATION_LIMIT no codec, com site explícito.
Não modifica air-model nem resolve esta dívida. Não bloqueia o GOBACK E2E atual;
integração e execução deste backlog continuam fora da autorização do checkpoint 1A.
