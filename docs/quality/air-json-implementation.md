# Evidência 1A — codec AIR JSON compartilhado

[Work item](../work/active/WORK-AIR-JSON-001/work-item.json),
[API/cobertura](../engineering/air-json.md), [baseline](../sources/air-json-baseline.json).
Self-review desta sessão; não é revisão independente nem aprovação humana.

## Entrada e autoridade

Main air-java limpa após fetch, checkout main e pull --ff-only:
`71937dfe88bac4dae10f6f195731acac638c2d29`, merge real do PR #4 / 0C-I
às 2026-09-07T18:08:56Z. GitHub via gh direto e git ls-remote confirmaram o SHA.
O conector inicialmente respondeu OPEN; a consulta direta subsequente confirmou
MERGED e os pais 59120fae… / 557d71c0…; nenhum merge SHA de teste foi tomado por merge real.
Branch `feat/air-json-codec-mvp`; lifecycle 0C-I arquivado com o SHA confirmado e
WORK-AIR-JSON-001 implementation criado antes de produto.

0B PR #3 / analysis-ir confirmado MERGED em 51b4d9a8ae0364232bd97103cd73a77e1a34996c.
Handoff lido por API nesse merge, pois main local do checkout irmão estava atrasada;
nenhum fetch/checkout/edição foi feito nele. Norma e binding extraídos por git archive
**somente de 122ce54e1b9ef9b00646f93ece409ca8b63bc933** para /tmp/air-1a/authority.
Hashes dos arquivos utilizados estão na baseline. O source lock conserva repository,
ref, versão AIR e seções; apenas o status de implementação JSON foi atualizado.

O roadmap local mudou desde o snapshot 0C-D: hash consultado em 1A
df93743bfc633fc0d6eac05113befca46a73fec8c3f13b9fe7a2224f30a5d587.
Seções 1.2–1.6 e 8 foram consultadas em leitura. Seu item genérico de merge 1A
não supera a parada explícita do usuário no PR para review humano.

## Oráculos e RED inicial

O golden foi escrito manualmente com fatos 0B §2.1 e coordenadas/textos do snapshot
lower e2488a362478057de7d59cdf9ae2b38b1f4040d3. A namespace/IDs/sourceKeys do oracle
são opacos, completos e legíveis; não há execução/cópia de um writer AIR downstream.
O formatter padrão apenas ordenou/minificou essa estrutura manual **antes do encoder**.
A Publication Java foi construída separadamente em GobackOracle: dois artifacts,
oito origins, cinco uncertainties, spans original/expandido da Entry e GOBACK,
Sequence/Return/Entry, assinaturas fechadas vazias, CoverageItems statement → entry,
PARTIAL publication/unit, EXACT control e UNAVAILABLE nas outras dimensões.

A Publication manual passou no AirValidator. O primeiro teste encode falhou com
`UnsupportedOperationException: 1A not implemented` no stub, exit 1. A topologia
0C-I também deu RED na primeira fixture: `air-json must remain empty; Unowned
source/resource`. Após codec/mapeamentos e política 1A, ambos ficaram GREEN.
O stub foi substituído; não há implementação alternativa ou fallback incompleto.

## Evidência de preservação

Os 43 checks nominais comparam encode byte a byte com o golden independente, decode
com todos os records/listas da Publication manual e os dois round-trips.
Casos adicionais: property order/whitespace/escapes, Unicode suplementar/decomposto,
controles e canonical escaping, coordinate BigInteger acima de long/2^53,
localização null/aproximada, includes ordenados com site null/presente, múltiplas
unidades/entradas/sequências e permutações de arrays. Códigos/reason/source name
alterados por metamorfismo preservam estrutura/escopos do transporte.

Negativos iteram por **todos os campos e objetos do golden**, removendo cada campo,
introduzindo unknown field e duplicata normal/escapada. Incluem UTF-8, BOM,
surrogates, trailing/segundo documento/raízes, formas/tokens, null, números físicos,
versões exatas, IDs completos/domínio/owner/redefinição/ref pendente, perda de
uncertainty/origin e PARTIAL sem reason. A razão ausente em PARTIAL produz I-28,
INVALID_IR; limite do Validator permanece INCOMPLETE_VALIDATION com issue original.
Halt válido e offsets válidos sem implementação produzem IMPLEMENTATION_LIMIT;
manifesto com capability produz UNSUPPORTED_CAPABILITY. Nenhuma Publication parcial.

