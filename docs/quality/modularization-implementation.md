# Evidência 0C-I — implementação da Opção B

[Work item](../work/active/WORK-AIR-MODULARIZATION-002/work-item.json),
[ADR-0002](../architecture/decisions/ADR-0002.md),
[gates](../engineering/modularization-gates.md).
Self-review desta sessão; não é review independente ou aprovação humana.

## Entrada e baselines reconfirmadas

Main air-java limpa após fetch, checkout main e pull --ff-only:
`59120faebfe8381df445fd6ea74822bb83f08554`, merge real do
[PR #3 / 0C-D](https://github.com/Gustavo2358/air-java/pull/3), GitHub MERGED em
2026-09-07T16:44:20Z. Branch `feat/air-java-modularization` criada dessa base.
Lifecycle anterior arquivado, novo item implementation antes de alterar produto.
Nenhum SHA de merge ou review humano foi inferido de documento local.

| Repo consultado somente em leitura | Main remota usada | Confirmação |
| --- | --- | --- |
| analysis-ir | 51b4d9a8ae0364232bd97103cd73a77e1a34996c | [0B PR #3](https://github.com/Gustavo2358/analysis-ir/pull/3), MERGED 2026-09-07T16:22:16Z |
| analysis-cfg | 141b8270f54558a24ee561281598e53c48a0ff6b | [0A PR #8](https://github.com/Gustavo2358/analysis-cfg/pull/8), MERGED 2026-09-07T16:30:43Z |
| cobol-lower | e2488a362478057de7d59cdf9ae2b38b1f4040d3 | main confirmada por ls-remote e exportada por SHA |
| proleap-poc | c8a891e0827ae1dc1140246f625fd16c2ac9bd97 | main confirmada por ls-remote e exportada por SHA; sem build |

0B mantém o pin `122ce54e1b9ef9b00646f93ece409ca8b63bc933`. Dos 15 arquivos
referidos no lock local, 14 permanecem byte a byte iguais; README apenas ganhou
links/descrição informativa do 0B. Norma e binding não mudaram. O source lock local
foi preservado. 0A acrescentou política/default e testes CFG, conservando GAV/imports
AIR. Esses merges não invalidam B e não exigem discovery amplo ou mudança de domínio.

Ambiente: Temurin 25.0.4, Maven 3.9.16, compilação release 21, sem preview;
Python padrão. CI usa Temurin 21 e fornece evidência distinta no PR do head.
Roadmap externo consultado, não alterado; seu registro de merges estava desatualizado,
por isso o estado acima veio de Git/GitHub reais.

## Produto e oracle de compatibilidade

Root `io.github.gustavo2358:air-java-parent:0.1.0-SNAPSHOT:pom` agrega:

- `air-model`: `io.github.gustavo2358:air-java:0.1.0-SNAPSHOT:jar`, model + validation;
- `air-json`: `io.github.gustavo2358:air-json:0.1.0-SNAPSHOT:jar`, vazio, dependência
  direta compile em air-java na versão conjunta. Nenhuma biblioteca JSON.

Os 42 arquivos main/test foram movidos byte a byte. Packages públicos
`io.github.gustavo2358.air.model` e `io.github.gustavo2358.air.validation`, APIs,
semântica e inventário de contratos foram preservados. Validation não virou módulo.

Baseline exportada por git archive do SHA de entrada, compilada antes dos moves
com `mvn -o -B -ntp -Dmaven.repo.local=/tmp/air-java-harness-m2 clean verify`:
172 contratos, um resumo, exit 0. Inventário real capturado: 308 classfiles,
283 model e 25 validation. Esse número é resultado, não constante do gate.

[Inventário completo](modularization-classfiles.json) contém nome, SHA-256 e
major/minor de cada classe, hashes da API pública (`javap -public -s`) e de jdeps.
[Comparador reproduzível](../../scripts/compare_classfiles.py) constatou:
308/308 nomes/bytes idênticos, major 65/minor 0, mesmos packages/API/referências
(4664 arestas). Única entrada alterada no JAR Maven: POM em META-INF/maven.

```sh
python3 scripts/compare_classfiles.py \
  --baseline-jar /tmp/air-0ci/baseline/target/air-java-0.1.0-SNAPSHOT.jar \
  --candidate-jar air-model/target/air-java-0.1.0-SNAPSHOT.jar \
  --baseline-commit 59120faebfe8381df445fd6ea74822bb83f08554 \
  --output /tmp/air-0ci/comparison.json
```

ContractSuite antes: exec:java single-module. Depois: exec:exec apenas no model,
JVM `-ea`, CP de testes, cwd `${project.basedir}`. Mesmos 172 checks, exatamente
uma vez por build; os scans relativos continuam intactos. Verify inspeciona o log
nominal antes de apresentá-lo. Check.sh faz o mesmo com build temporário offline.

## Falsificação e resultados

Testes de topologia introduzidos antes do reactor falharam pela ausência de módulos
/inspector no estado antigo; após implementação passaram. As regressões usam
cópias temporárias, não adulteram o checkout real. O conjunto cobre GREEN atual,
validation → model, JSON → modelo vazio; RED de inversão, model → Jackson/Gson,
filesystem/rede/reflexão/processos, terceiro/missing módulo, root source/class,
model copiado para JSON, primeira fonte/recurso/teste JSON, ciclo, classfile inesperado,
JAR stale, suíte e reactor omitidos/duplicados/reordenados/incompletos e skip.

**Contracaso Maven obrigatório executado de verdade:** cópia em
`/tmp/air-0ci/runtime-leak`, Jackson databind 2.22.2 runtime somente no POM do model.
Help 3.5.1 e dependency 3.8.1 geraram effective POM e árvore JSON com Jackson/core/
annotations runtime. Bytecode original continuou GREEN (4664 referências).
Separadamente: POM literal RED, effective POM RED, grafo resolvido RED. Restaurar
só o POM local também continuou RED no scanner compiled, graças à evidência Maven
com a dependência runtime. Portanto não é rejeição baseada apenas em source grep.
O challenge inicial `mvn -DskipTests=true verify` revelou outro falso GREEN:
effective POM conserva a propriedade declarada false, mas interpola true no launcher;
um log anterior mascarava a omissão. Corrigido com inspeção da configuração efetiva
e flags Maven resolvidos enviados ao scanner não ignorável. Regressão permanente
reproduz propriedade false/launcher true. Os quatro flags skipTests, maven.test.skip,
maven.main.skip e exec.skip foram então recusados em execuções Maven reais, mesmo
com outputs anteriores. A restauração clean verify recuperou GREEN, 172 contratos
uma vez e três owners.

O scanner standalone nunca chama Maven; compiled nunca chama full, semantic,
standalone ou Maven. A topologia obriga os dois módulos mesmo quando JSON não tem
classes. O build seletivo permite parent/model e não alega construir JSON.

## Consumers atuais em cache isolado

Snapshots vieram dos SHAs remotos acima, exportados em `/tmp/air-0ci`; nenhum
fetch/checkout/branch/commit ou edição foi feito nos quatro repos reais. Cache
`/tmp/air-0ci/m2` copiado apenas para reaproveitar plugins/dependências já baixados;
`io/github/gustavo2358` removido dessa cópia antes de instalar o reactor novo.
Nenhum AIR antigo permaneceu para mascarar resolução. Instalação só nesse cache
local descartável, sem deploy/release remoto.

```sh
# cwd air-java; depois cwd de cada snapshot consumer, sem -f com cwd incorreto
mvn -o -B -ntp -Dmaven.repo.local=/tmp/air-0ci/m2 clean install
mvn -o -B -ntp -Dmaven.repo.local=/tmp/air-0ci/m2 clean verify \
  org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree
```

| Probe | Resultado real |
| --- | --- |
| Reactor novo install | parent/model/json SUCCESS; 172 contratos, 2 JARs; exit 0 |
| cobol-lower main | 540 + 239 checks; core → air-java compile; adapters → core → air-java; exit 0 |
| analysis-cfg main após 0A | 102 testes, 0 falhas/erros/skips; kernel → air-java compile; exit 0 |
| Integridade consumers exportados | 31 arquivos POM/Java lower e 28 CFG comparados ao tar remoto, todos intactos |

Jackson dos adapters lower pertence ao leitor Semantic Product já existente;
não é dependência do modelo AIR nem código novo deste checkpoint. Nenhum consumer
precisou alterar GAV ou imports. Harnesses completos downstream não foram alegados:
eles governam proveniência/pins próprios e não são o oracle Maven/API deste probe.

## Handoff de adoção downstream

| Consumer | Arquivos | Pin atual | Novo pin esperado / motivo |
| --- | --- | --- | --- |
| cobol-lower | docs/sources/sources.lock.json | 6a4091e5394fc22b3d2ada9abbdb530eb3572a58 | SHA aprovado de 0C-I registrado no PR; adotar reactor/parent e paths air-model/src, renovar evidência de cache |
| analysis-cfg | docs/sources/sources.lock.json; .github/workflows/ci.yml (checkout e comparação) | 6a4091e5394fc22b3d2ada9abbdb530eb3572a58 | mesmo SHA de 0C-I, ou merge futuro confirmado; atualizar pin/paths/proveniência em work item próprio |

O recibo do head exato fica no PR, pois não se grava o SHA futuro dentro do próprio
commit. Pins/links históricos continuam referindo a fonte antiga corretamente;
a adoção downstream ainda não ocorreu. Não houve falha de API/Maven ou necessidade
de alteração de código consumer. Publicar/instalar o modelo isolado sem seu parent
continua insuficiente; root install instala o conjunto necessário.

## Verificação de entrega, riscos e rollback

Root clean verify e seleção `-pl air-model -am verify`: exit 0. Script offline
invocado de /tmp: 172 contratos, 2 JARs, exit 0. Full: docs/MANIFEST (126 paths),
80 testes harness, architecture 308 classes/4664 arestas, semantic 172 e Maven 172,
exit 0. Git e scope explícitos: 130 paths autorizados, exit 0. MANIFEST preserva
os 42 hashes movidos; sha256sum e cobertura integral conferidos. Exemplo público
executado com o novo path do JAR. Self-review integral e staged antes de commit;
recibo final de commit/push/CI/PR permanece no PR.
Logs locais em `/tmp/air-0ci` e target/harness; não são aprovação humana ou recibo
remoto. CI do SHA pushed será conferida no GitHub e registrada no PR, sem commit
apenas para autoinscrever CI.

Riscos/limites: SNAPSHOT exige pin/cache para provar proveniência; parent necessário;
JDK local difere do CI, ambos release 21; forma POM suportada deliberadamente fechada;
jdeps não prova reflexão dinâmica nem semântica. Sem transporte/performance/E2E.
Rollback: reverter o conjunto de modularização para o layout single-module/GAV
preservado em trabalho autorizado, repetir full e probes e reconciliar apenas
pins já adotados em trabalhos downstream. Não descartar mudanças alheias.

**air-json não contém codec. 1A não foi iniciado. Nenhum merge/auto-merge executado.**
