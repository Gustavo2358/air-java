# 1A — codec compartilhado

Autoridade: analysis-ir 122ce54e1b9ef9b00646f93ece409ca8b63bc933, bindings/json-v1.md
§§2–7, 9–12 e normativos referidos no mesmo SHA. Handoff 0B lido no merge
51b4d9a8ae0364232bd97103cd73a77e1a34996c. Binding analysis-ir-json / 1.0.0,
AIR 2.0.0, DRAFT. Nenhum schema paralelo ou introspecção de records.

Resultado: API Publication → byte[] e byte[] → Publication, canonical UTF-8,
fechamento pelo AirValidator antes de entregar resultado. Erros físicos, versões,
AIR inválida, capacidades, validação incompleta e limites distinguíveis.

Entradas: Publication GOBACK manual completa (2 artifacts, 8 origins, 5 gaps,
1 Unit/Entry/Sequence/Return), proveniência ausente/aproximada/includes e múltiplos
inventários na ordem publicada. PARTIAL global/unit e EXACT control coexistem com
UNAVAILABLE em quatro dimensões. Unknown explícito não vira ausência/default.

Não há requisito de reconhecer nomes/códigos do lower. Nenhum consumer, model,
filesystem, rede, CLI, segunda implementação ou promoção normativa em escopo.
Impacto COMPATIBLE: API nova apenas em air-json; model/GAV atuais preservados.
