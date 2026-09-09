# air-java

Modelo Java imutável e Validator estrutural para **Analysis IR 2.0.0**.
A autoridade semântica é `Gustavo2358/analysis-ir`, com baseline normativo fixado em
`122ce54e1b9ef9b00646f93ece409ca8b63bc933` por
`docs/sources.lock.json`. Este repositório implementa a AIR; não a redefine.

```text
analysis-ir (autoridade normativa)
        ↓
air-java (implementação Java)
        ↓
produtores e consumidores
```

**JDK:** 21 ou superior, compilado com `--release 21`.
**Python:** 3.10 ou superior, disponível como `python3`, para build e verificação.
**Coordenadas:** `io.github.gustavo2358:air-java:0.1.0-SNAPSHOT`.
A versão da biblioteca é independente da versão semântica da AIR.

## Layout Maven

| Diretório | GAV (versão conjunta 0.1.0-SNAPSHOT) | Conteúdo |
| --- | --- | --- |
| raiz | `io.github.gustavo2358:air-java-parent:pom` | parent/aggregator, sem dependências herdadas |
| `air-model` | `io.github.gustavo2358:air-java:jar` | modelo + validation, packages públicos preservados |
| `air-json` | `io.github.gustavo2358:air-json:jar` | codec AIR JSON 1A; dependência compile direta em air-java |

`air-model` é o diretório físico; **`air-java` permanece o artifactId consumido**.
A direção é `air-json → air-java`, nunca o inverso. Para consumo via cache Maven,
instale o reactor completo (`mvn install`): o POM do parent também é necessário.
0C-I foi mergeado no PR #4; 1A acrescenta o codec sem dependência JSON externa.

## Fronteira

`air-model/src/main` depende somente de `java.base`. Não contém Jackson, Gson, JSON,
filesystem, rede, CLI, frontend COBOL, CFG, cálculo de effects, reaching
definitions ou possible values. Nenhum campo semântico é um payload livre como
`Map<String,Object>`.

O modelo é transport-independent. `analysis-ir/bindings/json-v1.md` é uma
especificação de transporte **DRAFT**. O módulo irmão `air-json` implementa as formas
GOBACK do 0B no pin acima, com API `new AirJson().encode(Publication)` e
`decode(byte[])`, escrita canônica e falhas tipadas. [Cobertura, API e limites](docs/engineering/air-json.md).
Formas ainda não implementadas falham explicitamente. Sem integração lower/CFG ou E2E.

## Conteúdo

- `model`: identidades tipadas, publicação, unidades, entradas, sequências,
  operações, ocorrências de operandos, tipos/valores, memória, interações,
  proveniência, coverage, uncertainties e premissas normativas;
- `validation`: índices por identidade, checks de fechamento/ownership/tipos,
  prova finita de `sameDomain` e diagnósticos explícitos de limites/obrigações;
- `air-model/src/test`: suíte determinística sem framework externo;
- `air-json/src`: codec explícito e suíte com golden manual, preservação e negativos;
- `examples/MinimalPublication.java`: construção e validação sem transporte;
- `docs/reconciliation-air-2.md`: discovery AIR ↔ Java e evidência da migração;
- `docs/model-catalog.md`: catálogo informativo da API Java atual;
- `docs/implementation-status.md`: cobertura e limites efetivamente implementados.

## Mudanças normativas desta reconciliação

A API acompanha a AIR normativa, mesmo quando isso quebra a API Java anterior:

- não existem `Publication.contracts`, `ContractId` ou entidade `Contract`;
- `ContractRef` é valor de autoridade/versão/evidência, e a assinatura externa
  é materializada em cada `invoke`;
- targets executáveis são somente internal, literal e computed; `ResourceId`
  continua identidade declarativa;
- `disjoint_storage` contém bases distintas e vale universalmente, sem `FactScope`;
- não existem `SafetyAssertion`, `SafetyProperty` ou certificados privados em
  slices, ranges e choices;
- parâmetros e resultados possuem inventários/restantes independentes;
- `InvocationOutcomes` e `ControlEnvelope` são tipos distintos;
- `return` não possui seletor de entradas;
- relações usam `ArtifactRelationId`, subjects externos pertencem ao site e
  ocorrências exclusivas de effects continuam materializadas por `OperandId`;
- naturais AIR usam `BigInteger` quando não possuem teto semântico;
- localização preserva linha/coluna **ou** offsets com unidade explícita.

Não há aliases/deprecated wrappers para a semântica removida.

## Compilar e testar

