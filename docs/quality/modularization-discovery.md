# Evidência do discovery 0C-D

Data: 2026-09-07. [Veredito](../architecture/decisions/ADR-0002.md),
[desenho 0C-I](../engineering/modularization-gates.md),
[fontes e imports completos](../sources/modularization-baseline.json).
Self-review desta sessão; nenhuma alegação de revisão independente ou aprovação.

## Ambiente, escopo e método

Base air-java `c487b80dd5548236de9114d8b3ec370eabb25c30`; Temurin 25.0.4,
Maven 3.9.16, Python 3 padrão; compilação Java release 21 sem preview.
O full de baseline passou: 54 testes de harness, 308 classes/4664 referências,
172 checks via script e 172 via Maven clean verify, exit 0.

Probes em `/tmp/air-0cd-probes`, derivados de git archive dos SHAs registrados;
nenhum checkout irmão foi editado, nem mesmo para mudar sua branch. Caches Maven
foram copiados para m2 descartável e `io/github/gustavo2358` removido **apenas dessa
cópia**, antes de instalar o candidato. Nenhum JAR AIR pré-existente pode sustentar
a prova B. O install usado nesses probes é só cache local de experimento; nenhum
deploy, publicação remota, tag ou release foi executado. A branch não contém
POM novo, move de src ou dependência de produto.

Inventário feito por git ls-tree/git show dos snapshots e expressão de import
ancorada, por arquivo; wildcard conta um statement. Pacotes de produção também
confirmados por declarações package e classfiles. Nomes plenamente qualificados
em usos não são contados como statements import. Leitura de chamadas confirmou
Publication, AirValidator, ValidationResult e ValidationOptions nos consumidores.
A branch local CFG é distinta de main e tem contagem própria no JSON; os testes
abaixo usam exclusivamente a main remota registrada.

## Falsificações e resultados

| Probe / hipótese | Comando ou oracle | Resultado observado |
| --- | --- | --- |
| Baseline single-module | root mvn clean verify, cache isolado | 172 checks, BUILD SUCCESS |
| B com GAV preservado | root parent distinto, src copiado ao model, exec:java original | RED: NoSuchFileException src/main/java; cwd do processo ainda era aggregator |
| B corrigido em /tmp | exec:exec em test, JVM com cwd do model; root clean install | GREEN: parent + air-java + air-json, 172 checks uma vez, 2 JARs; JSON sem classes |
| Modelo binariamente equivalente | abrir dois JARs e comparar mapa nome → bytes de todas as classes | 308 classfiles idênticos: 283 model, 25 validation. JAR inteiro difere somente em META-INF/maven/io.github.gustavo2358/air-java/pom.xml |
| Effective POM e grafo de B | help:effective-pom + dependency:tree | parent sem deps, air-java sem deps, air-json → air-java compile; sem biblioteca JSON |
| Lower original fora do reactor AIR | clean verify + dependency:tree na cópia de main, usando B instalado | GREEN: LOWER_TESTS=540 e 239; core → air-java; adapters → core → air-java; Jackson próprio somente nos adapters |
| CFG original fora do reactor AIR | mesmo comando na cópia de main | GREEN: 82 testes, 0 falhas/erros/skips; kernel → air-java compile, JUnit test |
| Consumer independente | Java com método Publication → AirValidator → ValidationResult, clean verify | GREEN com POM/GAV antigos e B instalado |
| Parent omitido | retirar temporariamente air-java-parent só do cache do probe, repetir consumer | RED: cannot read artifact descriptor / air-java-parent pom absent, mesmo com JAR de modelo presente |
| Parent restaurado | recolocar o mesmo diretório de cache, repetir clean verify | segundo GREEN |
| Root e child com mesmo GAV | mvn validate em fixture de dois projetos | RED: Project io.github.gustavo2358:air-java:0.1.0-SNAPSHOT is duplicated in the reactor |
| Jackson sem referência em classes | dependência runtime Jackson 2.22.2 somente no POM de cópia model; dependency:tree e architecture.check | árvore mostra databind/core/annotations runtime; gate atual retorna PASS 308/4664. Lacuna real de metadados a fechar em 0C-I; não é prova de isolamento futuro |
| A com coordenadas novas | reactor A clean install em cache separado sem AIR; consumer antigo clean verify | A passa 172 checks, mas consumer falha por air-java:jar:0.1.0-SNAPSHOT ausente; POM aggregator não substitui JAR |
| Rollback de layout/GAV B | root baseline single-module clean install no cache do probe, consumer clean verify | GREEN, 172 checks da baseline e consumer com mesmas coordenadas, inclusive com parent oculto no cache |
| Workflow antigo | teste de ordem novo contra YAML com full antes de ci-scope | RED, exit 1, uma falha específica de ordem entre 12 testes de execução |
| Workflow proposto | mesmo unittest após trocar só os dois steps | GREEN, 12 testes, exit 0 |
| Challenge da ordem | inverter em memória; remover scope; remover full; scope continue-on-error; full if:false | cinco mutações rejeitadas; workflow real permanece ci-scope → full |

