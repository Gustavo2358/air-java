# Cobertura implementada e limites

Topologia pós-0C-I: `air-java-parent:pom` agrega `air-model` (artifactId `air-java`,
modelo + validation) e `air-json` (codec 1A, dependência direta no modelo).
A cobertura regional ST-W1 acrescenta o codec IBM1047 explícito e checks de acesso. O transporte tem
[cobertura própria e limites explícitos](engineering/air-json.md), comprovados
por [golden manual e suíte](quality/air-json-implementation.md).

Baseline normativo: Analysis IR 2.0.0, `Gustavo2358/analysis-ir@b26465964fe75f944f6324df63330d69f33d77cd`.
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
| Premissas | `sameDomain` com subjects/scopes; `disjoint_storage` redundante, sem obrigação para bases distintas |
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
- cardinalidade quando o inventário é fechado; independentemente de metadata de precisão,
  transmissão de todo slot conhecido cujo modo permite transmissão, mesmo com restante
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

## ST-W1 — codec explícito e precondições regionais

`Capabilities.IBM1047` identifica a extensão normativa opcional; `MemoryCodecs`
fornece encodeText/decodeText puros para ASCII e a tabela IBM/IANA 1.00 de IBM1047.
Resultados distinguem EXACT, UNSUPPORTED_CODEC, UNREPRESENTABLE_TEXT, INVALID_BYTES
 e EXTENT_MISMATCH, sem valor preciso nos demais estados. UnknownCodec não apaga
bytes e não aciona default. A API só interpreta valores concretos; não calcula RD,
valores possíveis, efeitos ou semântica de uma linguagem fonte.

O Validator reconhece somente nome/versão/domínio exatos de IBM1047. Descarga a
precondição de decodificação pela totalidade da tabela, e de escrita literal pelo
domínio e extensão exatos. ASCII, codec desconhecido, não literal sem prova e
outras extensões mantêm seus limites. BinaryCodec em RegionSlice exige extensão
igual à largura/8, como ViewBinding. Acesso puro a ViewBinding cuja base tem
extensão desconhecida agora emite VALIDATION_LIMIT; a declaração continua válida.
Isso corrige uma precondição antes não verificada, sem transformar o limite em
invalidade nem criar um certificado de segurança. W3 emitirá regiões finitas provadas.

Impacto: APIs aditivas; correções do Validator podem rejeitar como INVALID_IR slices
binárias contraditórias ou limitar acessos antes aceitos sem prova suficiente.
Contratos anteriores escalares e seus goldens permanecem. Nenhuma promoção de
SEMANTIC_OBLIGATION/VALIDATION_LIMIT ou alteração das versões AIR/binding.

ST-W6: codec write proof additionally discharges direct `FitText(Read(IBM1047), n, pad)`
when pad encodes exactly and destination codec/extent agree. Other nonliteral
codec writes retain PRECONDITION_NOT_DISCHARGED; this does not execute value flow.

Storage W6–W8 qualified locally; see [qualification and capability limits](engineering/storage-w8-qualification.md). Model/transport/validation support does not certify producer truth or downstream interpretation. Human review pending.

RF-W1: `entry.possibilities@1` implementa candidatos possíveis com remainder obrigatório. Consulte [contrato JSON](engineering/air-json.md); a nova forma não altera literal forte.

RF-W4 adds `target.possibilities@1` and iterative Place.Choice JSON transport; see the contract rationale and limitations in [air-json](engineering/air-json.md).

## FD-W1 vínculos de recursos

resource.bindings@1 acrescenta ResourceDeclaration/Object/Use e descrições
local/unknown. Modelo, Validator (I-RB-01–03) e codec têm oráculos A1–A4/A6, wire
independente e contracasos. Não cria operações/Target nem efeitos. Declarações
sem uso continuam sem execução; papéis não conferem MUST. Verdade da associação
e classificação continua obrigação semântica do produtor (I-RB-04).


## POSITIVE_MEMORY_TOPOLOGY W1

Bases distintas são independentes no modelo. A admissão de condições simultâneas
`entry.possibilities@1` compara intervalos dentro da mesma base; não procura uma
matriz de premissas negativas. Contradições de literals/aliases na mesma base,
IDs, tipos, codecs e bounds continuam validados. `Precision.OPEN` não dispensa
sameDomain de slots de invocação conhecidos. Claims e coverage não criam efeitos.

O codec transporta a forma existente `nop(header)` da norma/binding, preservando
cobertura e identidade sem operandos fictícios. Não foi criada variante, tag,
versão ou framework. A migração semântica exige o pin acima e consumidores
coordenados; o codec não calcula CFG/values nem altera opt-in físico.

Oráculos executáveis FAST: `PositiveStorageChecks`, `PositiveProjectionChecks`,
regressões de `PossibleEntryChecks`, `ContractSuite` e `W2cChecks`. REDs observados:
PRECONDITION_NOT_DISCHARGED para bases distintas sem disjoint; IMPLEMENTATION_LIMIT
para nop válido; transmissão sem sameDomain aceita indevidamente por Precision.OPEN.
O diagnóstico não mascara os contracasos de integridade. Não se alega qualificação
corporativa ou conformidade global de produtores/consumidores pela biblioteca.

Validação local W1: `python3 -B scripts/harness/lean.py fast` passou em Java21,
com 188 checks de modelo, 129 checks de codec, arquitetura/jdeps e 40 testes do
harness (além dos 12 testes da política lean). Goldens preexistentes permanecem
byte-exatos. Contagens de obrigações W2C mudaram intencionalmente pela remoção
de I-59 redundante; I-09/I-56 e todos os contracasos estruturais permanecem.
