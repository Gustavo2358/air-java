# Controle de mudanças e impacto downstream

A norma vem de [sources.lock.json](../sources.lock.json). Sincronizar `origin`
não atualiza esse lock automaticamente. Mudança normativa exige comparar snapshots,
reconciliar API/Validator/evals e registrar versão e limites no mesmo work item.
Roadmap é contexto de planejamento e não supera a semântica pinada.

Classifique impacto antes de mudar API pública, coordenadas Maven, construtores,
variantes sealed, diagnóstico, imutabilidade ou suporte de capacidades:

| Classe | Uso |
| --- | --- |
| NONE | evidência demonstra ausência de impacto de produto (ex.: este harness) |
| COMPATIBLE | comportamento/API preservados, com evidência focalizada |
| BREAKING | callers, formato ou premissas precisam de migração |
| UNASSESSED | evidência insuficiente; registrar o que falta e quando reavaliar |

Registrar classe, justificativa, evidência e consumidores afetados. Não inferir
compatibilidade pelo sufixo SNAPSHOT. Lower e CFG consomem hoje o mesmo artifactId;
na modularização comparar opções e churn antes de decidir nomes/módulos.

Mudança de gate/expected/allowlist é mudança de contrato de engenharia: explicar
motivo e apresentar positivo/negativo. Fonte lock, assertions ou inventário não são
atualizados automaticamente para apagar falhas. Não editar outros repositórios
sem escopo autorizado; handoff documenta a migração necessária.
