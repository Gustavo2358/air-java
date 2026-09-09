# Checkpoint 4B — AIR escalar compartilhada

Work item: [WORK-AIR-JSON-002](../work/history/WORK-AIR-JSON-002.md).
Baseline limpa e sincronizada: `b78f4068d8a479f48eb048b8d76fa60a0997dc4a`.
Branch: `feat/air-json-scalar-assign`. O PR #5 de 1A foi confirmado MERGED via GitHub
em 2026-09-07T21:30:12Z e seu lifecycle foi arquivado. Sem alteração dos repos irmãos.
O recibo remoto do head final pertence ao PR; não se grava um SHA futuro no próprio commit.

Autoridade mantida: analysis-ir `122ce54e1b9ef9b00646f93ece409ca8b63bc933`;
analysis-ir-json / bindingVersion 1.0.0 / airVersion 2.0.0 / DRAFT.
Consulta read-only por `git show` nesse SHA: binding §§3–4, 6–8, 10.3–10.4;
AIR 02 §1.3 (sameDomain), AIR 03 §§1–3 (Object/Cell/lifetime), AIR 04 §2 (Assign),
e invariantes I-01/02/03/04/08/11/28/32/36/49/52. Discovery/4A não definem o wire.
Nenhum defeito de representabilidade bloqueou o target: air-model production delta = zero.

## Resultado e cobertura

COMPATIBLE: mesmos artefatos/API/defaults e bytes GOBACK. Apenas reader/writer
privados recebem ObjectDeclaration, known(text), CellBinding, Cell/StorageHeader,
OperandHeader, ObjectPlace, TextValue, Literal, Assign e instructions ordenadas.
Lifetime/Visibility/OperandRole usam 16 tokens normativos com mappings explícitos.
Sem dependência nova, reflexão de records, DTO semântico ou alteração da camada física.
Closure/owner/role/sameDomain são verificações do model/Validator já existente.

Golden manual: [scalar-assign.canonical.json](../../air-json/src/test/resources/scalar-assign.canonical.json),
14.554 bytes, SHA-256 `40b9cec1bcc1c1e40cf3b9e3c48e834835e478e1d84bccafa575d63497ef3b60`.
Contém um Object, uma Cell, uma Entry, uma Sequence, um Assign, um Return,
duas ocorrências, text("PROGA"), oito origens aproximadas e cinco incertezas.
Inventários Publication/Unit PARTIAL; estado da Entry retém a lacuna de inventário.
Os fatos foram escritos manualmente; Python JSON padrão apenas ordenou/minificou
propriedades. Nenhum AirJson.encode produziu este arquivo. O oracle Java separado
é [ScalarAssignOracle](../../air-json/src/test/java/io/github/gustavo2358/air/json/ScalarAssignOracle.java).
Hash literal e inspeção de dependências no bytecode protegem a independência.

## Oráculos, RED e GREEN

Primeiro RED sem mudança de runtime: oracle Java validou sem issues; decode do golden
falhou IMPLEMENTATION_LIMIT em storage (limitação de inventário vazio do 1A).
O gate inicialmente também identificou resource sem ownership: a allowlist foi
ampliada somente para o novo golden obrigatório, com contracaso de remoção.
Logs brutos: `/tmp/air-4b/initial-red.log` e `/tmp/air-4b/expanded-green.log`.

As quatro igualdades passam: decode(golden)=expectedModel; encode(expectedModel)=golden;
decode(encode(expectedModel))=expectedModel; encode(decode(golden))=golden.
Os 57 checks 1A e suas regras físicas/adversariais/representabilidade permanecem.
Checks adicionais comparam integralmente issues do Validator (rule/subject/detail)
em encode/decode e paths locais. Casos model-only de int ou Region conferem a regra
I-08/I-52, I-49 ou I-13 sem implementar essas formas no transporte.
Nop válido no model prova IMPLEMENTATION_LIMIT para forma válida não suportada.

