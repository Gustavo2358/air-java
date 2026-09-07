# Backlog local — sem autorização de execução

| ID | Resultado futuro | Dependência / fronteira |
| --- | --- | --- |
| BACKLOG-AIR-001 | 0C-D: discovery autorizado no [WORK-AIR-MODULARIZATION-001](active/WORK-AIR-MODULARIZATION-001/work-item.json) | [recomendação](../architecture/decisions/ADR-0002.md) em review; decisão humana antes de 0C-I |
| BACKLOG-AIR-002 | 0C-I: módulos fisicamente separados e gates por módulo | veredito 0C-D aprovado; não implementar codec completo aqui |
| BACKLOG-AIR-003 | 1A: codec Java do binding DRAFT pinado | 0B no analysis-ir + 0C; API e erros de transporte fora do modelo |
| BACKLOG-AIR-004 | integração e conformance do transporte | codec real; lower/CFG e golden frontend em seus próprios escopos |
| BACKLOG-AIR-005 | hardening de escala e interoperabilidade | propriedade/oracle justificados; fora do primeiro harness |

Detalhes e distinção entre estado atual/futuro em [missão](../product/mission-and-roadmap.md).
Somente 0C-D foi promovido pela autorização explícita desta sessão. Nenhum
checkpoint seguinte está autorizado; novo work item exige base, escopo, gates e stop condition.
