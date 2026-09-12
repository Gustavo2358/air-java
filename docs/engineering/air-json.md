# Codec compartilhado AIR JSON — cobertura 1A + 4B + CP6 W1B/W2C

O módulo `air-json` implementa o subset transitivo do GOBACK descrito por 0B e
o transporte escalar Object/Cell/Assign text de 4B, Invoke W1B e Branch/Jump/Unknown
BOOL/Premise DisjointStorage W2C contra
**analysis-ir-json / bindingVersion 1.0.0 / airVersion 2.0.0, DRAFT**, no SHA
`51b4d9a8ae0364232bd97103cd73a77e1a34996c`. A autoridade é AIR → binding → codec.
[Baseline e arquivos consultados](../sources/air-json-baseline.json).
O [handoff 0B mergeado](https://github.com/Gustavo2358/analysis-ir/blob/51b4d9a8ae0364232bd97103cd73a77e1a34996c/bindings/checkpoint-0b-mvp.md)
é orientação operacional; normas e binding foram extraídos somente do pin.

## API e limites de responsabilidade

```java
import io.github.gustavo2358.air.json.AirJson;

var codec = new AirJson();
byte[] canonical = codec.encode(publication);
Publication restored = codec.decode(canonical);
```

`AirJson`, `AirJson.Limits`, `AirJsonException` e seu enum `Code` compõem a API
pública do módulo. Não há Map público, annotations no model, serialização de records, DTOs
acoplados ao model, adaptador Path/arquivo, streams de filesystem, CLI ou rede.
O codec é imutável e não mantém estado de uma chamada para outra. O model e seu
Validator pertencem a `air-model`, artefato Maven `air-java`.

Ambos os métodos retornam somente após sucesso integral. `decode` valida bytes,
campos/formas e versões, materializa os fatos exatos e chama `AirValidator`.
`encode` verifica a versão, mapeia explicitamente os fatos, valida AIR e emite
bytes canônicos. Obrigações semânticas isoladas não bloqueiam transporte e não são satisfeitas por esse sucesso. Consumers que precisem de avaliação executam `AirValidator.validate(restored)`; I-56 é reconstruído dos fatos materializados. Nenhum percurso completa lacunas ou executa análise de CFG.
Uma Publication com formas ainda não implementadas é rejeitada antes de alegar
validação completa desse conteúdo. Não há materialização parcial disponível ao caller.

## Decisão de biblioteca JSON

**Nenhuma dependência JSON externa em 1A.** O binding tem somente objetos, arrays,
strings, booleanos e null; todos os números wire são strings. A camada privada
[Json.java](../../air-json/src/main/java/io/github/gustavo2358/air/json/Json.java)
implementa essa gramática física e a escrita canônica, separada dos mapeamentos
[BindingReader](../../air-json/src/main/java/io/github/gustavo2358/air/json/BindingReader.java) /
[BindingWriter](../../air-json/src/main/java/io/github/gustavo2358/air/json/BindingWriter.java).
Isso é uma implementação do binding pinado, não um contrato/schema alternativo.

Jackson streaming ou outro parser ainda exigiria configurar/rever detecção de
duplicatas, Unicode e limites, rejeitar defaults/coercions e controlar a escrita
canônica. Não há necessidade de databinding. Para esta cobertura, a decisão é
manter uma camada física pequena, com cada regra adversarial testada, sem resolução
adicional de bibliotecas ou dependência transitiva. O custo assumido é manter esse
parser explícito e seus contracasos. Uma adoção futura de biblioteca exige decisão,
versão fixa e política de dependências exclusiva do JSON, sem allowlist `com.*`/`org.*`.

O grafo autorizado continua `air-json → air-java`, compile direto na versão Maven
conjunta. Grafos externos do model e do JSON são vazios. A política offline de
`check.sh` permanece verdadeira: JDK + Python, sem Maven ou downloads. Maven pode
resolver seus plugins previamente fixados; o codec não adicionou bibliotecas.

## Regras físicas e preservação

- UTF-8 estrito com REPORT, sem BOM; raiz objeto único e somente whitespace JSON
  depois do documento. Número JSON, segundo documento, lixo e escapes ilegais falham.
- Chaves são desescapadas e verificadas antes de inserção. Duplicatas são recusadas
  em qualquer profundidade, inclusive `binding`/`\u0062inding`.
- Surrogates isolados são rejeitados; pares válidos e escalares suplementares são
  preservados. Sem normalização de caixa/NFC/NFD. U+FEFF dentro de string é dado.
- Campos catalogados são obrigatórios; somente os campos `?` aceitam null presente.
  Campo desconhecido não é descartado. Variantes têm `kind` explícito e tokens fechados.
- Dimension, PrecisionStatus, CoverageStatus, InventoryStatus e ColumnUnit usam
  mappings explícitos da §10.4 em reader e writer. Tokens wire nunca derivam de
  `Enum.name()`, `toString()` ou `String.valueOf(enum)`. O oracle literal cobre os
  20 tokens, inclusive os não exercitados pelo golden; um check do bytecode do
  writer rejeita a volta à autoridade de runtime mesmo quando os bytes coincidem.
  4B acrescenta tabelas fechadas para os 16 tokens de Lifetime, Visibility e OperandRole;
  tokens de role válidos fisicamente continuam sujeitos ao papel exigido pelo Validator.
- Naturais implementados (Position/Span) usam BigInteger e strings canônicas, sem
  teto int/long/2^53. `0` é aceito, negativos/`-0`/`+1`/zeros iniciais/fração/expoente não.
  Inteiros assinados em valores, decimal e Base64 ainda não têm cobertura de codec;
  não há helper de decimal/Base64 nem alegação de conformidade dessas primitivas.
- Propriedades são ordenadas por escalares Unicode; arrays conservam a ordem recebida.
  A saída escapa somente aspas, barra invertida e controles U+0000..001F, com escapes
  curtos onde definidos e `\u00xx` minúsculo nos demais; `/` e outros escalares ficam
  literais. Sem espaços de formatação, BOM, newline final, timestamps ou IDs gerados.
- Identidades têm domínio e namespace completos; IDs locais e nomes nunca são joins.
  Definição duplicada, owner errado e referência pendente são rejeitados por AIR.
- Coverage PARTIAL da Publication/Unit, EXACT local e UNAVAILABLE nas demais dimensões
  coexistem sem fortalecimento. Origens, razões, scopes e ordens permanecem fatos.
  Códigos qualificados e mensagens não selecionam mapeamento ou regra do codec.

## Matriz de cobertura

O binding cobre todo o catálogo de suas §§1–12. A tabela descreve somente o que
esta implementação aceita; GOBACK é testemunho, não perfil nem restrição normativa.

| Formas do binding | Cobertura implementada | Golden / casos dirigidos |
| --- | --- | --- |
| Envelope, Publication, SemanticVersion | versões exatas e todos os contêineres | publicação completa; ambos os round-trips |
| Manifest | required/provided vazios | ambos presentes; conteúdo dá UNSUPPORTED_CAPABILITY |
| Artifact | id/logicalName/contentDigest nullable | dois artifacts; nomes Unicode/digest vazio em variação |
| Unit, BodyKnowledge | available; containingUnit nullable | Unit com Entry/Sequence; inventários múltiplos em variação |
| Entry, Signature | initialLabel nullable; parameters/results known vazios, remainder none/unknown | assinatura fechada, origem própria; ausência de label em available é INVALID_IR |
| EntryState | conditions vazio; uncertainties transportadas | vazio no golden |
| Sequence, OperationHeader, Return | instructions ordenadas de Assign; Return com values vazio | GOBACK vazio byte-identical; Assign seguido de Return |
| IDs | todas as formas da §4, inclusive OperandId com owner entry/operation | oito domínios exercitados no golden; demais IDs isolados, sem suporte a suas definições |
| Origin | written, derived | oito origens; escrita null/aproximada e IncludeFrames em variação |
| Location, Span, Position, IncludeFrame | line_columns; medidas BigInteger; site nullable | quatro spans exatos; bases/unidades/exclusividade/includes e números grandes em variações |
| Coverage, CoverageItem | inventário/scope/items/reasons; elimination null | PARTIAL global/unit; dois itens e outputs heterogêneos |
| Precision, Claim, Uncertainty, Dimension | todos os campos/tokens catalogados | cinco claims e cinco lacunas; dados opacos modificados em teste |
| FactScope | publication, unit, entities | três formas preservadas; IDs não ampliam escopos |
| ObjectDeclaration | oito campos do binding, displayName nullable, TypeRef e binding preservados | WS-PGM, nomes vazios/espaçados/Unicode/null sem joins textuais |
| TypeRef, Type | known(text), known(bool) | Object/Cell; demais formas reconhecidas dão IMPLEMENTATION_LIMIT |
| Storage, StorageHeader, StorageBinding | Cell, header completo e cell(StorageId) | owner nullable; ACTIVATION requer owner por AIR 03 §2 |
| OperandHeader, Place, Expression, LiteralValue | occurrence id/role/origin; ObjectPlace; Literal(TextValue), Read(ObjectPlace), Unknown | duas ocorrências pertencentes ao Assign; sem TypeRef duplicado no Place |
| Assign | header, destination, value | ObjectPlace ← Literal(TextValue) ou Read(ObjectPlace); sameDomain pelo Validator |
| resources/artifactRelations; visibleObjects/completionPorts | contêineres vazios obrigatórios | omissão/null recusados; conteúdo falha explicitamente |

**Fora da cobertura:** BodyKnowledge.unavailable, origens contractual/unavailable,
Location.offsets, Elimination com conteúdo, Capability com conteúdo, Parameter /
ResultSlot, TypeRef.unknown_type, tipos além de text/bool,
literais além de TextValue, expressões além de Literal/Read/Unknown, Places além de ObjectPlace,
storage/bindings além de Cell/CellBinding, condições iniciais, recursos/relações,
SameDomain, demais operações core e extensões/envelopes conservadores. Invoke tem somente o subset descrito abaixo.
O envelope JSON top-level está implementado; `Envelopes.Envelope` de efeitos/controle
é outra forma do binding e permanece fora da cobertura. Return não ganha tal envelope.

Uma forma reconhecida sem mapeamento ou contêiner não vazio fora da cobertura
produz falha explícita. Seu payload não é certificado como válido: a inspeção para
na fronteira não implementada, após o parser físico, sem ignorar conteúdo e continuar.
A lista de operações reconhecidas vem da §7 inteira; espécie não catalogada é erro
físico, espécie válida ainda sem implementação não vira INVALID_IR por esse motivo.

## Diagnósticos e recursos operacionais

| AirJsonException.Code | Fronteira |
| --- | --- |
| INPUT_ERROR | bytes/léxico, shape, campo obrigatório/desconhecido, null/tipo/token/inteiro físico |
| VERSION_MISMATCH | binding ou qualquer versão diferente do envelope exato |
| INVALID_IR | constraint local AIR ou erro de fechamento/ownership/validação |
| UNSUPPORTED_CAPABILITY | manifesto com conteúdo ainda não negociado/suportado ou issue do Validator |
| INCOMPLETE_VALIDATION | VALIDATION_LIMIT ou traversal/validação incompleta por causa distinta de SEMANTIC_OBLIGATION; sempre bloqueante |
| IMPLEMENTATION_LIMIT | forma fora da cobertura ou drift de representabilidade Java identificado |
| RESOURCE_LIMIT | budget operacional de bytes/depth/Validator ou teto de representação do buffer/contadores |

`path()` aponta para o campo, objeto local ou posição física (`$@N`, índice UTF-16
na string decodificada). `issues()` conserva rule/subject/detail originais quando
emitidos pelo AirValidator. Construtores AIR são chamados somente após verificar
os tipos físicos; suas violações locais não são confundidas com JSON malformado.
Falhas de programação/API (por exemplo argumento Java null) não são disfarçadas
como validade AIR. Erros fatais da JVM não recebem promessa de recuperação.

Na remediação do review humano, as violações locais com autoridade identificada
produzem `ValidationIssue` com regra explícita e detail, e `path()` preserva o site
JSON. A escolha da regra ocorre junto à materialização, sem interpretar mensagens
de exceptions Java. Os issues do AirValidator continuam intactos, comparados
integralmente nos testes de encode/decode, inclusive rule/subject/detail.

| Constraint local | Regra/fonte no pin 122ce54… |
| --- | --- |
| Derived sem inputs | I-36 e AIR 06 §5: uma ou mais origens |
| Unit available sem entradas | AIR 01 §2: pelo menos uma entrada |
| Unit available sem sequências | AIR 01 §3: pelo menos uma sequência |
| Uncertainty sem domínio afetado | AIR 06 §4: domínio afetado identificável |
| ID de domínio incompatível | I-02 e AIR 01 §4 |
| Terminador em instructions/operação comum como terminador | I-04 e AIR 01 §3 |
| StorageHeader ACTIVATION sem owner | AIR 03 §2: instância por ativação da unidade proprietária |

A decisão humana da remediação classifica os três gaps conhecidos abaixo como
`IMPLEMENTATION_LIMIT`, com `path()` do campo e diagnóstico
`air-java representability limit`. A entrada é admitida pelo contrato pinado, mas
não pode ser materializada fielmente pelo model Java atual. Isso não é defeito AIR
da Publication. A inspeção interrompe nesse limite; não certifica os demais fatos.

| Restrição adicional do Java | Autoridade e tratamento explícito |
| --- | --- |
| Span.lineBase/columnBase >1 | Binding §§3/10.3 admite Natural; checar >1 depois da leitura física, sem mudar Origins.Span |
| FactScope.entities vazio | Binding §10.3 admite Id[]; checar lista vazia sem transplantar I-52 de DomainProofScope |
| Text blank nos campos auditados | Binding §§3/4/10.3 não define nonBlank; checar String.isBlank somente nos sites limitados por Require.text |

Os sites Text auditados são os componentes opacos de IDs, Artifact.logicalName,
CoverageItem.sourceKey, Uncertainty.code/reason, Derived.rule e IncludeFrame.requestedName. W2C acrescenta Premise.authority e
Premise.justification à mesma política auditada: o binding usa Text sem nonBlank,
enquanto o model usa Require.text; blank é IMPLEMENTATION_LIMIT, não regra AIR inventada.
AIR 06 §§4/5 exige código, motivo e regra como fatos, mas não define a gramática
String.isBlank; o codec não decide semântica pelo conteúdo da mensagem. As constraints
locais identificadas (como inputs e dimensões não vazios) são verificadas antes do
limite de Text de Derived/Uncertainty. Não há filtro global de strings: contentDigest
blank é representável e preservado; tokens/versões mantêm suas regras específicas.
Naturais negativos/lexemas inválidos e números JSON continuam INPUT_ERROR.

Todo INVALID_IR emitido pelo codec tem regra AIR em `issues()` e site em `path()`;
os issues do AirValidator permanecem intactos. Não há catch que converta genericamente
IllegalArgumentException em INVALID_IR ou IMPLEMENTATION_LIMIT. Falhas inesperadas
de construtores propagam com identidade original para investigação; não recebem
uma classificação por mensagem ou uma regra presumida. Um teste injeta esse tipo
de falha para proteger a fronteira. Novos drifts exigem consulta normativa e decisão
quando ambíguos. A dívida está em [AIR-MODEL-DRIFT](../work/backlog.md#backlog-air-006--air-model-drift);
sua correção no model e novas formas de transporte permanecem fora deste PR.

A decisão humana posterior ao review de ab25ea0 resolveu também as precondições
adicionais de Span: coordenadas abaixo da base e start > end na ordem linha/coluna.
Guardas explícitas após ler todos os campos físicos e antes do construtor produzem
IMPLEMENTATION_LIMIT com path do Span, sem issue/rule AIR inventada. O diagnóstico
explica que os campos físicos foram aceitos, nenhuma regra de invalidade AIR foi
identificada no pin e air-java não consegue materializar a forma. O mesmo caminho
trata Written.location e IncludeFrame.site. O primeiro limite encontrado é reportado;
bases >1 conservam sua fronteira anterior e o path específico da base.

Os controles preservam spans representáveis (bases 0/1, igualdade de posições,
endExclusive verdadeiro/falso e coluna menor quando a linha final é posterior).
Spans coerentes com bases arbitrárias chegam ao limite da base Java, sem INVALID_IR.
Naturals inválidos continuam INPUT_ERROR antes das condições de representabilidade.
Não se generaliza essa decisão para construtores/predicados não investigados.

Há duas categorias de dívida: [drift Java](../work/backlog.md#backlog-air-006--air-model-drift)
(bases 0/1, nonBlank genérico, EntityScope não vazio) e
[clarificação normativa futura](../work/backlog.md#backlog-air-007--air-normative-clarification)
(coordinate >= base e start <= end). Nenhuma regra foi promovida no pin atual;
analysis-ir e air-model permanecem intactos. [Histórico do blocker e decisão](../quality/air-json-implementation.md#review-independente-do-head-ab25ea0--novo-blocker).

Defaults de bytes/depth e de entidades/nesting do Validator usam Integer.MAX_VALUE,
limite de representação dos buffers/índices/contadores da API Java em memória,
sem teto semântico de cobertura. Os construtores existentes com limites positivos
continuam disponíveis como budgets operacionais opt-in (sem antigos máximos
128/256/512). Uma exhaustion retorna RESOURCE_LIMIT e não expõe produto parcial.
Malformed JSON continua INPUT_ERROR; forma fora do subset continua IMPLEMENTATION_LIMIT.
OOM/erros fatais JVM não são capturados nem convertidos em invalidade AIR.

`maximumIssues` controla retenção, sem parar traversal. `ValidationResult` preserva
os dois accessors e construtor anteriores e acrescenta `diagnostics()` e
`hasIssues(kind)`: classificação usa contagens totais, inclusive mensagens omitidas.
O codec usa esses totais, nunca deduz validade de um prefixo vazio. Falhas após
Validator expõem `AirJsonException.validationResult()` (Optional); `issues()`
permanece a lista retida. RESOURCE_LIMIT prevalece no codec quando houve exhaustion operacional,
mesmo havendo erro detectado; o resultado associado preserva ambos.

O parser físico usa frames explícitos e mantém UTF-8 estrito e a mesma gramática.
Depth continua contando arestas Value→Value a partir da raiz 0, inclusive folhas,
não níveis semânticos AIR. O writer usa duas passagens iterativas: conta bytes
UTF-8 canônicos exatos e valida depth/scalars antes de alocar um único byte[] de
saída; depois preenche esse buffer privado. Um budget exato inclui escapes e
multibyte Unicode. Não há StringBuilder/String de documento no encode.
A árvore intermediária continua presente; decode mantém bytes/string/árvore/model
nas fases aplicáveis. Não é streaming nem promessa de heap ilimitado.
[Discovery, custos e compatibilidade](../quality/air-capacity.md).

## Evidência, risco e continuação

[CodecSuite](../../air-json/src/test/java/io/github/gustavo2358/air/json/CodecSuite.java),
[inventário nominal](../evals/transport-checks.json) e
[evidência/challenges](../quality/air-json-implementation.md) protegem o escopo declarado.
O [golden manual](../../air-json/src/test/resources/goback.canonical.json) não veio
do encoder. Seus fatos são derivados do 0B §2.1 e dos spans/textos no snapshot lower
referido por 0B. Usa namespace `goback-0b-manual`, IDs locais e sourceKeys deliberadamente
legíveis, completos e opacos; não simula o algoritmo de identidade do lower nem alega
ser bytes produzidos por ele. O oracle Java foi construído separadamente, com os
mesmos fatos concretos. Um formatter JSON padrão apenas ordenou/minificou os fatos
wire escritos manualmente, antes da implementação; não fez conversão AIR.

Integração lower/CFG, adaptadores de arquivo, E2E, segunda implementação independente,
interoperabilidade cross-language, promoção DRAFT e qualificação completa da §13
permanecem DEFERRED. Dois callers do mesmo codec não são implementações independentes.
Claim desta entrega: implementação Java compartilhada do subset necessário ao primeiro
E2E contra o draft pinado, com regras físicas e preservação testadas no escopo declarado.

## Checkpoint 4B — transporte escalar

[Handoff e evidência](../quality/air-json-scalar-assign.md) documentam contrato 4C,
probe N/2N, limites e pin handoff 4D. O código de produção muda somente os mappings
privados do `air-json`. `air-model`, Validator, camada física, API pública,
POMs/dependências, versões e source lock permanecem intactos.

[Golden escalar manual](../../air-json/src/test/resources/scalar-assign.canonical.json):
14.554 bytes, SHA-256 `40b9cec1bcc1c1e40cf3b9e3c48e834835e478e1d84bccafa575d63497ef3b60`.
Os fatos wire foram escritos manualmente e ordenados/minificados com JSON padrão Python,
sem encoder AIR. [Oracle Java](../../air-json/src/test/java/io/github/gustavo2358/air/json/ScalarAssignOracle.java)
foi construído separadamente, sem ler o golden ou usar qualquer mapping.
Não representa saída do lower nem afirma proveniência COBOL: os IDs são opacos,
as oito origens escritas são aproximadas sem spans, as quatro dimensões não
certificadas permanecem UNAVAILABLE e a lacuna de inventário de entradas é preservada.

A suíte compara as quatro igualdades e bytes determinísticos; guarda o hash literal
e verifica que o bytecode do oracle não depende do codec/arquivo. Isso protege a
independência do teste, sem alegar segunda implementação do binding.
O golden GOBACK 1A permanece inalterado. Os 57 checks anteriores permanecem;
seu teste de inventários adiados retira objects/storage da lista de contêineres
sem cobertura. Os novos contracasos os validam por shape, inclusive elemento
arbitrário que agora é INPUT_ERROR. Essa mudança decorre da cobertura, sem aceitar
conteúdo silenciosamente ou alterar expected para encobrir defeito.

O scanner do harness permite exatamente o novo resource e exige ambos os goldens;
o contracaso de remoção verifica a exigência. Não há allowlist genérica para resources.
Qualquer forma reconhecida fora do subset interrompe em IMPLEMENTATION_LIMIT,
sem certificação do restante do payload. TextValue e displayName não recebem trim,
case folding ou normalização Unicode; somente a gramática física vigente se aplica.

## CP6 W1B — perfil de transporte para W1C

**Invoke parcialmente coberto pelo codec**, não suporte integral de invocações.
O formato continua binding 1.0.0 DRAFT / AIR 2.0.0; model e Validator não mudaram.
[Evidência, REDs e gates](../quality/cp6-w1b-invoke.md).

| Forma que W1C pode emitir | Campos preservados / restrição |
| --- | --- |
| Operations.Invoke | action Text exato, Header completo; é terminador |
| LiteralTarget | category, namespace, name Unicode inclusive vazio/espaços, namePolicy, origin |
| ComputedTarget | mesmos campos; Expression Literal(TextValue) ou Read(ObjectPlace) |
| NamePolicy | ExactName ou UnknownName(UncertaintyId); ExtensionName não coberto |
| Operand e Place | Header com OperandId/owner/role/origin; ObjectPlace(ObjectId). Read não duplica TypeRef: conhecido text vem do Object/Cell |
| arguments / results | listas vazias; modos de argumentos e resultados com conteúdo não cobertos |
| ExternalSignature | Signature com parameters.known e results.known vazios; cada remainder NoRemainder ou UnknownRemainder, origem própria. Closed vazio significa zero aridade; unknown vazio não significa zero aridade |
| effectOperands | lista ordenada de ObjectPlace; ocorrências identificadas usadas por mustOverwrite |
| EffectBound | otherwise ForeignEffects; perOutcome vazio. reads/writes NoMemory ou WithinMemory; mustOverwrite OperandId[] preservado |
| MemoryScope | VisibleMemory(unit, includingExternal) ou AllMemory(publication, includingEnvironment), inclusive flags false quando a Publication as declara |
| InvocationOutcomes | known ordenado: Normal, Exceptional(tag, destination), AnyException(destination), HaltAlternative, Diverge. Destination Handler(label) ou Propagate |
| ControlBound | NoControl ou WithinControl(UnitControl/AllControl); UnitControl conserva todas as seis flags. Restante aberto nunca é fechado pelo codec |
| ContractKnowledge | KnownContract(ContractRef authority/version/evidence OriginId[]) ou UnknownContract(UncertaintyId) |
| Evidência | Written/Derived origins, IDs completos, uncertainties, cobertura/PARTIAL e precisão já suportados; não substituir origens específicas pela origem da operação |

**Não cobertos:** InternalTarget, EntrySignature, ExtensionName, qualquer Argument,
results não vazios, Parameter/ResultSlot, efeitos perOutcome, ObjectsMemory,
StorageMemory, MemoryUnion, LabelsControl, ControlUnion, TrimRight, FitText,
expressões/places adicionais e origens Contractual/Unavailable. W2C acrescenta Unknown
e Premise(DisjointStorage), conforme o perfil abaixo.
Essas formas continuam `IMPLEMENTATION_LIMIT`; manifestos com conteúdo continuam
`UNSUPPORTED_CAPABILITY`. Demais limites de 1A/4B permanecem em vigor.

A admissão normal de `new AirJson().encode/decode` mudou para todo transporte AIR:
`SEMANTIC_OBLIGATION` isolado é não bloqueante. O Validator continua emitindo I-56,
com o mesmo subject/detail antes e depois do round-trip. Sucesso de transporte,
validade estrutural e satisfação da obrigação são fatos distintos. Não há wrapper,
flag, segunda API, negociação de versões, certificação ou ValidationResult no wire.
`INVALID_IR`, `RESOURCE_LIMIT`, `UNSUPPORTED_CAPABILITY`, `VALIDATION_LIMIT` e traversal
incompleto continuam bloqueantes, usando categorias/contagens reais, inclusive
issues omitidos pela retenção. Limite físico ou forma não implementada não é obrigação.

Os mappings novos percorrem listas uma vez, sem resolver targets nem procurar
Object/Storage por operação. As subárvores recursivas fora do subset continuam
recusadas. Action/category/namespace/name são dados: nenhum trim, case folding,
fitting, canonicalização runtime, semântica de linguagem ou análise derivada.

Regressões antigas foram ajustadas apenas onde a coverage mudou: Read com shape
Literal agora falha por INPUT_ERROR, e UnknownBound.unknown possui oráculos próprios.
O guard de tokens no bytecode permite estritamente os accessors normativos
LiteralTarget.name():String e ComputedTarget.name():Expression; Enum.name(), toString()
e ordinais não fornecem tokens wire. Um challenge compilável protege essa distinção.
GOBACK e scalar-assign mantêm bytes e hashes originais.

## CP6 W2C — controle explícito, Unknown e DisjointStorage

Cobertura de transporte para o futuro W2B. [Evidência e handoff](../quality/cp6-w2c-transport.md).
Autoridade: binding pinado §§5–7/10, AIR 01 §§3–4, 02 §§3–4, 03 §3.1 e 06 §5.1.
As versões continuam AIR 2.0.0 e binding 1.0.0 DRAFT. Mudam somente BindingReader e
BindingWriter; model, Validator, política de admissão e camada física permanecem iguais.

| Forma | Preservação / limite |
| --- | --- |
| Jump | kind jump, Header completo e destination LabelId completo; nenhum fallthrough inferido |
| Branch | kind branch, Header, predicate, trueDestination e falseDestination distintos e explícitos, sem reordenação |
| TypeRef | known com type.kind text ou bool em tabela fechada; BoolValue literal e demais tipos continuam fora |
| Unknown | Header/OperandId/owner/role/origin, TypeRef conhecido, dependencies Expression[] ordenadas, remainingReads, reason UncertaintyId completo |
| Dependencies | Literal(TextValue), Read(ObjectPlace) e Unknown recursivo; ocorrências e tipos próprios não são convertidos para o tipo do resultado |
| MemoryBound | none e within visible/all reaproveitam W1B; listas completas não substituem um restante aberto publicado |
| Premise | id PremiseId, authority, justification, origin OriginId e assertion obrigatórios; texto opaco preservado |
| Assertion | somente disjoint_storage com storage StorageId[] em ordem física; sem inferir, expandir, reduzir ou ordenar membros |
| Publication.premises | zero ou mais premissas cobertas; qualquer assertion desconhecida ou fora da cobertura impede retorno integral |

I-02/I-04/I-08/I-11 verificam referências, ownership, posição e BOOL por meio do
Validator existente. I-58 exige pelo menos duas bases existentes distintas.
O codec não verifica a verdade física da separação. I-09 e I-59 permanecem
SEMANTIC_OBLIGATION; I-56 também permanece em composições Invoke. Sucesso de transporte
não satisfaz essas obrigações. SameDomain permanece IMPLEMENTATION_LIMIT nos dois
sentidos; token de assertion desconhecido é INPUT_ERROR. Formas físicas malformadas,
referências pendentes e limites operacionais seguem a taxonomia existente.

O traversal de dependências usa frames explícitos para não introduzir recursão
Java dependente de profundidade. Cada lista é percorrida em ordem, sem joins por
nome, scans de labels/storages/uncertainties ou ordenação semântica. Caminhos de
diagnóstico continuam strings completas; a retenção desses paths em aninhamento
muito profundo tem custo adicional à quantidade de nós. Os probes não alegam
streaming, heap ilimitado ou prova geral de complexidade do Validator.

A fixture principal é AIR model-level e o oracle wire escreve o envelope inteiro
somente com primitivas JSON, sem usar o model oracle ou os mappings. A comparação
inclui campos fechados, todos os IDs, Unicode, precisão, coverage e ordens. Os
hashes W1 foram congelados em main; as mudanças de cobertura apenas retiram bool,
Unknown e premises dos antigos negativos de forma não implementada, substituindo-os
por positivos, shape checks e regras estruturais específicas. Literais booleanos,
SameDomain e demais expressões continuam recusados.

Sem SP decoder, COBOL, predicate evaluation, lower, CFG, dataflow ou dependências.
W2B NOT_STARTED / NOT_AUTHORIZED. W2D NOT_STARTED / NOT_AUTHORIZED.
