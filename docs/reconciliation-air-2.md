# Reconciliação com a Analysis IR 2.0.0

Este registro compara o `air-java` no baseline `2108294d9dfeb89d0019ce75fab27172b15a75b9`
com a `main` normativa de `Gustavo2358/analysis-ir` em
`122ce54e1b9ef9b00646f93ece409ca8b63bc933`.

A direção de autoridade usada foi:

```text
analysis-ir (normativa)
        ↓
air-java (modelo e verificador estrutural Java)
        ↓
produtores e consumidores
```

`bindings/revisao-json-v1.md` foi o handoff explícito desta mudança.
`bindings/json-v1.md` descreve um transporte futuro em estado DRAFT; nenhum codec,
DTO de transporte, filesystem adapter ou dependência JSON foi adicionado aqui.

## Discovery sistemático

| Área AIR ↔ Java | Classificação inicial | Constatação |
| --- | --- | --- |
| Imutabilidade, `TypeRef`, valores core e sequências | `MATCH` | Records e listas defensivas preservavam os fatos; `unknown_type` já não era wildcard. |
| Operações core e extensões tipadas existentes | `MATCH` | As formas normativas já estavam separadas de CFG, efeitos calculados, RD e values. |
| Memória: células, regiões, aliases, choices e limites | `MATCH` parcial | O núcleo estava presente, mas certificados privados ainda contaminavam alguns acessos. |
| Escopos estáticos de `sameDomain` | `MATCH` | A álgebra finita e a não propagação entre sites já correspondiam à AIR. |
| `Publication.contracts`, `ContractId`, `Contract` | `JAVA_DRIFT` | O Java criava inventário e identidade sem autoridade normativa. |
| `ContractRef` como valor e assinatura externa por site | `MISSING_IN_JAVA` | Faltavam autoridade/versão/evidência fechada e assinatura materializada no `invoke`. |
| `ResourceTarget(ResourceId)` | `JAVA_DRIFT` | Uma quarta forma executável não existia na AIR. |
| `disjoint_storage` | `JAVA_DRIFT` | A forma Java tinha `FactScope`; faltavam cardinalidade/unicidade e obrigação universal explícitas. |
| `SafetyAssertion`, `SafetyProperty` e certificados privados | `JAVA_DRIFT` | Tokens do Validator apareciam como fatos AIR. |
| `ContractName` e codec por contrato de chamada | `JAVA_DRIFT` | Autoridade de chamada era usada como política de nome/codec sem manifesto de extensão. |
| Assinatura com uma lacuna `incomplete` | `JAVA_DRIFT` | Aridade de parâmetros e resultados não tinha restantes independentes; modo/binding não preservavam desconhecimento próprio. |
| `return.entryScope` | `JAVA_DRIFT` | O Validator podia receber uma seleção de entradas que não pertence ao comportamento AIR. |
| Outcomes de chamada e envelope genérico no mesmo tipo | `JAVA_DRIFT` | Faltavam `jump`, `return`, `continue` no envelope e a restrição própria de invocação. |
| Ocorrências só em effects e resultados de `opaque` | `JAVA_DRIFT` | Algumas referências duplicavam ou deixavam de materializar a ocorrência única. |
| `RelationId` e subjects externos por contrato | `JAVA_DRIFT` | Relações e posições externas não carregavam o namespace normativo correto. |
| `ArtifactRelationId`, owners de operandos e subjects por site | `MISSING_IN_JAVA` | Faltavam identidade da relação e sujeitos externos identificados pelo `OperationId` do invoke. |
| Entrada declarada sem corpo | `MATCH` parcial | A representação de disponibilidade existia; o fechamento de label precisava de regressão explícita. |
| Naturais limitados por `int`/`long` | `JAVA_DRIFT` | Versões, escalas, posições, larguras e contagens herdavam teto do runtime. |
| Proveniência por offsets | `MISSING_IN_JAVA` | Só linha/coluna era representável. |
| Eliminação por `PremiseId` safety | `JAVA_DRIFT` | Coverage exigia identidade de premissa onde AIR exige regra e origem. |
| Records, sealed types, `Optional`, índices internos | `IMPLEMENTATION_DETAIL_VALID` | São escolhas Java sem significado AIR adicional, documentadas como tal. |
| Limites de profundidade/entidades/diagnósticos do Validator | `IMPLEMENTATION_DETAIL_VALID` | São limites operacionais observáveis; produzem `INCOMPLETE_VALIDATION`, nunca mudam a validade normativa. |

