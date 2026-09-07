# Desenho dos gates para 0C-I

**Projeto executável recomendado, ainda não implementado.**
Decisão: [ADR-0002](../architecture/decisions/ADR-0002.md).
Hoje os executores continuam single-module; este documento não declara proteção
multi-módulo entregue. Nenhuma biblioteca arquitetural externa deve ser adicionada.

## Premissas atuais que quebram

| Superfície atual | Quebra / adaptação necessária em 0C-I |
| --- | --- |
| scripts/harness/architecture.py | root/src/main/java único; prefixes model/validation únicos; compila sem classpath. Mover política para owners por módulo e inspecionar todos os módulos conhecidos; não apenas trocar um path |
| scripts/check.sh | dois source roots e um JAR hardcoded; mesmo classpath/cwd para suíte; jdeps global exige java.base. Compilar módulos em ordem, suites por owner, artefatos por módulo, jdeps específico |
| ContractSuite | dois scans de src/main/java relativos ao processo (linhas 1024 e 1087 na baseline). Executar no cwd air-model; não apontar scans para o root vazio ou para JSON |
| pom.xml / mvn verify | exec:java em test assume processo no root single-module; plugin no parent seria herdado por JSON. Exec somente no modelo, fork com workingDirectory explícito; verificar bytecode em verify |
| contracts.py / contract-checks.json | único fluxo de nomes, índices e resumo de 172 checks. Preservar esse inventário como model, sem regenerá-lo da saída; adicionar namespace/inventário JSON somente com suíte real em 1A |
| MANIFEST.sha256 | caminhos src e POM/JAR docs atuais. Remover paths movidos, adicionar novos POMs/src/docs; preservar hashes de fontes movidas; manifesto não prova semântica |
| docs.py / work / links | autorização discovery proíbe src/POM; próximo item implementation delimita ambos os lados dos moves; corrigir links e must_read de documentos atuais |
| CI / full | CI deve chamar reactor inteiro, sem -N/-pl parcial e sem path filters. Full continua docs/harness/architecture/semantic/maven; ci-scope precede full; maven clean pode apagar recibos anteriores |

## Política de módulos e direção

Um mapa versionado pequeno deve declarar exatamente estes owners em 0C-I, e ser
comparado à lista real de modules do root e a todo source root de produção no repo.
A ausência de fontes em air-json é permitida explicitamente só no estado vazio
de 0C-I; não pode fazer o módulo/POM desaparecer do inventário.
Módulo/source root não mapeado é FAIL, não descoberta ignorada. Root tem packaging
pom, sem src/main/test/classes de produto e GAV diferente dos filhos.

| Owner físico / GAV | Classes próprias | Dependências de produção |
| --- | --- | --- |
| air-model / air-java | prefixes model e validation atuais | nenhum artefato compile/runtime; JDK somente pela allowlist atual |
| air-json / air-json | prefix futuro `io.github.gustavo2358.air.json.`; sem classes em 0C-I | exatamente air-java compile na mesma versão; libs JSON somente após decisão e alteração explícita em 1A |

Model não conhece validation mesmo compartilhando JAR. Validation conhece model.
Nenhum dos dois conhece JSON, arquivos, rede, CLI, frontend, CFG ou frameworks.
JDK permitido no modelo continua `java.lang`, `java.lang.invoke`,
`java.lang.runtime`, `java.math`, `java.util`, `java.util.function`, `java.util.stream`,
apenas `java.base`, excetuando System/Runtime/Process/ProcessBuilder/ServiceLoader.
`java.io.*`, `java.nio.*`, `java.net.*`, reflexão explícita e libs externas seguem
rejeitados por ausência da allowlist. Record/lambda support permanece aceito.

JSON pode referir modelo e validação pública do JAR e seu próprio prefix. Pode
usar a biblioteca escolhida em 1A e JDK de transporte explicitamente revisado;
não liberar globalmente `com.*`, `org.*` ou todo `java.base`. Em 0C-I, sem código
JSON, não conceder antecipadamente filesystem/rede. 1A decidirá classes necessárias
(ex.: Reader/Writer ou bytes), mantendo rede/CLI/frontend fora do codec sem motivo
concreto. Bibliotecas transitivas da lib escolhida entram numa lista explícita do
JSON, jamais numa allowlist compartilhada com o modelo.

