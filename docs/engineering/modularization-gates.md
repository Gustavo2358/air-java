# Gates do reactor — política 0C-I

Implementação da Opção B aprovada no [ADR-0002](../architecture/decisions/ADR-0002.md).
[Discovery e probes anteriores](../quality/modularization-discovery.md) registram
as falhas que motivaram o desenho; [evidência 0C-I](../quality/modularization-implementation.md)
registra os resultados atuais. Não há biblioteca arquitetural externa.

## Ownership e dependências

| Owner físico | Artefato / classes | Grafo autorizado em 0C-I |
| --- | --- | --- |
| root | air-java-parent:pom; sem produto | nenhuma dependência herdada; modules exatamente air-model, air-json |
| air-model | air-java:jar; packages model + validation | zero dependências Maven, inclusive runtime/optional/test; somente JDK pela allowlist |
| air-json | air-json:jar; vazio explícito | uma aresta direta compile para air-java na versão conjunta; fechamento sem outras dependências |

Todos usam groupId `io.github.gustavo2358` e versão conjunta `0.1.0-SNAPSHOT`.
`air-model` é diretório, **air-java é o artifactId preservado**. Validation fica
no mesmo JAR: validation → model é permitido; model → validation é proibido.
Model/validation → air-json, bibliotecas JSON ou infraestrutura são proibidos.

[Política versionada](../../scripts/harness/module_policy.py) compara os três POMs
literais com a forma fechada autorizada: parent local, módulos, propriedades,
plugins fixados e execuções explícitas. Não aceita profiles, parent externo,
source roots customizados, extensões `.mvn`, dependências de plugins ou plugins
não cobertos. Não há dependencyManagement porque nenhuma dependência precisa dele
neste checkpoint. Uma futura necessidade exige alteração explícita da política.
O fechamento local permite o gate offline sem invocar Maven ou baixar artefatos.

O inventário físico percorre o repo, inclusive arquivos novos: POM adicional,
source/resource sem owner, fonte/classes de produto no root, classfile fora do
prefix/sem fonte correspondente e JAR stale falham. Classes aninhadas participam
da inspeção; JAR e saída compilada devem ter o mesmo inventário e bytes. Diretórios
target dos módulos são saídas de build, nunca fontes ou entradas do MANIFEST.

Em `air-json`, **qualquer fonte, recurso, teste ou classfile invalida o estado
vazio de 0C-I**. Módulo/POM/aresta ausentes também falham. O primeiro código de 1A
exigirá nova política de ownership/classpath, biblioteca explícita e suíte nominal
própria; não existe allowlist antecipada de JSON, I/O ou rede nem zero testes
apresentado como conformidade de transporte.

## Bytecode do modelo

[architecture.py](../../scripts/harness/architecture.py) verifica major 65/minor 0,
classes próprias e referências diretas de `jdeps -verbose:class -filter:none`.
Saída vazia, `not found` ou classe não reconhecida falham; nunca ignore-missing-deps.
O modelo compila isolado com classpath explicitamente vazio, sem air-json instalado.

JDK permitido: `java.lang`, `java.lang.invoke`, `java.lang.runtime`, `java.math`,
`java.util`, `java.util.function`, `java.util.stream`, somente de `java.base`.
Excluídos: System, Runtime, Process, ProcessBuilder, ServiceLoader. `java.io`,
`java.nio`, `java.net`, reflexão explícita, Jackson/Gson/frameworks ficam fora da
allowlist. Suporte gerado a records/lambdas permanece permitido.

## Entradas sem recursão

1. `run.py architecture`: topologia/POMs locais, compilação temporária do modelo
   com JDK release 21 e CP vazio, ownership e jdeps. Não usa outputs anteriores.
2. `scripts/check.sh`: valida os mesmos owners e POMs, compila modelo e testes
   temporariamente, executa JVM `java -ea` no cwd air-model, compara inventário
   nominal e gera/verifica ambos os JARs nos targets dos módulos. Funciona de
   outro cwd e offline, sem Maven/download. Resíduo JAR/classes no root é falha
   explícita; `mvn clean` remove outputs de um checkout single-module anterior.
3. Root Maven `validate`: inspeção focalizada `--topology`, não herdada. Modelo
   executa a ContractSuite em `test` via exec:exec, JVM separada, cwd
   `${project.basedir}`, classpathScope test, `-ea`. Nenhuma edição da suíte para
   contornar seus dois scans relativos de src/main/java.
