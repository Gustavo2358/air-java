# CORE-SIZE-001 — discovery, desenho e evidência

Base air-java: ce530a7e17ab12b23c48f29425f503ff920b09fb; branch
fix/air-capacity-contract. PR #6 MERGED confirmado por gh (2026-09-09), merge
2026-09-08T16:03:41Z, reviews=[]; lifecycle anterior reconciliado pelo protocolo.
Autoridade read-only: AIR 2.0.0 / analysis-ir
122ce54e1b9ef9b00646f93ece409ca8b63bc933; analysis-ir-json 1.0.0 DRAFT.
Consultados binding §§1–6 e AIR 08 §8 (limites observáveis, cardinalidade preservada),
contratos locais e evidência histórica do Validator/1A/4B. Sem mudança normativa.

## Discovery do baseline

- ValidationOptions defaults: nesting 128, entities 2.000.000, issues 10.000;
  nesting configurável até 512 (não 256, que pertence ao JSON).
- PublicationIndex.add testa identities.size antes de inserir, inclusive duplicatas;
  esgotamento lança ValidationContext.Limit e aborta todos os passes restantes.
- ValidationContext.issue aborta ao emitir o diagnóstico K+1, não apenas sua retenção.
  AirValidator acrescenta VALIDATION_LIMIT/ANALYSIS_LIMIT fora do budget. Status
  deriva somente da lista: INVALID_IR prevalece; caso contrário INCOMPLETE_VALIDATION.
- depth interrompe indexação de operandos, TypeResolver, escopos memory/control,
  alternativas de binding, ScopeRules.intersection e expansão/normalização de choice.
  São chamadas recursivas de profundidade dependente do input. Cycles de
  origens/unidades/aliases já usam Kahn iterativo; resolvedBinding usa cache e loop.
  Graph.root usa compressão e union by size; overlays de escopo têm altura fixa
  publication/unit/operation/invocation. Não há DFS de fluxo/CFG.
- JSON decode compara byte[].length antes de UTF-8 REPORT; ByteBuffer.wrap não copia
  input. Decoder aloca chars, depois String, parser constrói árvore Obj/Arr/Text;
  maps/lists são congelados com cópias defensivas. Reader materializa Publication;
  Validator retém seus índices durante a validação, coexistindo com a árvore.
- Encode mapeia Publication → árvore, valida e escreve recursivamente em StringBuilder;
  faz String + getBytes UTF-8 e testa bytes. Guardas anteriores usam contagem de chars
  e podem falhar antes; não são contagem exata de bytes (UTF-8/escapes). Nenhum prefixo
  sai da API. Limites de bytes/depth são IMPLEMENTATION_LIMIT; Validator usa
  INCOMPLETE_VALIDATION, malformed é INPUT_ERROR, erro estrutural INVALID_IR,
  forma fora do subset IMPLEMENTATION_LIMIT e manifesto UNSUPPORTED_CAPABILITY.
- Parser.value/object/array e Writer.write recursivos; depth sintático conta arestas
  entre Values, root 0 (inclusive filhos escalares), diferente de nesting AIR.
  Mappings 1A/4B têm profundidade estrutural fixa: IDs não embutem definições,
  origens Derived transportam referências. Sem nova variante a implementar.

## Auditoria de joins e retenção existente

Sejam E identidades únicas, O operações, A operandos, R ocorrências de referência,
P premissas, S slots, H profundidade e B bytes. PublicationIndex usa hash sets/maps
LinkedHash, construídos uma vez e retidos por validate: O(E) referências auxiliares;
closure O(R) lookups esperados, custo de hash/equals de IDs inclui seu texto.
TypeResolver memoiza por OperandId, O(A). Visible usa conjuntos por unidade,
O(objects + visible references). Kahn cria graph/degree/reverse/queue por família,
O(V+edges), liberados após cada check. DomainProofEngine indexa premissas por
scope/subject, slots por entry/invocation; union-find global e overlays esparsos,
sem copiar grafo global por operação. Cache de sites O(O); provas/normalização de
choices podem revisitar subárvores por query/premissa: custo depende do total de
ocorrências de candidatos efetivamente examinados, não alegação universal O(E).

