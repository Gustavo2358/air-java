# Evidência de validação da reconciliação AIR 2.0.0

## Fontes e baseline

- `analysis-ir/main` consultada em
  `122ce54e1b9ef9b00646f93ece409ca8b63bc933`.
- Baseline Java antes da mudança:
  `2108294d9dfeb89d0019ce75fab27172b15a75b9`.
- `bindings/revisao-json-v1.md` usado como handoff; binding JSON continua DRAFT
  e não foi implementado.
- Discovery completo registrado em `docs/reconciliation-air-2.md`.

## Executado

- `./scripts/check.sh` no baseline: PASS com 94 checks.
- `mvn verify` no baseline: PASS.
- `./scripts/check.sh` após modelo, Validator, testes, exemplo e documentação:
  PASS com **172 checks determinísticos**.
- `mvn verify` final: BUILD SUCCESS, executando os mesmos 172 checks.
- `java -cp target/air-java-0.1.0-SNAPSHOT.jar examples/MinimalPublication.java`:
  `STRUCTURALLY_VALID`.
- `git diff --check`: PASS durante a revisão; repetido no gate final de handoff.
- `jq empty docs/sources.lock.json`: PASS.
- `jdeps --multi-release 21 --print-module-deps`: executado pelo harness;
  o JAR final depende somente de **`java.base`**.

A suíte inclui 6.000 `assign`/12.000 operandos para observar indexação, reuso de
premissa em 1.000 cópias e 16 validações concorrentes. Os novos testes cobrem
I-55–I-61 e os contracasos estruturais de O-86–O-91: contratos por site,
targets, disjunção, remoção de safety, assinaturas independentes, outcomes,
return, owners, proveniência e ocorrências referenciadas.

## Falsificação controlada

Foi removido temporariamente apenas o ramo que detecta bases repetidas em
`disjoint_storage` dentro de `ReferenceChecks`.

```text
./scripts/check.sh
...
FAIL - disjoint_storage duplicate base is rejected
exit code: 1
```

O check foi restaurado por patch, sem reset/stash/checkout. O SHA-256 do arquivo
voltou ao valor anterior:

```text
8289be3251b953575b4b633722de83f403e05e68789decc5e57c8893a1ef0e74
  src/main/java/io/github/gustavo2358/air/validation/ReferenceChecks.java
```

O gate imediatamente posterior à restauração passou; testes acrescentados depois
também passaram no gate final de 172 checks. A evidência estruturada está em
`docs/mutation-evidence.json`. Nenhuma mutação artificial permanece.

## Limites observados, não mascarados

- Validade física de `disjoint_storage`, verdade de `sameDomain` e fidelidade do
  produtor aparecem como `SEMANTIC_OBLIGATION`.
- Bounds simbólicos, codec não decidido, igualdade de extensão e compatibilidade
  de `return` dependente de alcançabilidade aparecem como `VALIDATION_LIMIT`.
- Contradições conhecidas continuam `INVALID_IR`; ausência de certificado privado
  não transforma uma precondição em sucesso.
- Limites de recursos do Validator produzem `INCOMPLETE_VALIDATION`, sem truncar
  ou alterar a cardinalidade AIR.

## Não executado / não alegado

- Workflow GitHub Actions: não executado localmente; deve rodar no PR.
- JSON/schema/round-trip: não existe codec neste repositório e o binding é DRAFT.
- Testes dos repositórios consumidores/produtores, inclusive `cobol-lowering`:
  não executados; a API incompatível precisará ser adotada por eles.
- CFG, effects calculados, reaching definitions, possible values e oráculos de
  fluxo: fora do escopo e não implementados.
- Certificação integral de perfis AIR: não alegada por este Validator estrutural.

## Saída final resumida

```text
ok 172 - computed dependency name must be observed before owning operation
PASS: 172 deterministic contract checks
PASS: architecture bytecode dependency check (java.base only)
PASS: JAR generated in target/air-java-0.1.0-SNAPSHOT.jar
```

```text
[INFO] BUILD SUCCESS
```