Python é pré-requisito tanto de `./scripts/check.sh` quanto do build Maven:
o reactor executa `python3` desde a fase `validate`, inclusive em
`mvn package`, `mvn verify` e `mvn install`. Os dois workflows de CI configuram
explicitamente Python 3.12.

Gate offline, sem dependências de teste:

```sh
./scripts/check.sh
java -cp air-model/target/air-java-0.1.0-SNAPSHOT.jar examples/MinimalPublication.java
```

O script conhece os dois módulos, compila modelo e testes com warnings como erros,
executa a suíte com cwd em `air-model` e valida seu inventário nominal. Produz os
dois JARs em seus próprios `target/` e verifica ownership e bytecode. Não baixa
dependências; POMs/profiles não cobertos falham. Saída de produto no `target/` raiz
é rejeitada; ao migrar um checkout antigo, `mvn clean` remove a saída anterior.

Com Maven:

```sh
mvn clean verify
# Seleção coerente: parent + modelo, com a mesma suíte e gate compilado
mvn -pl air-model -am verify
```

`ContractSuite` roda exatamente uma vez no modelo, na fase `test`, em JVM separada
com `-ea`, classpath de testes e cwd `${project.basedir}`. Seu log é validado e
apresentado em `verify`; omissão, duplicação, reordenação ou skip falham. Cada JAR
passa por effective POM, dependency tree JSON e inspeção focalizada de bytecode,
sem invocar Maven/full novamente. Plugins Maven podem precisar de rede no primeiro
uso; não são dependências de runtime. O harness `full` exige o reactor completo.

## Consumir

```xml
<dependency>
  <groupId>io.github.gustavo2358</groupId>
  <artifactId>air-java</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```java
Publication publication = lowerer.lower(semanticInput);
ValidationResult result = AirValidator.validate(publication);
if (!result.isStructurallyValid()) {
    throw new IllegalStateException(result.toString());
}
// SEMANTIC_OBLIGATION continua exigindo evidência do produtor/autoridade.
CfgBuildResult cfg = cfgBuilder.build(publication, options);
```

`lowerer`, `cfgBuilder` e `CfgBuildResult` ilustram componentes externos; não são
fornecidos aqui.

## Contrato do Validator

Construtores verificam forma local e imutabilidade. Referências cruzadas,
ownership, posições e precondições decidíveis exigem
`AirValidator.validate(publication)`. O Validator nunca repara nem modifica a
publicação.

- `STRUCTURALLY_VALID`: traversal completo, sem erro estrutural ou limite computado;
- `INVALID_IR`: contradição estrutural detectada;
- `INCOMPLETE_VALIDATION`: capacidade/versão não interpretada, limite operacional
  ou precondição que esta implementação não conseguiu decidir;
- `RESOURCE_LIMIT`: kind operacional de exhaustion, com traversal incompleto;
- `SEMANTIC_OBLIGATION`: `ValidationIssue.Kind` que preserva uma obrigação da
  autoridade/produtor sem transformá-la em fato ou certificado Java.

`issues()` contém apenas mensagens retidas. Use `hasIssues(kind)` e
`diagnostics().counts()` para categorias/totais, e `diagnostics().traversalCompleted()`
para distinguir trabalho completo de interrupção operacional. Options e Limits
mantêm construtores int positivos; defaults de entidades/bytes/profundidade usam
Integer.MAX_VALUE (representabilidade desta API em memória). O default retém até
10.000 mensagens, sem interromper checks; exhaustion acrescenta um marcador
operacional. Budgets menores são opt-in. [Desenho e migração](docs/quality/air-capacity.md).

`STRUCTURALLY_VALID` não certifica automaticamente um perfil AIR. A verdade de
premissas, a correspondência com a entrada do produtor, a cobertura de effects e
outcomes e os resultados de análises derivadas exigem seus próprios oráculos.

## Decisões de representação

Records, sealed types, `Optional`, listas e índices internos são detalhes Java.
Eles não definem semântica AIR nem namespace por nome de classe. Identidades usam
seu proprietário completo; `ObjectId` não é `StorageId`, e `localId` isolado não
é chave global.

`unknown_type` não é wildcard. Compartilhar uma lacuna não prova `sameDomain`.
O Validator só usa identidade, domínio conhecido, célula/alias/leitura e premissa
normativa aplicável ao site. Não escolhe candidato, entrada de retorno, storage ou
fallthrough para tornar uma publicação válida.

Antes de propor mudanças, execute os dois gates, revise o diff completo,
dependências e estado do Git. Não publique artefatos nem faça merge sem autorização.
