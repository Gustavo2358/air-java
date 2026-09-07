# Codec compartilhado AIR JSON — cobertura 1A

O módulo `air-json` implementa o subset transitivo do GOBACK descrito por 0B contra
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

| Formas do binding | 1A implementa | Golden GOBACK / casos dirigidos |
| --- | --- | --- |
| Envelope, Publication, SemanticVersion | versões exatas e todos os contêineres | publicação completa; ambos os round-trips |
| Manifest | required/provided vazios | ambos presentes; conteúdo dá UNSUPPORTED_CAPABILITY |
| Artifact | id/logicalName/contentDigest nullable | dois artifacts; nomes Unicode/digest vazio em variação |
| Unit, BodyKnowledge | available; containingUnit nullable | Unit com Entry/Sequence; inventários múltiplos em variação |
| Entry, Signature | initialLabel nullable; parameters/results known vazios, remainder none | assinatura fechada, origem própria; ausência de label em available é INVALID_IR |
| EntryState | conditions vazio; uncertainties transportadas | vazio no golden |
| Sequence, OperationHeader, Return | instructions e values vazios; Return, header inteiro | controle sem Halt, successor ou entryScope |
| IDs | todas as formas da §4, inclusive OperandId com owner entry/operation | oito domínios exercitados no golden; demais IDs isolados, sem suporte a suas definições |
| Origin | written, derived | oito origens; escrita null/aproximada e IncludeFrames em variação |
| Location, Span, Position, IncludeFrame | line_columns; medidas BigInteger; site nullable | quatro spans exatos; bases/unidades/exclusividade/includes e números grandes em variações |
| Coverage, CoverageItem | inventário/scope/items/reasons; elimination null | PARTIAL global/unit; dois itens e outputs heterogêneos |
| Precision, Claim, Uncertainty, Dimension | todos os campos/tokens catalogados | cinco claims e cinco lacunas; dados opacos modificados em teste |
| FactScope | publication, unit, entities | três formas preservadas; IDs não ampliam escopos |
| storage/resources/artifactRelations/premises; objects/visibleObjects/completionPorts | contêineres vazios obrigatórios | omissão/null recusados; conteúdo falha explicitamente |

**Fora da cobertura:** BodyKnowledge.unavailable, origens contractual/unavailable,
Location.offsets, Elimination com conteúdo, Capability com conteúdo, Parameter /
ResultSlot / UnknownBound.unknown, TypeRef/Type, valores/literais (incluindo bytes e
decimal), expressões/locais, objetos/memória, condições iniciais, recursos/relações,
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