O trabalho encontrou ainda três drifts não destacados pela API antiga: declaração
de `CompletionPortId` não exigia `control.local@1`; um uso calculado em
`DependencyEnvelope` não fixava `before` na operação proprietária; e a verdade dos
fatos contratuais materializados não era reportada como obrigação semântica I-56.

## Resultado da reconciliação

| Divergência AIR ↔ Java | Antes | Depois | Evidência/teste |
| --- | --- | --- | --- |
| Contracts | Inventário top-level, `ContractId` e entidade `Contract` | `ContractRef(authority, version, evidence)`; conteúdo no `invoke`; ausência por `CONTRACT_UNKNOWN` | `Publication has no contracts inventory`; `ContractRef dangling evidence is rejected`; `same contract authority does not merge invocation sites` |
| ResourceTarget | `ResourceId` era target executável | Target selado somente em internal/literal/computed; `ResourceId` permanece declarativo | `Target admits exactly internal literal computed`; `ResourceId remains declarative rather than executable` |
| DisjointStorage | Lista mais `FactScope` | `disjoint_storage(StorageId[])`, mínimo dois, distintos, fechamento e obrigação universal I-59 | quatro testes de forma, dangling e não inferência |
| SafetyAssertion | Tokens e ponteiros privados podiam liberar checks | Tipos/campos removidos; bounds, pureza, codec, choice e extensão continuam verificados ou limitados | `private safety API is absent`; testes de slice, codec, pureza e igualdade de extensão |
| Signature | `parameters/results/incomplete`, posição/mode limitados, lookup por contrato | inventários e restos independentes; `BigInteger`; `ModeKnowledge`; `ParameterBinding`; assinatura externa embutida; slots conhecidos conservam `sameDomain` mesmo com restante aberto, sem promover `unknown_type` a `known(T)` | testes `unknown_type`, remainders independentes, transmissão exata/conservadora com remainder aberto, modo desconhecido e posições grandes |
| InvocationOutcomes | Mesmo `Envelope` genérico | `InvocationOutcomes` separado de `ControlEnvelope`, unicidade I-60 e chaves de efeito | duplicatas de normal/tag/catch-all/effect; preservação de todas as alternativas |
| Return | `entryScope` selecionável | Apenas valores; incompatibilidade multi-entry gera limite e obrigação I-61 | `Return has no entryScope component`; dois testes multi-entry |
| Identities | `RelationId`, subjects externos por `ContractId`, owners incompletos | `ArtifactRelationId`; subjects internos/externos por invoke e posição; owners completos; porta local exige capability | testes de relações, owners, sites e colisões de `localId` |
| Provenance | Apenas span de linha/coluna | `Location = LineColumns | Offsets`, naturais arbitrários e unidade explícita | `offset provenance survives without fabricated line columns` |
| Operand occurrences | Effects não tinham local próprio; `opaque` podia duplicar Places | `effectOperands` define Places; effects/envelopes/resultados referem `OperandId` já indexado | `effect-only place occurrence is materialized once`; `opaque result references one pre-existing occurrence` |
| Entry body | Ausência de label pouco exercitada | label ausente somente em corpo `UNAVAILABLE` com lacuna; todo label publicado fecha na unidade | três testes de disponibilidade/label |
| Numeric domains | Diversos `int`/semver Java | `BigInteger` para naturais e inteiros AIR; versão de capability/extensão é texto declarado | testes de versão/escala/posição e gate de compilação |
| Name/codec extensions | `ContractId` de chamada fornecia regra implícita | capacidade de extensão com nome/versão exatos; sem normalização, codec ou igualdade inventada | capability gate e igualdade de extensão como limite explícito |
| Coverage elimination | `PremiseId` genérico | regra e `OriginId`, sem asserção safety artificial | `coverage elimination uses rule and origin, not safety premise` |
| Calculated dependency point | owner/ponto não eram correlacionados | operando pertence à operação, tem papel `RESOURCE_TARGET` e é observado em `before(owner)` | `computed dependency name must be observed before owning operation` |

## Limites mantidos explícitos

O Validator verifica forma, fechamento, ownership, tipos decidíveis, scopes e
contradições locais. Ele não certifica que a autoridade disse a verdade, que os
effects/outcomes cobrem um corpo, que um acesso simbólico é total, nem que um
produtor preservou integralmente sua entrada. Esses casos aparecem como
`SEMANTIC_OBLIGATION`, `VALIDATION_LIMIT` ou `UNSUPPORTED_CAPABILITY`.

Não foram adicionados JSON, Jackson/Gson, rede, filesystem, CLI, COBOL, CFG,
effects calculados, reaching definitions ou possible values.