## Challenges e restauração

Comando opt-in: `python3 -B scripts/challenge_json.py --output /tmp/air-1a/challenges.json`.
O script copia fontes/tests/resources para diretório temporário, compila modelo
intacto, testa GREEN, altera uma fonte do codec por vez, recompila e executa a suíte.
Compilação malsucedida não conta como RED semântico. Cada fonte é restaurada byte a
byte em finally; hashes de restauração ficam no relatório. Segundo GREEN após todas
as mutações. Checkout original não é adulterado para executar o challenge.

| Mutação real de implementação | Teste que deve ficar RED |
| --- | --- |
| PARTIAL → COMPLETE | encode equals independent canonical golden |
| UNAVAILABLE → EXACT | encode equals independent canonical golden |
| remover primeira uncertainty | encode equals independent canonical golden |
| remover primeira origin | encode equals independent canonical golden |
| ordenar artifacts por localId | encode equals independent canonical golden |
| retirar detecção de duplicate key | duplicate property rejected at every object depth |
| aceitar JSON number como string | integer JSON numbers forbidden |
| ignorar comparação de versões | all envelope versions require exact identity |
| ignorar unknown fields | unknown fields rejected throughout supported tree |
| emitir newline final | encode equals independent canonical golden |
| escapar á como Unicode escape | Unicode scalar text and canonical escaping |
| decodificar Halt como Return vazio | valid Halt unsupported and Return never silently changed |

**Challenge revelou lacuna real no oracle:** inicialmente as duplicatas repetiam a
chave com null; retirar a guarda de duplicatas sobreviveu porque a falha de tipo
posterior ainda era INPUT_ERROR. A regressão foi corrigida para repetir **o mesmo
valor válido**, inclusive via chave escapada, e exigir o diagnóstico de duplicata.
Com essa correção a mutação ficou RED por aceitação indevida. O parser original já
tinha a guarda correta; não se ajustou o expected para acomodar implementação errada.

A revisão final também refinou as mutações de remoção de origin/uncertainty:
`subList(1, size)` abortava no caso novo de inventário vazio antes do golden.
As mutações passaram a usar `stream().skip(1)`, que conserva vazio e descarta o
primeiro fato quando presente. A exigência é então RED por divergência do golden,
sem contar o erro de índice como prova de preservação de evidence.

Os gates têm contracasos permanentes executados para omissão de JSON/suíte/política/
golden, source sem owner, dependência externa não autorizada, model → JSON/Jackson/
Gson, grafos compile/runtime/optional/test e dependência transitiva, ciclos,
modelo copiado/shaded no JAR JSON, outputs stale/ausentes e suite ignorada/duplicada.
O desafio Maven real `-DskipTests=true verify` foi RED no gate de flags mesmo com
logs/classes do build GREEN anterior. Não houve edição persistente por esse probe.

## Verificação de entrega

Full final executado com `AIR_MAVEN_REPO=/tmp/air-0ci/m2 python3 -B scripts/harness/run.py full`,
exit 0: docs/MANIFEST, **86 testes do harness**, architecture, **172 model + 43 JSON**
via check.sh e os mesmos **172 + 43** via root Maven clean verify. As suítes foram
executadas uma vez por build, conferidas por nomes/ordem/contagem. Nenhum Surefire
vazio foi aceito como evidência. [Recibo dos 12 challenges e 6 gates](air-json-challenges.json)
registra REDs e segundo GREEN de 43 checks, com hashes de restauração.

