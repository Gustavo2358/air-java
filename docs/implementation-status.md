# Cobertura implementada e limites

Topologia pós-0C-I: `air-java-parent:pom` agrega `air-model` (artifactId `air-java`,
modelo + validation) e `air-json` (codec 1A, dependência direta no modelo).
A cobertura semântica do model abaixo não mudou. O transporte tem
[cobertura própria e limites explícitos](engineering/air-json.md), comprovados
por [golden manual e suíte](quality/air-json-implementation.md).

Baseline normativo: Analysis IR 2.0.0, `Gustavo2358/analysis-ir@51b4d9a8ae0364232bd97103cd73a77e1a34996c`.
A biblioteca `0.1.0-SNAPSHOT` é uma implementação Java revisável. Ela não declara
conformidade integral de Producer, Validator ou Consumer com todos os perfis AIR.

O discovery e a migração do baseline Java anterior estão em
`docs/reconciliation-air-2.md`.

## Modelo materializado

| Área normativa | Representação Java atual |
| --- | --- |
| Publicação e identidades | `Publication`, `Unit`, `Sequence`, `Ids`, `ArtifactRelationId` |
| Entradas | assinatura normalizada, inventários/restantes independentes e `EntryState` |
| Tipos e valores | `known(T)`, `unknown_type(u)`, core/extension/label; naturais AIR por `BigInteger` |
| Expressões e locais | ocorrências por `OperandId`, inclusive owners de operação ou entrada |
| Armazenamento | célula, região, view/codec, alias, alternativas e binding desconhecido |
| Operações comuns | assign, havoc.must, havoc.may, nop, copy_bytes |
| Terminadores core | jump, branch, dispatch, invoke, return, raise, halt, opaque |
| Extensões padronizadas | local.invoke/boundary/resume/unwind e indirect.jump, com capability/fallback |
| Interações | targets internal/literal/computed, assinatura por site, effects, outcomes e `ContractRef` |
| Controle incompleto | `InvocationOutcomes` separado de `ControlEnvelope` |
| Incompletude | precisão, coverage, uncertainties, memory/control/dependency envelopes |
| Proveniência | escrita, derivada, contratual, indisponível; linha/coluna ou offsets com unidade |
| Premissas | `sameDomain` com subjects/scopes; `disjoint_storage` universal |
| Relações estruturais | artifacts, resources declarativos e artifact relations sem execução fictícia |

Não existem no domínio atual `Publication.contracts`, `ContractId`, entidade
`Contract`, `ResourceTarget`, `SafetyAssertion`, `SafetyProperty`, certificados
privados de bounds/acesso/choice ou `return.entryScope`.

## Checks estáticos implementados

O Validator cobre, na parcela decidível a partir de uma publicação isolada:

- unicidade, namespace completo e fechamento de identidades/referências;
- owners de unidade, entrada, label, operação, objeto, porta e operando;
- body disponível/indisponível e fechamento de labels;
- ciclos de origem, contenção e aliases exatos;
- visibilidade explícita e associação object/storage/codec;
- assinatura ordenada, posição contígua quando fechada, modo, binding e restos
  independentes de parâmetros/resultados;
- assinatura interna igual ao target e assinatura externa materializada no site;
- `ContractRef` com evidência existente e `CONTRACT_UNKNOWN` tipado;
- target calculado `known(text)` e ausência de `ResourceId` executável;
- cardinalidade quando o inventário é fechado; com precisão de valores `EXACT`,
  transmissão de todo slot conhecido cujo modo permite precisão, mesmo com restante
  de aridade aberto; `sameDomain` não promove `unknown_type` a `known(T)`;
- derivação finita de `sameDomain`, aplicação de scopes, choices universais e
  contradições de domínios concretos;
- `disjoint_storage` com pelo menos duas bases existentes distintas;
- unicidade de normal/tag/catch-all e de effect bound por outcome;
- distinção de `InvocationOutcomes`/`ControlEnvelope`, labels locais e regra de
  `continue` apenas em fallback de operação comum;
- ocorrência única para effect-only Places e referências de resultados `opaque`;
- precondições constantes de slice, faixas, codecs core e escrita literal;
- capabilities/versionamento exatos para extensões conhecidas;
- provenance, coverage, elimination e razões tipadas de incompletude;
- limites operacionais observáveis e determinismo/reentrância do Validator.

Os diagnósticos citam invariantes como I-01–I-13, I-17/I-20, I-23/I-26,
I-28–I-32, I-36/I-43/I-46, I-49–I-61. A citação identifica a regra aplicada;
não alega certificação de todas as dimensões semânticas daquele invariável.

## Obrigações e limites explícitos