Scans adicionais encontrados: manifesto required.contains por uso; outcome effects
× outcomes, opaque results × knownWrites, e Return × entries × result slots.
O último depende de provar sameDomain por EntryId em tipos desconhecidos; não pode
ser substituído indiscriminadamente por escolher a primeira entry. Inventários
closed/known compatíveis permitem reutilização estrutural. A evidência final deve
explicitar correções focalizadas e qualquer custo intrínseco restante.
Sequence.operations aloca lista de instructions + terminator em cada passe (linear,
temporária), soma int potencialmente excede representabilidade de lista. Não é nova
estrutura desta tarefa; o model AIR está protegido contra alterações.

## Desenho escolhido antes do código

Conservar records Options/Limits e construtores int positivos. Defaults de entidades,
nesting, bytes e depth passam a Integer.MAX_VALUE: teto de representabilidade das
coleções/byte[]/String desta API Java, não constante de cobertura ou promessa de heap.
Nenhum 0/-1 sentinel: CFG lê maximumDocumentBytes diretamente em readNBytes.
Valores menores são budgets operacionais opt-in; exhaustion tipada RESOURCE_LIMIT,
com INCOMPLETE_VALIDATION no status legado e completion explícita. OOM não é capturado.

Retenção default conserva 10.000 mensagens por decisão de memória, mas continua
traversal e conta diagnósticos por kind. ValidationResult ganha resumo imutável
com contagens e traversalCompleted, conservando construtor de dois argumentos e
accessors antigos. Status usa contagens totais; nenhuma mensagem omitida é inventada.
Codec consulta contagens e expõe resultado de validação no failure quando disponível.
Falha operacional tem código próprio, separada de gaps de forma/representabilidade.

Substituir recursão dependente do input por frames/iteradores explícitos, preservando
ordem depth-first e verificações pós-filhos. Profundidades internas em long evitam
wrap. Parser mantém gramática existente. Writer conta UTF-8 exato em passe iterativo
antes de alocar byte[] e escreve diretamente na saída privada de tamanho exato;
remove StringBuilder/String/bytes duplicados, sem API streaming ou troca de framework.

## Compatibilidade descoberta (read-only)

cobol-lower HEAD consultado 2329993ce61b33fd7105759e211a1861ca6cb217;
analysis-cfg b84389b6ccf94c259774b82a99bc7296278b65c0; analysis-ir checkout
51b4d9a8ae0364232bd97103cd73a77e1a34996c (norma lida via git show no pin).
Lower usa ValidationOptions.defaults e overload explícito, status/issues; CLI usa
new AirJson e imprime código. CFG preflight passa options e preserva ValidationResult;
AirJsonFileReader chama readNBytes(maximumDocumentBytes) + leitura extra; CLI propaga
AirJsonException. Testes usam budgets (128,1,100), (128,1000,100), etc.

API de entrada source/binary preservada. Evolução aditiva em resultado/enum pode
exigir atualizar switches exaustivos externos; leitura de issues não substitui
contagem total após retenção. Defaults e taxonomia operacional mudam comportamento
intencionalmente (BREAKING comportamental, sem versão nova autorizada). Callers que
requerem limites históricos devem construir Options/Limits explicitamente e tratar
RESOURCE_LIMIT. Nenhum consumidor alterado; suíte downstream não executada aqui.

## Complexidade e retenção do desenho final

Custos esperados de hash incluem leitura/comparação de IDs/texto; não é promessa
adversarial contra colisões da JDK. T é o total de nós semânticos percorridos,
C o total de candidatos de choices examinados nas queries/premissas e J o total
de nós Value JSON. Campos de objetos conhecidos são limitados pelo subset; arrays
não têm cardinalidade fixa. Para JSON genérico, F é o número de campos de um objeto.

