# Estado — WORK-AIR-JSON-002

## Onde estamos

4B implementado e ready_for_review local, branch feat/air-json-scalar-assign.
Baseline limpa/sincronizada b78f4068d8a479f48eb048b8d76fa60a0997dc4a.
1A arquivado após confirmação remota do merge #5. Sem mudança em air-model ou repos irmãos.

## Verde conhecido

Oracle independente validou no model intacto. RED do codec 1A registrado.
75 checks transporte (57 anteriores + 18 novos), 42 challenges compiláveis mortos,
restore exato e segundo GREEN 75. Golden manual 14.554 bytes, SHA-256
40b9cec1bcc1c1e40cf3b9e3c48e834835e478e1d84bccafa575d63497ef3b60; GOBACK intacto.
Scale N/2N 1.000/2.000 Objects/Cells/Assigns com crescimento linear observável;
10.000 Assigns referenciam somente um Object/Cell. Defaults 16 MiB/depth 128 preservados.
Full exit 0: docs/MANIFEST, 86 harness, architecture 308/17 classes e 4664/481 arestas,
semantic 172+75 e Maven clean verify 172+75. Git/scope e diff --check verdes.
Self-review integral sem finding pendente. [Evidência](../../../quality/air-json-scalar-assign.md).

## Restante

Etapa de publicação deste snapshot: commit/push normal, PR e CI no head exato;
o recibo remoto será anexado ao PR, sem commit apenas para autoinscrever CI.
Depois aguardar revisão humana; merge/auto-merge e CP4C/4D não autorizados aqui.

## Descobertas que afetam o plano

ACTIVATION requer owner (AIR 03 §2), com guarda local tipada. DisplayName aceita
blank/null; TextValue vazio/Unicode sem normalização. Nenhum blocker no model.
A allowlist do harness exige o golden novo, com contracaso de remoção, e lifecycle
1A teve link/índice reconciliados após merge confirmado. Full inicialmente parou
no DNS do Maven após gates offline verdes; reexecução com rede passou integralmente.
Formas fora do subset continuam IMPLEMENTATION_LIMIT; não certificam payload ignorado.
