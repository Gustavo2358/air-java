# Evidência de validação desta entrega

## Executado

- JDK: OpenJDK 21.0.11; `javac --release 21 -Xlint:all -Werror`.
- Gate `scripts/check.sh`: duas passagens completas após implementação/hardening,
  sendo a última após restaurar todas as mutações temporárias.
- **94 verificações determinísticas** em cada passagem final.
- Cenário de escala: **6.000 assign**, 6.001 operações totais, 12.000 operandos,
  6.000 consultas de domínio; sem oracle frágil de milissegundos.
- Reuso de prova de unidade em 1.000 cópias, validação concorrente em 16 execuções,
  além de imutabilidade, tipos, escopos, choices abertos, chamadas e regiões.
- JAR inspecionado com `jdeps --print-module-deps`: **somente java.base**.
- `examples/MinimalPublication.java`: executado contra o JAR compilado.

## Falsificações temporárias realmente executadas

Cada mutação foi aplicada isoladamente em cópia de trabalho, executou o gate até
falhar e teve o arquivo restaurado byte a byte. Os SHA-256 abaixo foram conferidos
novamente depois da restauração. As mutações não estão nos fontes finais.

| Mutação | Teste que detectou | Exit code |
| --- | --- | --- |
| `domain` | `FAIL - assign different concrete domains rejected` | 1 |
| `reference` | `FAIL - missing label rejected` | 1 |
| `architecture` | `FAIL - pure core has no JSON/frontend imports` | 1 |

### Hashes de restauração

- `src/main/java/io/github/gustavo2358/air/validation/OperationChecks.java`: `9a932446ae56598752ebea6576fe0b15b55aa8be39f853d357fcbf04725b3b7e`
- `src/main/java/io/github/gustavo2358/air/validation/ReferenceChecks.java`: `4f3c3ebca68eade0c8bdd8fd989c608249e62123a0f9328b903c67daac4462bd`
- `src/main/java/io/github/gustavo2358/air/model/Publication.java`: `586e1155199b9f96fc96f5fe852733160de52e029ac39ab2ca58be482f5a38b4`

## Não executado / não alegado

- `mvn verify`/`mvn install`: Maven não estava instalado neste ambiente. O POM foi
  fornecido e sua estrutura XML conferida; o ciclo Maven deve ser executado no repo.
- Workflow GitHub Actions: arquivo preparado, não foi disparado nem consultado como
  evidência de sucesso desta entrega.
- Oráculos de JSON: não existe codec aqui; o binding é uma proposta de documento.
- Certificação integral dos perfis AIR e todos os 85 oráculos normativos: não alegada.
  Esta suíte verifica contratos do modelo/validator; não executa CFG/RD/PV.

## Saída final do gate

```text
ok 93 - large number of unit-scoped domain premises is reusable
ok 94 - two independent constructions validate identically
PASS: 94 deterministic contract checks
PASS: architecture bytecode dependency check (java.base only)
PASS: JAR generated in target/air-java-0.1.0-SNAPSHOT.jar
```

## Exemplo

```text
STRUCTURALLY_VALID: Statistics[entities=6, operands=0, operations=1, domainQueries=0]
```
