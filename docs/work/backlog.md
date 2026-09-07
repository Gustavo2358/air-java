# Backlog local — sem autorização de execução

| ID | Resultado futuro | Dependência / fronteira |
| --- | --- | --- |
| BACKLOG-AIR-001 | 0C-D: discovery mergeado no [WORK-AIR-MODULARIZATION-001](history/WORK-AIR-MODULARIZATION-001.md) | [Opção B](../architecture/decisions/ADR-0002.md) aprovada; histórico preservado |
| BACKLOG-AIR-002 | 0C-I: mergeado PR #4 | topologia preservada na implementação 1A |
| BACKLOG-AIR-003 | 1A: implementação no [item atual](index.md) | API e erros de transporte fora do modelo; parar no PR humano |
| BACKLOG-AIR-004 | integração e conformance do transporte | codec real; lower/CFG e golden frontend em seus próprios escopos |
| BACKLOG-AIR-005 | hardening de escala e interoperabilidade | propriedade/oracle justificados; fora do primeiro harness |

Detalhes e distinção entre estado atual/futuro em [missão](../product/mission-and-roadmap.md).
0C-D e 0C-I foram mergeados. 1A está autorizado no item atual.
Integração, 2A/2B e E2E exigem trabalho posterior; nenhuma adoção de pins downstream
é realizada neste checkpoint.