4. `verify` de cada JAR: help 3.5.1 gera effective-pom.xml; dependency 3.8.1 gera
   dependency-tree.json (sem filtro que oculte runtime); exec 3.5.0 chama apenas
   a inspeção compiled desse owner. Compiler 3.13.0/jar 3.4.2 são gerenciados no
   parent. Não há chamada de Maven, full ou compilação dentro do scanner compiled.

```sh
python3 -B scripts/harness/architecture.py --module air-model \
  --classes "$PWD/air-model/target/classes" \
  --dependency-tree "$PWD/air-model/target/dependency-tree.json" \
  --effective-pom "$PWD/air-model/target/effective-pom.xml"
```

Modo compiled exige paths absolutos desse módulo; sem fallback para cache/owner
vizinho. Valida GAV, dependencies efetivas, árvore resolvida (incluindo fechamento),
Java release, paths efetivos, skips, classfiles e JAR. A ausência de classfiles do
modelo falha, mesmo com JAR antigo presente. O JSON vazio pode não ter diretório
target/classes, mas seu JAR/POM/grafo são obrigatórios.

## Suíte, reactor e manifesto

A JVM grava `air-model/target/contract-suite.log`. Verify valida e apresenta esse
log somente após conferir [contract-checks.json](../evals/contract-checks.json):
mesmos 172 nomes, ordem, números e um resumo. Suite ausente/duplicada/reordenada/
incompleta, skip ou execução herdada falham. O inventário não deriva do resultado
sob teste. Arquivo de log antigo não torna skip permitido: flags resolvidos
skipTests/maven.test.skip/maven.main.skip/exec.skip são enviados por argumento aos
scanners, que têm skip=false literal. Todos devem ser false; isso impede omissão
mesmo por CLI. A configuração efetiva da suíte também é validada: a propriedade
no effective POM pode conservar false enquanto o launcher interpola true.

`mvn clean verify` percorre parent → modelo → JSON. `run.py maven` também exige
os marcadores dos três owners exatamente uma vez/em ordem e ambos os JARs,
além da suíte e BUILD SUCCESS. `mvn -pl air-model -am verify` é seleção coerente:
valida a topologia completa e inspeciona o modelo, sem alegar build do JSON.
`full` continua docs + harness + architecture + semantic + maven uma vez cada;
a inspeção compiled repetida no Maven não é recursão de gates.

O step ci-scope permanece antes de full. `git --work <ID>` e scope são explícitos.
O gate docs do runner acrescenta [MANIFEST](../../scripts/harness/manifest.py):
sha256 de todos os paths Git presentes, incluindo untracked não ignorados; sem
paths antigos dos moves, target/caches/classes/JARs ou manifesto de si próprio.
`sha256sum -c MANIFEST.sha256` confirma também cada hash por ferramenta externa.

## Contracasos permanentes e limites

[Testes de módulos](../../scripts/harness/tests/test_modules.py),
[bytecode](../../scripts/harness/tests/test_architecture.py) e
[execução](../../scripts/harness/tests/test_execution.py) desafiam:

- model/validation e validation → model GREEN; inversão, I/O, rede, reflexão,
  processos e JSON RED; records/lambdas GREEN;
- JSON → modelo vazio GREEN; aresta ausente/incorreta, inversão e ciclo RED;
- runtime Jackson/optional no POM ou grafo, inclusive sem bytecode reference RED;
- terceiro/missing módulo, fontes no root, cópia de modelo no JSON, primeira
  fonte/recurso/teste JSON, classfile/JAR inesperado ou ausente RED;
- suíte ou reactor omitido/duplicado/reordenado/incompleto/skip RED;
- metadata efetiva com paths de outro owner e JAR com bytes stale RED.

O [probe real](../quality/modularization-implementation.md) usa Jackson runtime
resolvido pelo Maven, além das fixtures de grafo. Classfiles/API são comparados
com o inventário capturado da baseline, sem hardcode de 308 como regra futura.
Limites: jdeps não prova acesso dinâmico por Class/method handles, verdade do
produtor, semântica AIR ou performance. A forma Maven fechada limita configuração
suportada, não constitui sandbox contra código de build arbitrário. Review humano
e oráculos de domínio continuam necessários. Transporte/integration/performance
permanecem UNAVAILABLE.