| Estrutura/passagem | Criação e dimensão | Lookup/trabalho | Lifetime/retenção |
| --- | --- | --- | --- |
| Walk.Frame + iterador | um por ancestral ativo, H | DFS ordenado pre/post, O(T), cada aresta uma vez por traversal | O(H) frames por percurso; listas children podem reter referências de irmãos; sem cópia global |
| PublicationIndex | E identidades, O operações, A operandos | hash esperado O(1) por ID, O(E + R) base | até retorno de validate, não anexado à Publication/result |
| TypeResolver | cache A e active ≤ H | pós-ordem, cada operando calculado uma vez, filhos por cache | cache O(A) por validate; active e frames só por chamada não cached |
| memory/control/bindings | nós de scopes/bindings | O(nós + referências); binding reexamina cada lista de filhos na saída para homogeneidade | frames O(H); BindingNode via AbstractList lazy, sem lista de candidatos materializada |
| ScopeRules | folhas/intersections da premissa | interseção associativa de regiões fechadas em O(nós), todos os refs visitados | frames O(H), um acumulador; sem hash/equals da árvore profunda |
| choice normalize/expand | subjects visitados por query/premissa | O(C) visitas + union-find amortizado; expansão preserva ordem DFS de links | visited O(subjects tocados), frames O(H), liberados após chamada; overlay só keys tocadas |
| required capabilities | manifesto K | criação O(K), lookup hash esperado O(1), remove uso × manifesto | O(K) por validate |
| outcomeKeys | conjunto por objeto InvocationOutcomes efetivamente consultado | identidade do objeto O(1); cria O(alternativas), queries de effects/points O(1) esperado | soma das alternativas consultadas até fim de validate; não hash estrutural do outcomes |
| writes em opaque | W IDs knownWrites/mustOverwrite | cria O(W), R resultados × lookup esperado O(1) | só a operação atual, O(W) |
| returns vazios/closed | compatibilidade por UnitId | compatibilidade O(entries + slots) uma vez; caso vazio compatível O(1) por Return | cache Boolean por unidade; demais Returns com domínio desconhecido ainda exigem entry × slot × Return por site |
| issueCounts + issues | número fixo de kinds + prefixo K | O(1) esperado por emissão, soma long verificada | O(K) mensagens + um marcador operacional, resumo imutável; totais exatos por kind, sem mensagens falsas |
| Parser.Container | contêineres JSON abertos e nós J | O(B+J) físico esperado; chaves duplicadas por map | O(H) frames + árvore O(J+B) com freezing de maps/lists ao fechar |
| Writer.Frame | contêiner aberto, iterador, sorted keys | duas passagens O(B+J+Σ F log F × comparação de chaves) | O(H+Σ campos em ancestrais) auxiliar; único buffer final B; sem buffer/String de documento |

Lookup de references permanece indexado: oracle injeta Set observável somente no
teste, exige 64 contains e rejeita iterator/stream global. Nenhum contador artificial
foi colocado no produto para esse teste. Graph weights passam a long com soma exata;
queries/diagnostics têm guards de overflow tipados, nunca wrap. Depth usa long nos
frames semânticos e tamanho int da pilha física JSON. Recursão restante em
intersect (no máximo uma troca de argumentos) e graphForRegion/base.root tem
altura fixa da álgebra de scopes, independente de H/E/O.

As listas do model são snapshots imutáveis. Tamanho int dos accessors, String e
byte[] e memória/GC da JVM continuam limites de representação/execução. Não se
alega que qualquer heap sustente Integer.MAX_VALUE. Sequence.operations ainda
materializa cópia temporária por passe e o codec ainda materializa árvore:
capacidade contratual melhorada não transforma a pipeline em streaming/unbounded.
Crescimento real de texto afeta hash/equals/BigInteger e os custos próprios desses
tipos; sem SLA temporal ou linearidade universal para provas de domínio.

## Evolução dos oracles

O primeiro RED compilável foi CORE-SIZE nesting 256 → INCOMPLETE_VALIDATION no
baseline. Uma tentativa anterior teve erro de assinatura do teste e não conta.
O teste histórico de maximumIssues deixou de exigir ANALYSIS_LIMIT por retenção:
agora exige erro, traversal completo e total maior que mensagens materializadas.
Isso corresponde à separação autorizada de retenção/trabalho, sem enfraquecer a
proibição de sucesso parcial. Os testes de budgets explicitamente construídos
agora exigem RESOURCE_LIMIT; malformed profundo chega a INPUT_ERROR depois do
parse completo. Goldens, versões, subset e regras de semantic invalidity intactos.

Os 42 challenges anteriores do codec recebem apenas anchors físicos atualizados
para frames e buffer UTF-8, mantendo as mutações de duplicate/number/canonical
escaping/newline. Recibos anteriores/reviews não foram reescritos. Os challenges
novos verificam defaults reais acima dos antigos limites, counters/completion,
retenção, taxonomia e lookup indexado. Evidência abaixo registra somente execuções.

A ordem permanece determinística. TypeResolver agora calcula filhos antes do pai
em pós-ordem também para variantes cujo TypeRef próprio era anteriormente calculado
antes de visitar filhos no loop global. Portanto, não se promete preservar a ordem
histórica de todos os diagnósticos aninhados. Rule/subject/detail e classe semântica
continuam protegidos; retenção aplica-se ao novo prefixo determinístico. Não derivar
lógica downstream de posição textual de uma mensagem.