## Execução implementável sem recursão de gates

Manter funções de inspeção puras de classfiles/dependências e dois modos de entrada
no mesmo script de arquitetura (nomes de argumentos abaixo são contrato proposto):

1. **Standalone, chamado por run.py architecture e check.sh:** parsear topologia e
   política; rejeitar POMs/profiles não cobertos; criar TemporaryDirectory; compilar
   model com javac release 21 e classpath de produção **vazio**; compilar JSON
   somente com model e dependências JSON autorizadas. Em 0C-I só há model no CP.
   Enumerar classes por owner e inspecionar jdeps de cada saída, com o CP daquele
   owner. Não usar target de builds anteriores nem um CP misturando testes/JSON.
   Para manter o modo offline em 0C-I, validar a forma literal fechada dos três
   POMs locais (sem parent externo/profiles/dependencies herdadas); derivar o CP
   dessa forma permitida. O modo compiled reconfirma pelo modelo efetivo Maven.
2. **Compiled, chamado pelo Maven verify de cada módulo JAR:**
   `architecture.py --module air-model --classes <outputDirectory> --dependency-tree <json> --effective-pom <xml>`
   (equivalente para air-json). Sem chamar mvn, full, semantic ou arquitetura
   standalone de dentro de verify. Validar topologia/POM e inspecionar exatamente
   os classfiles e a árvore real produzidos nesse build; paths absolutos, owner
   esperado, arquivos obrigatórios, sem fallback para artefato de outro módulo.
3. Gerar effective-pom e dependency:tree JSON de cada módulo antes da inspeção
   no verify, via plugins oficiais Maven fixados e execuções declaradas em ordem.
   Os plugins help 3.5.1 (effective-pom) e dependency 3.8.1 (tree textual)
   foram exercitados como ferramentas de descoberta. Em 0C-I usar tree com
   `-DoutputType=json` e validar sua integração/parse antes de ligar o gate. Esses são plugins de build, não dependências do modelo. Não executar
   suíte pelo parent ou herdar scanner em projeto pom. Verificar no contracaso que
   -pl air-model -am também executa bytecode desse módulo.
4. Model: exigir dependencies efetivas compile/runtime vazias e conferir CP
   resolvido vazio, inclusive dependência runtime/optional sem referências Java.
   Parent não pode injetar dependencies herdadas. dependencyManagement sozinho não
   acrescenta dependência; uma declaração efetiva sim. Dependência test permanece
   separada e não pode entrar no CP usado para inspecionar/empacotar produção.
5. JSON: exigir aresta direta compile para GAV modelo exato; inspecionar fechamento
   transitivo e CP real por allowlist própria. Validar ausência de cycles/shading,
   classes AIR duplicadas no JAR JSON e qualquer caminho model → JSON/lib JSON.
   Nunca --ignore-missing-deps; `not found`, saída jdeps vazia inesperada e classfile
   não reconhecido falham. Class major 65, minor 0 em toda classe de produto.

Pseudoalgoritmo, a converter em código/testes no próximo checkpoint:

```text
assert declared_modules == policy.modules
assert discovered_production_owners <= policy.modules
assert every_production_source_root_has_exactly_one_owner
assert empty_modules == policy.authorized_empty_modules  # somente JSON em 0C-I
assert root.packaging == pom and root.GAV not in child.GAVs
for module in topological_order:
    effective = read_effective_model(module)
    graph = read_resolved_compile_and_runtime_graph(module)
    assert exact_allowed_graph(module, effective, graph)
    classes = clean_compile(module, classpath=graph.for_module)  # standalone
    # compiled mode recebe a saída Maven deste módulo, sem recompilar
    assert all_classfiles_owned_by(module, classes)
    assert class_major_minor(classes) == (65, 0)
    inspect_jdeps_per_source_owner(classes, classpath=graph.for_module)
    assert jar_class_inventory == compiled_class_inventory
```

Allowlist usa **origem e destino**, não só nome de classe. A referência ao model
precisa resolver no output/JAR de air-model, nunca numa cópia dentro de JSON ou
numa lib transitiva homônima. Model compila sozinho sem JSON instalado: essa prova
independente impede um classpath amplo de esconder direção inversa.

