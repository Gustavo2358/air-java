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

## INTERNAL-CONTRACT-DEV-001

Contratos internos controlados podem evoluir producer-first; consumers migram em waves posteriores. Incompatibilidade temporária de pins downstream não é blocker por si só. Não criar compatibility layer, dual codec, legacy writer, version negotiation, downgrade adapter ou APIs paralelas sem necessidade concreta. Registrar impacto e handoff, preservando evidência e os limites da wave autorizada.


## CP6 W2C — cobertura aditiva

COMPATIBLE para Publications W1 já aceitas: mesmos bytes físicos, API, versões,
POMs e política de admissão. Amplia somente mappings de formas já normativas;
callers que dependiam de IMPLEMENTATION_LIMIT para essas formas passam a obter
transporte ou diagnóstico estrutural específico. Fixtures malformadas com token
Unknown/premises antes fora da cobertura passam a ser classificadas por shape.
[Recibo e contracasos](../quality/cp6-w2c-transport.md) registram essa evolução.
Consumers W1 não precisam migrar bytes antigos; o futuro W2B precisará adotar o
commit qualificado e construir os fatos AIR a partir de sua própria autoridade.
Nenhuma aprovação/implementação de W2B ou W2D é inferida desta wave.
