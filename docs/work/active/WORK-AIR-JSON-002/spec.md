# Especificação 4B

Autoridade: analysis-ir `122ce54e1b9ef9b00646f93ece409ca8b63bc933`, binding
analysis-ir-json 1.0.0 / AIR 2.0.0 DRAFT §§3–4, 6–8, 10.3–10.4.
AIR 03 §§1–3: Object e Cell distintos, mesmo TypeRef; AIR 02 §1.3 e AIR 04 §2:
Assign exige sameDomain. I-01/02/03/04/08/11/28/32/36/49 preservados.

Entradas: Publication com ObjectDeclaration, Cell/StorageHeader/CellBinding,
known(text), Operand.Header, ObjectPlace, Literal/TextValue, Assign e Return vazio.
DisplayName e storage owner têm null presente; demais campos obrigatórios.
Todos os tokens dos três enums transitivos são mapeados explicitamente.
Saída: fatos/IDs/origens/coverage/precision/uncertainties e arrays preservados.
Nenhuma premissa ou conversão de domínio é criada. Validator decide closure/roles/domínio.

Positivos: golden independente, texto vazio/espaçado/Unicode, nullable, N/2N.
Negativos: dangling/domínio/owner/role, slot instruction/terminator, shape/tokens.
Formas reconhecidas além do subset: IMPLEMENTATION_LIMIT, sem certificar payload.
Impacto COMPATIBLE: cobertura ampliada, bytes GOBACK/API/defaults intactos.
Sem COBOL/SP/lowering, CFG, novas versões, dependências, model ou outros tipos.