1. **Verdade do produtor e das premissas.** O Validator não observa a entrada do
   lowerer nem certifica `sameDomain`, `disjoint_storage`, pureza, coverage ou a
   correspondência de facts com linguagem/ambiente. Registra
   `SEMANTIC_OBLIGATION`.
2. **Contrato materializado.** Forma, fechamento e contradições locais de
   assinatura/effects/outcomes são verificados. A completude em relação à
   autoridade externa ou a um corpo requer evidência própria (I-56).
3. **Precondições simbólicas.** Bounds não literais, conteúdo de codecs não
   decidido, igualdade de domínio de extensão e views parcialmente sobrepostas
   podem produzir `VALIDATION_LIMIT`; não são aceitos como provados.
4. **Return com múltiplas entradas.** Inventários estaticamente iguais são
   verificados para todas as entradas. Se a compatibilidade depende de
   alcançabilidade, o Validator registra limite/I-61 e não escolhe uma entrada.
5. **Análises derivadas.** Não há CFG, strong/weak update, cálculo de effects,
   reaching definitions, possible values ou grafo final de dependências.
6. **Perfis.** Claims de perfil exigem execução dos oráculos correspondentes fora
   deste check estrutural. A suíte local não é certificação integral dos perfis.
7. **Extensões arbitrárias.** Tipo/codec/política nomeada permanece identificada.
   Sem implementação do manifesto, o resultado é unsupported/limit, nunca
   `TYPE_UNKNOWN` ou semântica inventada.
8. **Transporte.** JSON, schema, reader/writer, filesystem e round-trip ficam em
   adapters. O módulo air-json implementa somente os subsets 1A + 4B + CP6 W1B/W2C declarados na política.

## Detalhes Java sem autoridade semântica

Records, sealed interfaces, enums, `Optional`, listas defensivas, representação
de bytes como `List<Integer>`, o booleano interno de signedness de codec e índices
do Validator são escolhas de implementação. `ProofSite` representa o site estático
usado internamente para aplicar a regra AIR; não é inventário publicado.

`ValidationOptions` default não impõe cardinalidade/profundidade arbitrária.
Os tetos int são representabilidade da API; budgets menores são opt-in. Exhaustion
produz RESOURCE_LIMIT, status INCOMPLETE_VALIDATION (ou INVALID_IR se já foi
provado erro) e traversalCompleted=false. Contagens por kind preservam inclusive
issues não retidos; reter até K mensagens não para a validação. Não há recuperação
de OOM nem promessa de recursos ilimitados. [CORE-SIZE-001](quality/air-capacity.md).

## Consumo por cobol-lowering

Não há blocker conhecido no modelo reconciliado. Um produtor precisa, porém,
migrar para a API incompatível: materializar assinatura/effects/outcomes por
`invoke`, fornecer `ContractRef`/lacunas e subjects por site, definir ocorrências
de effects e não emitir as formas removidas. Isso é trabalho de adapter/lowering,
não uma compatibilidade retroativa dentro de `air-java`.


## CP6 W1B — transporte Invoke

AirJson admite LiteralTarget e ComputedTarget(Read(ObjectPlace) ou Literal text),
arguments/results vazios, assinatura externa de inventários conhecidos vazios com
restantes none/unknown, efeitos gerais com escopos visible/all, outcomes finitos e
restante unit/all, Known/UnknownContract. [Perfil exato e exclusões](engineering/air-json.md#cp6-w1b--perfil-de-transporte-para-w1c).

SEMANTIC_OBLIGATION isolado não impede encode/decode; o Validator permanece igual e
reconstitui I-56 da Publication decodificada. Transportável com obrigação pendente
não significa obrigação satisfeita nem perfil certificado. Todas as outras falhas
de validação e limites continuam bloqueantes. Sem mudança de AIR/binding version,
semântica de model/Validator, linguagem fonte, lowering, CFG ou análise derivada.
W1C/W1D/W2 permanecem fora da autorização desta entrega.


## CP6 W2C — Branch/Jump/Unknown BOOL/DisjointStorage

Reader e writer transportam Branch e Jump com destinos explícitos, known(bool),
Unknown com dependências cobertas e remainingReads/reason preservados, e listas de
Premise(DisjointStorage) em ordem. [Perfil e limites](engineering/air-json.md#cp6-w2c--controle-explícito-unknown-e-disjointstorage)
e [qualificação](quality/cp6-w2c-transport.md). Sem mudança de model/Validator, normas,
versões ou dependências. SameDomain, BoolValue e demais formas não cobertas continuam
limites explícitos. I-09/I-59 continuam obrigações semânticas do produtor.
W2B NOT_STARTED / NOT_AUTHORIZED. W2D NOT_STARTED / NOT_AUTHORIZED.