## check.sh, Maven/full e módulo vazio

Em 0C-I preservar a capacidade offline do script: não há libs JSON para resolver.
Ele conhece ambos os POMs/owners, compila model/testes com warnings como erros,
roda ContractSuite num subshell com cwd air-model, produz
`air-model/target/air-java-0.1.0-SNAPSHOT.jar` e
`air-json/target/air-json-0.1.0-SNAPSHOT.jar`. O segundo pode estar vazio apenas pela
política explícita do checkpoint. Não produzir uma segunda cópia compatível no
root target: atualizar exemplos/docs e exigir caminhos reais evita JAR obsoleto.

A política do JSON vazio exige POM presente, packaging jar, dependência correta,
zero classes e zero inventário de testes do transporte; não basta ignorar diretório
ausente. A primeira classe de 1A deve tornar classes/suíte não vazias obrigatórias.
A configuração de 1A também resolverá o CP de bibliotecas JSON explicitamente;
check.sh não pode permanecer testando só model quando existir codec real. Sem CP
resolvido, falhar com erro de ambiente, não omitir o módulo. Atualizar então o
contrato de offline, em vez de baixar bibliotecas silenciosamente pelo script.

Maven root clean verify percorre parent/model/json; scanner compiled em cada JAR
verifica artifacts efetivos. Full ainda roda architecture standalone, semantic
(script) e maven, cada gate uma vez; a repetição de inspeção no verify protege o
build Maven independente e não é uma chamada recursiva ao full. Não aceitar
BUILD SUCCESS/Surefire vazio sem os 172 registros model completos e um resumo.
O runner deve detectar reactor incompleto/owner ausente e suíte repetida no JSON.

MANIFEST deve cobrir cada novo POM/arquivo versionado e cada move, sem hashes de
target, classfiles, caches ou de si próprio. Conferir `sha256sum -c MANIFEST.sha256`
e cobertura dos paths Git em 0C-I; docs/gate atual não verifica esse manifesto.
Maven clean remove target/harness/ci-scope.json após scope passar; isso já decorre
do clean. Na ordem nova, logs/steps remotos provam o scope anterior. Não mudar
permissões ou clean só para conservar esse arquivo; recibo full é gravado depois.

## Oráculos obrigatórios de 0C-I

| Entrada / challenge | Resultado exigido |
| --- | --- |
| model + validation atuais, records/lambdas | GREEN; 172 checks; modelo compila sem JSON |
| validation → model / JSON → modelo | GREEN com owner/CP correto |
| model → validation, model/validation → JSON | RED pelo owner/direção ou compile isolado, incluindo nested |
| model → Jackson/Gson/terceira lib sintética | RED mesmo que JSON tenha essa biblioteca permitida |
| Files/Path/streams/rede no model | RED pela allowlist; não basta java.base |
| Jackson runtime/optional herdado, sem uso no bytecode | RED no grafo/POM; probe desta sessão revelou que architecture atual passa |
| terceiro módulo, classe em root, classfile fora de prefix ou módulo ausente | RED por inventário/owner, não silêncio |
| model copiado para JSON / dependência não resolvida / cycle | RED pelo owner/grafo; jamais ignore-missing-deps |
| apagar saída classfiles de model | RED, ainda que target antigo/JAR de cache exista |
| JSON vazio autorizado em 0C-I | GREEN somente com POM/aresta presentes; não declarar transporte válido |
| primeira classe JSON sem atualizar política/testes em 1A | RED; vazio não vira licença permanente para omitir testes |
| ContractSuite omitida/duplicada/reordenada ou skipTests no perfil full | RED no inventário; não aceitar surefire vazio |
| exec herdado em parent/JSON ou cwd incorreto | RED no lançamento/inventário; fork correto recupera GREEN |
| scope fora do manifesto | ci-scope RED, full não iniciado; full falho também bloqueia CI |

Esses contracasos do estado futuro são requisitos, não resultados executados aqui.
[Probes reais](../quality/modularization-discovery.md) distinguem o que já foi
falsificado. Limites continuam: jdeps não prova reflexão dinâmica, semântica AIR,
verdade do produtor ou performance. Review de código e oráculos de domínio continuam.
