# Estado — WORK-AIR-CAPACITY-001

## Onde estamos

Implementação, oracles e full completos; ready_for_review local na branch
fix/air-capacity-contract, base ce530a7e17ab12b23c48f29425f503ff920b09fb.
4B arquivado após merge remoto confirmado. Modelo AIR, goldens, pins, POMs e irmãos intactos.

## Verde conhecido

179 checks model + 79 transporte. Fast (86 testes harness), architecture,
semantic, transport e full (incluindo Maven clean verify) passaram. 12 challenges de capacidade compiláveis: RED esperado,
restore byte-exact e segundo GREEN individual; snapshot confere com fontes atuais.
42 challenges anteriores do JSON reexecutados, restore exato e GREEN de 79.
Probes N/2N/4N incluem 2.000.005 entidades e 20.972.108 bytes STRUCTURALLY_VALID;
24.747.652 bytes também passam no default. Cliente binário de ce530a7 passou.
[Evidência](../../../quality/air-capacity.md).

## Restante

Publicar este snapshot por commit/push/PR e confirmar CI no head exato; recibo
remoto será anexado ao PR, sem commit apenas para autoinscrever CI. Aguardar
review humano. Sem merge/auto-merge, publicação Maven ou próximo item.

## Descobertas que afetam o plano

Issues retidos não são todos os resultados: API acrescenta totais/completion.
Defaults positivos preservam uso de readNBytes no CFG. Recursão de tamanho removida;
lookup de outcomes/capabilities/writes indexado e Return vazio compatível reutilizado.
Versão não interpretada não afirma traversal completo. Binding permanece 1A+4B DRAFT.
Full inicial falhou por DNS Maven; reexecução com rede para plugins fixados passou.
Self-review integral sem finding pendente; git/scope e diff --check passaram.