A lacuna de dependência runtime não é corrigida nesta sessão, pois exige a política
multi-módulo autorizada apenas em 0C-I. O desenho exige grafo efetivo vazio do modelo
além de bytecode. Contracasos atuais de inversão model/validation, I/O, rede, JSON,
classfile inválido e resultado de suíte ausente continuam exercitados pelo full.

## Receita dos probes (sem modificar checkouts)

1. Criar diretório temporário, exportar `git archive <SHA>` de cada repo para
   baseline/lower/cfg; SHAs completos no baseline JSON. Executar comandos abaixo
   **com cwd no projeto indicado**, não só `mvn -f` de outro diretório: a suíte
   atual possui os scans relativos de fonte. Exportar B a partir da mesma baseline.
2. Somente na cópia B: mudar root artifactId para air-java-parent e packaging pom;
   modules air-model/air-json; mover src para air-model; manter propriedades e
   plugins compiler/jar comuns. Mover execução exec apenas para model. Child model
   tem parent air-java-parent:0.1.0-SNAPSHOT e artifactId air-java. JSON tem mesmo
   parent, artifactId air-json e dependência compile em air-java:${project.version}.
3. Demonstrar a falha da execução original; substituir sua configuração apenas
   na cópia por este lançamento, sem alterar ContractSuite nem Fixtures:

```xml
<execution>
  <id>air-contract-suite</id><phase>test</phase>
  <goals><goal>exec</goal></goals>
  <configuration>
    <executable>${java.home}/bin/java</executable>
    <workingDirectory>${project.basedir}</workingDirectory>
    <classpathScope>test</classpathScope><skip>${skipTests}</skip>
    <arguments>
      <argument>-ea</argument><argument>-classpath</argument><classpath/>
      <argument>io.github.gustavo2358.air.validation.ContractSuite</argument>
    </arguments>
  </configuration>
</execution>
```

4. Com caches de plugins disponíveis e sem artefatos AIR anteriores no cache
   dedicado, executar (prefixo local ilustrado com o diretório usado nesta sessão):

```sh
# cwd: cópia B; instalação estritamente local descartável
mvn -o -B -ntp -Dmaven.repo.local=/tmp/air-0cd-probes/m2 clean install
# cwd: cada cópia lower / cfg, sem alterações de POM ou fontes
mvn -o -B -ntp -Dmaven.repo.local=/tmp/air-0cd-probes/m2 clean verify org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree
# cwd: baseline e depois B; output absoluto diferente por execução
mvn -o -B -ntp -Dmaven.repo.local=/tmp/air-0cd-probes/m2 org.apache.maven.plugins:maven-help-plugin:3.5.1:effective-pom -Doutput=/tmp/air-0cd-probes/B-effective.xml
```

5. Comparar entradas `.class` do JAR baseline e B por nome e bytes (zipfile Python),
   separando metadados Maven. Criar consumer autônomo com dependência air-java,
   compiler 3.13.0/jar 3.4.2, release 21; importar Publication/AirValidator/ValidationResult.
   Repetir clean verify com/sem parent no cache, restaurar e obter segundo GREEN.
6. Criar fixture root/child com mesmo GAV, packaging pom/jar e child declarado em
   modules; mvn validate deve rejeitar duplicata. Para demonstrar lacuna de grafo,
   adicionar Jackson runtime à cópia model e comparar dependency:tree com
   architecture.check dessa cópia. Nunca alterar allowlist/testes para obter PASS.
7. Para rollback: reinstalar a cópia single-module no mesmo cache e repetir consumer.
   Para A: em outra cópia/cache sem AIR anterior, root air-java:pom, child air-model,
   JSON → air-model; verificar que consumidor antigo requer JAR air-java ausente.

O primeiro help:effective-pom precisou baixar plugin oficial em cache /tmp após
restrição de rede local; sucesso confirmado depois. Um primeiro consumer sem jar
plugin fixo encontrou dependências de plugin ausentes no modo offline; fixar
jar 3.4.2 alinhou o ambiente. Essas falhas de ambiente não são evidência contra B.

## Verificação da entrega e limites

O diff executável é a ordem de dois steps e dois testes de regressão em
scripts/harness/tests/test_execution.py. Executor ci-scope, full, permissões
contents:read e CI anterior permanecem byte a byte intactos. O teste não é parser
YAML geral: protege a forma explícita dos dois steps do workflow atual. O full
continua obrigatório após scope; falha de um step impede sucesso do job.

Full final passou com docs, 56 testes harness, 308 classes/4664 referências,
172 checks offline e 172 via Maven clean verify (exit 0). Git e scope explícitos
também passaram. MANIFEST e staged diff são conferidos antes do commit.
Estado remoto é registrado no PR após publicação. Não usar
este relatório ou o SHA base como recibo de CI do futuro commit. O PR deve conter
SHA remoto, runs/checks e ordem dos steps desse head. Não há aprovação humana,
merge ou publicação implícitos em um PASS local.

Não foram executados harnesses completos de lower/CFG (proveniência deles ainda
pina o SHA antigo), branch local CFG, frontend/E2E, round-trip ou matriz 0B.
Foram executadas suas suítes Maven em snapshots temporários como testes de
compatibilidade. Não há verificação remota de release Maven nem reactor C completo.
As regras futuras listadas no desenho são critérios de aceite de 0C-I, não gates
entregues. A implementação atual e source lock permanecem intactos.
