# Codec compartilhado AIR JSON — cobertura 1A + 4B

O módulo `air-json` implementa o subset transitivo do GOBACK descrito por 0B e
o transporte escalar Object/Cell/Assign text de 4B contra
**analysis-ir-json / bindingVersion 1.0.0 / airVersion 2.0.0, DRAFT**, no SHA
`122ce54e1b9ef9b00646f93ece409ca8b63bc933`. A autoridade é AIR → binding → codec.
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
Validator permanecem intactos em `air-model`, artefato Maven `air-java`.

Ambos os métodos retornam somente após sucesso integral. `decode` valida bytes,
campos/formas e versões, materializa os fatos exatos e chama `AirValidator`.
`encode` verifica a versão, mapeia explicitamente os fatos, valida AIR e emite
bytes canônicos. Nenhum percurso completa lacunas ou executa análise de CFG.
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
| Entry, Signature | initialLabel nullable; parameters/results known vazios, remainder none | assinatura fechada, origem própria; ausência de label em available é INVALID_IR |
| EntryState | conditions vazio; uncertainties transportadas | vazio no golden |
| Sequence, OperationHeader, Return | instructions ordenadas de Assign; Return com values vazio | GOBACK vazio byte-identical; Assign seguido de Return |
| IDs | todas as formas da §4, inclusive OperandId com owner entry/operation | oito domínios exercitados no golden; demais IDs isolados, sem suporte a suas definições |
| Origin | written, derived | oito origens; escrita null/aproximada e IncludeFrames em variação |
| Location, Span, Position, IncludeFrame | line_columns; medidas BigInteger; site nullable | quatro spans exatos; bases/unidades/exclusividade/includes e números grandes em variações |
| Coverage, CoverageItem | inventário/scope/items/reasons; elimination null | PARTIAL global/unit; dois itens e outputs heterogêneos |
| Precision, Claim, Uncertainty, Dimension | todos os campos/tokens catalogados | cinco claims e cinco lacunas; dados opacos modificados em teste |
| FactScope | publication, unit, entities | três formas preservadas; IDs não ampliam escopos |
| ObjectDeclaration | oito campos do binding, displayName nullable, TypeRef e binding preservados | WS-PGM, nomes vazios/espaçados/Unicode/null sem joins textuais |
| TypeRef, Type | known(text) | Object/Cell; demais formas reconhecidas dão IMPLEMENTATION_LIMIT |
| Storage, StorageHeader, StorageBinding | Cell, header completo e cell(StorageId) | owner nullable; ACTIVATION requer owner por AIR 03 §2 |
| OperandHeader, Place, Expression, LiteralValue | occurrence id/role/origin; ObjectPlace; Literal(TextValue) | duas ocorrências pertencentes ao Assign; sem TypeRef duplicado no Place |
| Assign | header, destination, value | somente ObjectPlace ← Literal(TextValue); sameDomain pelo Validator |
| resources/artifactRelations/premises; visibleObjects/completionPorts | contêineres vazios obrigatórios | omissão/null recusados; conteúdo falha explicitamente |

**Fora da cobertura:** BodyKnowledge.unavailable, origens contractual/unavailable,
Location.offsets, Elimination com conteúdo, Capability com conteúdo, Parameter /
ResultSlot / UnknownBound.unknown, TypeRef.unknown_type, tipos além de text,
literais além de TextValue, expressões além de Literal, Places além de ObjectPlace,
storage/bindings além de Cell/CellBinding, condições iniciais, recursos/relações,
premissas, demais operações core, invocações e extensões/envelopes conservadores.
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
| INCOMPLETE_VALIDATION | limite ou obrigação inconclusiva do Validator; nunca sucesso silencioso |
| IMPLEMENTATION_LIMIT | forma fora da cobertura, limite explícito do transporte ou representabilidade Java identificada |

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
CoverageItem.sourceKey, Uncertainty.code/reason, Derived.rule e IncludeFrame.requestedName.
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

Por default: 16 MiB por documento e profundidade 128; `Limits` permite configurar
bytes e profundidade 1..256. `ValidationOptions` mantém limites independentes do
Validator (default 128 níveis, 2.000.000 entidades, 10.000 issues). Tetos atingidos
nunca mudam números ou retornam prefixos. A materialização usa uma árvore intermediária;
esta entrega não é streaming nem gate de desempenho. Parser caminha pelo documento;
writer ordena propriedades por objeto e percorre arrays sem ordenar. Conversão de
BigInteger e Validator têm custos próprios; não se alega tempo linear universal.

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