Git e scope passaram para os 54 caminhos alterados contra a baseline; MANIFEST
conferiu os 141 caminhos versionados. Diff integral e staged revisados nesta sessão,
incluindo produto, oracles, gates, contracasos, lifecycle e documentação. A revisão
corrigiu duas transcrições na evidência (merge 0B e hash do roadmap), confrontadas
com a baseline e os bytes consultados, e moveu o entrypoint do teste Python para
depois de todas as classes de teste. CI remoto e SHA do head serão registrados no
PR após push; esta evidência não inventa resultado remoto.
Evidência intermediária executada antes do check adicional de zero conhecido: `./scripts/check.sh` exit 0, 172 model + 42 JSON;
root `mvn -o -B -ntp -Dmaven.repo.local=/tmp/air-0ci/m2 clean verify` exit 0,
parent → model → JSON, 172 + 42 checks, effective POM/grafo/classes/JAR por owner.
Architecture: 308 classfiles model / 4664 arestas, 16 classfiles JSON / 408 arestas;
JSON compila só com model, model com classpath vazio. Nenhuma dependência externa.
Contagens são resultados observados, não inventário fixo de classes no gate.

## Limites e próximo passo não iniciado

Binding permanece DRAFT. Não há alegação de qualificação integral da §13, catálogo
completo, interoperabilidade independente/universal, conformidade cross-language
ou desempenho. Base64/decimal/valores/operandos e demais formas listadas na política
ficam DEFERRED; os IDs de referência são transportáveis, suas definições não estão
implementadas apenas por isso. Limites de bytes/profundidade são operacionais.

Nenhuma alteração semântica, annotation ou dependência em air-model. Nenhum código,
POM ou pin alterado em lower/CFG; proleap/analysis-ir também somente leitura. Nenhum
filesystem adapter, CLI, rede de produto ou segunda implementação. Sem merge,
auto-merge, 2A/2B ou E2E. Próxima decisão é review humano do PR 1A.

## Remediação solicitada em review humano

Retomada no PR #5 e branch feat/air-json-codec-mvp, head local/remoto de entrada
b2c923230b2a857a5cb3beb8baadcb5eb9f77aef, working tree inicialmente limpa.
AGENTS/protocolo lidos; fetch apenas de origin, sem checkout de main, rebase,
force-push, nova branch/PR ou alterações nos quatro repositórios irmãos.

Finding 1: seis sites do writer derivavam tokens de Enum.name. Foram substituídos
por cinco mappings exaustivos de literais do binding §10.4: Dimension,
PrecisionStatus, CoverageStatus, InventoryStatus e ColumnUnit. Reader já era
explícito. Oracle novo compara os 20 tokens com referências semânticas e strings
literais independentes, inclusive os tokens não exercitados no GOBACK. A proteção
arquitetural inspeciona referências de métodos no bytecode do writer e recusa
name/toString/String.valueOf e concatenação de model para String. O teste ficou
RED na implementação de entrada por `Runtime token authority: name`, apesar de
o oracle de bytes continuar GREEN; ficou GREEN após os mappings explícitos.

Finding 2, parte com autoridade identificada: o site de materialização associa
regra explícita a Derived sem inputs (I-36/AIR 06 §5), Unit available sem entrada
(AIR 01 §2) ou sequência (AIR 01 §3), Uncertainty sem dimensão afetada (AIR 06 §4),
domínio de ID incompatível (I-02/AIR 01 §4) e posição inválida de terminador
(I-04/AIR 01 §3). As verificações ocorrem depois de ler os campos físicos locais;
nenhuma regra é inferida da mensagem Java. ValidationIssue local traz rule/detail,
path() aponta para o campo/site. Os issues originais do AirValidator são comparados
inteiros para encode/decode, incluindo subject e detail, sem transformá-los.
O novo oracle local ficou RED na entrada (site genérico e issues ausentes) e GREEN
após a correção. Os 43 checks anteriores permanecem, com quatro checks novos: 47.

**Blocker normativo identificado conforme a instrução do usuário:** o pin não
estabelece os predicados Java `lineBase/columnBase ∈ {0,1}`, `Text.isBlank=false`
para todo ID/metadado, nem cardinalidade positiva de FactScope.entities. Consultas
`git show`/`git grep` em bindings/, especificacao/ e conformidade/ foram confirmadas
por consulta independente da sessão revisora 01a07d3e-2004-7483-b05f-ddc241b6bdf5.
Não há justificativa para inventar I-32/I-36/I-01 para esses predicados.

- Binding §§3/10.3 declara Natural/Text, preservação de base/unidade, e somente
  offsets requer base zero. AIR 06 §5 exige convenção explícita, sem enumerar 0/1.