Testes abrangem dangling Object/Cell, storage owner ausente, IDs de domínio errado,
owners de OperandId em outra operação/entrada, roles trocados, slots I-04,
campos obrigatórios/desconhecidos recursivamente, nullability, tokens e formas fora
do subset, texto vazio/espaçado/Unicode, tabelas dos enums e dois Assigns fora da
ordem lexical de seus IDs. Defaults 16 MiB / depth 128 intactos e limites de
validação continuam INCOMPLETE_VALIDATION.

## Escala observada

| Objects | Cells | Assigns | Operações | Operandos | Input bytes | Output bytes | Nós físicos | Entidades | Domain queries | Tempo observado (ms) |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1.000 | 1.000 | 1.000 | 1.001 | 2.000 | 4.371.328 | 4.371.328 | 238.546 | 5.019 | 1.000 | 462 |
| 2.000 | 2.000 | 2.000 | 2.001 | 4.000 | 8.741.328 | 8.741.328 | 476.546 | 10.019 | 2.000 | 808 |
| 1 | 1 | 10.000 | 10.001 | 20.000 | 24.747.652 | 24.747.652 | 1.320.652 | 30.021 | 10.000 | 2.138 |

Tempos da primeira execução expandida com JDK 25, release 21; apenas observações.
N/2N aproxima 2 em bytes/nós/queries; definitions continuam exatas. O último caso
prova 10.000 referências ao mesmo Object sem cópias da definição. Usa Limits de
64 MiB explícitos; o default rejeita esse documento, e a profundidade continua 128.
Não há instrumentação nova ou SLA de milissegundos. Inspeção do diff confirma
percurso direto dos arrays sem scans por referência. Índices do Validator não mudaram.
Custo aproximado O(bytes + entidades + referências) para este subset; não é prova
assintótica universal, benchmark de corpus COBOL ou contador de todo lookup interno.

## Challenges e gates

`python3 scripts/challenge_json.py --output /tmp/air-4b/challenges.json`: exit 0.
42 mutações compiláveis mortas (27 anteriores + 15 novas), restore byte a byte e
segundo GREEN de 75 checks. [Recibo integral](air-json-scalar-challenges.json).
Logs brutos por mutante, GREEN inicial e restaurado em `/tmp/air-4b/challenges.logs/`.
Falha de compilação aborta o runner e nunca conta como RED semântico.

Os challenges novos removem Object/Cell, ignoram instructions, trocam role, reparam
owner indevidamente, usam enum.name, classificam unsupported como INVALID_IR,
removem destination/value, invertem/ordenam instructions, normalizam/alteram texto,
derivam o oracle do decoder e alteram o golden. Os anteriores incluem unknown fields,
UTF-8/JSON adversarial, enums e preservação da taxonomia/representabilidade 1A.
O checksum do golden e as quatro igualdades permanecem independentes.

Gates locais concluídos, exit 0:

- `python3 -B scripts/harness/run.py full`: docs/MANIFEST (147 arquivos),
  harness (86 testes), architecture (308 model + 17 JSON classes; 4.664 + 481 arestas),
  semantic (172 model + 75 transporte) e Maven root clean verify (172 + 75).
- `python3 -B scripts/harness/run.py transport`: GREEN focalizado e segundo GREEN
  de 75 pelo runner de challenges; mesmas assertions executadas em semantic/Maven.
- `python3 -B scripts/harness/run.py git --work WORK-AIR-JSON-002` e `scope --work WORK-AIR-JSON-002`.
- `git diff --check`; self-review integral de código, testes, golden, harness e docs.

O primeiro full parou em links do lifecycle, corrigidos; a execução seguinte passou
os quatro gates offline e parou no DNS restrito do Maven. O full completo foi
reexecutado com rede para resolver apenas plugins fixados no cache `/tmp`.
Logs de todas as tentativas foram preservados, incluindo `/tmp/air-4b/full-network.log`.
O gate `performance` focalizado e integration permanecem UNAVAILABLE e não foram
alegados como PASS; os probes de escala são parte obrigatória do transporte.