Um cliente Java compilado contra ce530a7 foi ligado sem recompilação à implementação
atual e passou encode/decode, ambos validate, construtores/accessors Options/Limits e
construtor legado de ValidationResult. Log: `/tmp/air-capacity-binary/legacy-run.log`.
Isso prova esses símbolos, não compatibilidade reflexiva universal: o record
ValidationResult ganha componente; source com record pattern de dois componentes
precisa migrar, assim como switches exaustivos dos enums e consumidores que contam
apenas issues retidos. Nenhum uso desse pattern foi encontrado nos callers lidos.
Versão não interpretada registra traversalCompleted=false e UNSUPPORTED_CAPABILITY;
não foi executada a validação estrutural, nem houve exhaustion operacional.

## Probes executados e limites das medidas

[Recibo de escala](air-capacity-scale.json); saída bruta
`/tmp/air-capacity-scale.logs/`. JVM Temurin 25.0.4+7 (release 21), G1GC,
-Xms64m -Xmx1536m -Xss256k; cada probe em JVM nova, sem warmup. Não se mediu heap
usado/RSS: heap configurado e contagens lógicas são apenas contexto de execução.
Nenhum threshold de tempo ou memória atua como expected semântico.

| Dimensão | N | 2N | 4N | Classificação e contadores |
| --- | ---: | ---: | ---: | --- |
| Objects/Storage model | 32 | 64 | 128 | STRUCTURALLY_VALID; N pares, uma operação |
| Sequences model | 32 | 64 | 128 | STRUCTURALLY_VALID; N operações |
| Operations model | 32 | 64 | 128 | STRUCTURALLY_VALID; N+1 operações, 2N operandos, N queries |
| Referências model | 32 | 64 | 128 | STRUCTURALLY_VALID; inventário constante, N referências de scope |
| Objects/Storage JSON | 16 | 32 | 64 | round-trip completo; N pares, um Assign |
| Operations JSON | 16 | 32 | 64 | round-trip completo; um Object/Cell, N Assigns |
| Sequences JSON | 16 | 32 | 64 | round-trip completo; N terminadores, um Assign |
| Referências JSON | 16 | 32 | 64 | round-trip completo; N referências, definições constantes |
| Payload Text JSON | 16.384 | 32.768 | 65.536 | STRUCTURALLY_VALID; 2 entidades constantes |
| Entidades dedicadas (Artifacts + Publication) | 500.002 | 1.000.003 | 2.000.005 | STRUCTURALLY_VALID; artifacts 500.001/1.000.002/2.000.004 |
| Bytes JSON dedicados | 5.243.468 | 10.486.348 | 20.972.108 | STRUCTURALLY_VALID; payload 5/10/20 MiB, 2 entidades |
| Nesting AIR (unary/choices/bindings/scopes) | 256 | 512 | 1.024 | STRUCTURALLY_VALID; mesmos tipos/formas, sem stack Java proporcional |
| Depth físico JSON (arrays) | 512 | 1.024 | 2.048 | round-trip físico completo; envelope desconhecido = INPUT_ERROR após parse |

Os bytes exatos têm overhead fixo e os totais de identidades incluem Publication;
N/2N/4N referem-se ao componente aumentado, não exigem duplicar overhead constante.
No caso 4B adicional: 1 Object/Cell, 10.000 Assigns, 10.001 operações,
20.000 operandos, 30.021 entidades, 10.000 domainQueries, 24.747.652 bytes e
1.320.652 nós físicos, agora com default. Arrays/goldens/inteiros continuam intactos.

## Challenges executados

[12 recibos de capacidade](air-capacity-challenges.json) registram GREEN inicial,
compilação, RED identificado, hash do mutante, restore byte-exact e segundo GREEN
individual. Fontes/tests/resources do recibo foram comparados ao checkout final.
Logs brutos: `/tmp/air-capacity-challenges-final.logs/`. A primeira execução também
permanece em `/tmp/air-capacity-challenges.logs/`; a segunda cobre o refinamento da
completion para versão não interpretada. Nenhum mutante ficou no checkout.