- Text é sequência de escalares Unicode; §13 pede preservar texto vazio/padding.
  Exigências substanciais de motivo/regra não definem a gramática String.isBlank.
- FactScope.entities é Id[] sem mínimo explícito. I-52 trata de DomainProofScope;
  não pode ser transplantado para proibir FactScope vazio.

O blocker foi levado ao usuário, que autorizou explicitamente IMPLEMENTATION_LIMIT
para os três gaps de representabilidade conhecidos. O patch parcial anterior foi
preservado; havia passado full com 47 checks e 16 mutações, ainda sem commit/push.
Nenhuma regra fictícia foi atribuída aos predicados exclusivos do model.

A continuação adiciona checagens explícitas: bases Natural >1, EntityScope vazio
e Text blank nos campos auditados por binding §§3/4/10.3 e AIR 06 §§4/5. O diagnóstico
inclui o campo exato e `air-java representability limit`; não acusa defeito da
Publication. Não há catch genérico ou interpretação de mensagem de exception.
Falha inesperada de constructor mantém a identidade original para investigação.
Text em tokens/versões preserva sua própria classificação; contentDigest blank,
que o model representa, continua aceito e preservado. A dívida
[BACKLOG-AIR-006 / AIR-MODEL-DRIFT](../work/backlog.md#backlog-air-006--air-model-drift)
registra as três restrições e as alternativas futuras, sem mudar air-model.

A suite passa de 47 para **51 checks**; os 43 originais continuam cobertos. O check
sobre constraints locais foi renomeado para distinguir regra AIR de limite Java,
e seu caso lineBase=2 foi corrigido conforme a decisão humana, sem remover o caso.
Quatro checks adicionais cobrem ambas as bases, EntityScope, Text e injeção de
IllegalArgumentException inesperada na fronteira privada de materialização.
Todos os erros INVALID_IR testados exigem issues não vazios, rule/detail e path;
os issues originais do Validator continuam comparados integralmente.

RED real antes da correção: lineBase=2 produzia INVALID_IR, contrariando o oracle
IMPLEMENTATION_LIMIT. GREEN após correção: transport exit 0, 51 checks, incluindo
os oracles de golden, encode/decode, ambos os round-trips, PARTIAL, UNAVAILABLE,
origins, uncertainties, scopes, ordem, UTF-8/BOM, duplicatas e canonical JSON.

Os **21 challenges** foram executados em cópia descartável: 16 anteriores mais os
cinco abaixo. Todos compilaram e falharam por assertion, com restauração byte a
byte e segundo GREEN de 51 checks. Recibo identifica nome do check e hash restaurado.

| Mutação nova | Check RED / diagnóstico |
| --- | --- |
| lineBase >1 volta a INVALID_IR | constraints AIR vs representabilidade; esperado IMPLEMENTATION_LIMIT |
| columnBase >1 volta a INVALID_IR | Natural span bases; esperado IMPLEMENTATION_LIMIT |
| entities=[] vira INVALID_IR | empty FactScope entities; esperado IMPLEMENTATION_LIMIT |
| Text blank admitido vira INVALID_IR | admitted blank Text; esperado IMPLEMENTATION_LIMIT |
| qualquer IllegalArgumentException vira IMPLEMENTATION_LIMIT | unexpected constructor exceptions; identidade da exceção injetada foi perdida |

Também permanecem RED name/toString/String.valueOf no writer e remoção da regra
local. Nenhuma nova forma, dependência, integração, mudança normativa ou segunda
implementação foi introduzida.

Verificação final local: `full` exit 0 — docs/MANIFEST (141 paths), 86 testes do
harness, architecture 308 model + 17 JSON classfiles / 4664 + 420 arestas, 172 model
+ 51 transport via check.sh e root `mvn clean verify`. `transport` exit 0, 51 checks.
Git/scope, docs/MANIFEST e diff --check passaram após atualizar a evidência.
Diff integral de remediation revisado, sem alteração do golden/model/pin.
SHA publicado e CI do novo head serão recibos remotos no mesmo PR #5; resultados
históricos não certificam o novo commit. Parar para review humano, sem merge.