Comparação byte a byte confirmou todos os 43 arquivos de air-model, source lock,
baseline do binding, AirJson.java, Json.java e golden GOBACK intactos. SHA-256 GOBACK:
`fa299c2e5f3fae75afe365363b9f16925f0cfea591f631768ace82f0fb9a1075`.
Snapshot read-only de HEAD/status/hashes dos cinco repos irmãos também permaneceu
idêntico (24/468/395/191/443 arquivos, respectivamente analysis-ir/proleap-poc/
cobol-lower/analysis-cfg/artefatos-e2e). POMs/grafos não ganharam dependências.
A confirmação remota de push/CI exact-head será publicada no PR, separada destes
resultados locais e da revisão humana ainda pendente.

## Input contract for Checkpoint 4C

O caller Java fornece identidades completas e independentes e todos os metadados
com seus escopos/evidências fechados. Os valores abaixo são trechos de construção;
`dataOrigin`, `targetOrigin`, `literalOrigin`, `precision`, `assignHeader`,
`returnHeader`, `sequenceOrigin` e os IDs são fatos já escolhidos pelo produtor:

```java
var text = Types.known(Types.Builtin.TEXT);
var s = new Memory.Cell(new Memory.StorageHeader(storageId, Optional.of(unitId),
        Memory.Lifetime.PERSISTENT, Memory.Visibility.PRIVATE, dataOrigin), text);
var d = new Memory.ObjectDeclaration(objectId, Optional.of("WS-PGM"), text,
        new Memory.CellBinding(s.header().id()), Memory.Visibility.PRIVATE,
        dataOrigin, Evidence.CoverageStatus.MODELED, precision);
var owner = new Ids.OperationOwner(assignHeader.id());
var destination = new Places.ObjectPlace(new Operand.Header(
        new Ids.OperandId(owner, "destination"), Operand.Role.VALUE_WRITE, targetOrigin), d.id());
var value = new Expressions.Literal(new Operand.Header(
        new Ids.OperandId(owner, "source"), Operand.Role.VALUE_READ, literalOrigin),
        new Values.TextValue("PROGA"));
var assign = new Operations.Assign(assignHeader, destination, value);
var ret = new Operations.Return(returnHeader, List.of());
var sequence = new Sequence(labelId, List.of(assign), ret, sequenceOrigin);
// unit.objects contém d; unit.sequences contém sequence; publication.storage contém s.
// Entry aponta para labelId; origins, coverage, precision e uncertainties fecham.
byte[] canonical = new AirJson().encode(publication);
Publication restored = new AirJson().decode(canonical);
```

A [construção completa compilada](../../air-json/src/test/java/io/github/gustavo2358/air/json/ScalarAssignOracle.java)
e os quatro round-trips exercitam o contrato pela API pública. O caller deve usar
`io.github.gustavo2358.air.model.Unit` explicitamente se houver import concorrente.
IDs locais do exemplo não são convenção obrigatória nem algoritmo do lower.
Não há `logicalExtent=5` no domínio AIR text nem interpretação de MOVE no codec;
4C deve estabelecer FULL_IDENTITY e o domínio antes de construir Assign. Declarar
uma Cell não prova disjunção de outras células. O codec não gera IDs ou corrige claims.

## Pin handoff for Checkpoint 4D

O SHA exato de `air-java` validado/pushed será anexado ao PR e ao handoff final
após os commits; `analysis-cfg` só deverá mudar seu pin no trabalho 4D, após merge.
Consumir os módulos conjuntos `air-java` (model) e `air-json`, versão Maven
`0.1.0-SNAPSHOT`, construídos desse SHA. Este trabalho não instala/publica Maven,
não altera pin no CFG e não faz merge/auto-merge. Se o merge modificar o código,
revalidar o commit resultante antes de selecioná-lo como pin downstream.

## Limitações conhecidas

Demais tipos/literais/Places/expressões/Storage/Binding, Read/unknown, premissas,
havoc/nop/controle/invoke, recursos/relações, regions/views/bytes, condições iniciais,
completion ports e capabilities com conteúdo permanecem fora da cobertura.
Nop aparece apenas como negativo válido não suportado. O binding continua DRAFT;
sem qualificação integral/cross-language, novo capability profile ou segunda implementação.
Limites e drifts de representabilidade 1A permanecem explícitos; não foram corrigidos
no model. Não houve SP reader, lowering, E2E, CFG, RD, Possible Values ou CALL/IF.
Review deste diff pelo autor é self-review; aprovação humana permanece pendente no PR.