| Mutação | Compilou | RED esperado | Restore exato | Segundo GREEN |
| --- | --- | --- | --- | --- |
| maxEntities default 2M real | sim | status incompleto em 2.000.004 Artifacts | sim | sim |
| maxDocumentBytes default 16 MiB | sim | budget no encode de payload 20 MiB | sim | sim |
| exhaustion vira sucesso | sim | exhaustion cannot succeed | sim | sim |
| exhaustion vira unsupported | sim | exhaustion kind | sim | sim |
| retenção aborta trabalho | sim | erro tardio perdido | sim | sim |
| completion marker removido | sim | traversal incompleto não declarado | sim | sim |
| issue operacional removido | sim | retained resource marker | sim | sim |
| recurso JSON vira implementation limit | sim | código errado | sim | sim |
| recurso Validator no codec vira unsupported | sim | código errado | sim | sim |
| kind omitido perde classificação | sim | invalidade tardia perdida | sim | sim |
| resultado incompleto vazio vira sucesso | sim | missing traversal completion | sim | sim |
| scan global por referência | sim | iterator global acionado | sim | sim |

[42 challenges JSON preservados](air-capacity-json-challenges.json) também compilaram,
foram mortos e tiveram restore byte-exact + GREEN final (79 checks), com logs em
`/tmp/air-capacity-json-challenges.logs/`. A execução ocorreu antes do ajuste apenas
da flag para versão AIR desconhecida; os 12 challenges finais e full subsequente
cobrem o snapshot final. Reviews/evidências de 1A/4B não foram reescritos.

## Gates, self-review e limites do handoff

Gates executados nesta branch: fast (86 harness), architecture (325 model + 19 JSON
classfiles, Java 21; 5.001 + 501 dependências no snapshot final), semantic e
transport (179 model + 79 transporte), git/scope (50 paths) e diff --check, exit 0.
Full com rede também passou todos os componentes, inclusive root Maven clean verify
com os mesmos 179+79 checks: `/tmp/air-capacity-full-network.log`.
A primeira execução full falhou apenas na resolução de maven-clean-plugin 3.2.0 por
DNS restrito, depois de todos os gates offline verdes; log preservado em
`/tmp/air-capacity-full-first.log`. Nenhum plugin/dependência/POM foi alterado.
Maven explícito, revisão final e checks remotos são confirmados no handoff/PR;
aqui não se inscreve SHA futuro nem resultado de CI ainda não consultado.

Self-review do diff integral contra origin/main, sem declarar revisor independente:
contratos/consumidores, parser/writer, pós-ordem/escopos, índices, limites/contadores,
oracles, falsificações restauradas, lifecycle e escopo. O ajuste identificado na
revisão foi completion=false para versão não interpretada, com teste de categoria
separada; os 12 challenges finais conferem os hashes de todas as fontes/tests.
Nenhum finding de implementação pendente identificado nesse escopo.

36 arquivos protegidos (model AIR, goldens, source lock, baseline binding e POMs)
foram comparados byte a byte com ce530a7: intactos. Source lock SHA-256
`a1c168c52eae5ca26f4d3148de8012e589062ccc7a808c66f98bafd0911329f1`;
baseline binding `d843ab45537a06764e5edc7ab2b3aa3343862bbd848ef8efe2997c870a035979`.
GOBACK `fa299c2e5f3fae75afe365363b9f16925f0cfea591f631768ace82f0fb9a1075`;
scalar golden `40b9cec1bcc1c1e40cf3b9e3c48e834835e478e1d84bccafa575d63497ef3b60`.
Somente air-java foi escrito por esta tarefa; os irmãos podem ter trabalho
concorrente legítimo, não são resetados/alterados/certificados por este diff.

A pipeline inteira não é size-unbounded: lower mantém budgets próprios de SP,
admissão e identidades; adapter CFG lê byte[] e ainda possui AirInputLimitException
classificada por sua CLI como IMPLEMENTATION_LIMIT; outros transportes/CP5 não foram
remediados nem testados aqui. Neste repo ainda há árvore JSON/model em memória,
limites de buffers/contadores da JVM, recursos físicos finitos, custos por slots/
proof sites em Return de domínio desconhecido e por candidatos/provas. Erros de
representabilidade semântica Java previamente reconhecidos e checks indecididos
continuam explícitos, assim como o subset 1A+4B e o binding DRAFT. Nenhuma nova
capacidade AIR/JSON, E2E, benchmark/SLA, publicação Maven ou merge foi realizado.
Os gates performance e integration permanecem UNAVAILABLE; não são alegados PASS.
