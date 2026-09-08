# Avaliação

Golden manual escrito como fatos wire sem AirJson.encode; Java construído sem decode.
Exigir quatro igualdades model/bytes, determinismo, hash fixo, GOBACK byte-identical.
Partições dirigidas: Object/Storage/Operand campos obrigatórios, null/unknown fields,
IDs/domínios/owners/roles incorretos, I-04 e sameDomain via Validator existente.
Enums fechados, texto sem normalização, unsupported reconhecido e token desconhecido.
Scale probe 1.000/2.000 Objects/Cells/Assigns, operações N+1, operandos 2N;
contar AST/definitions/references e statistics existentes, bytes/tempo observacionais.
Caso muitos usos de um Object prova ausência de duplicação de definitions.
Sem threshold de milissegundos ou instrumentação de runtime.
Challenges: omissões, roles/owners, enum.name, unknown field, classificação,
destination/value, ordem, bytes e independência do golden. Compilação deve passar.
Gates architecture, semantic, transport, full, git/scope e diff --check.
