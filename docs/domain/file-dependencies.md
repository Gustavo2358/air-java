# FILE-DEPENDENCIES — contrato/codec a qualificar em W1

FD-H0–H4 é somente harness. [Campanha/brief](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/product/file-dependencies/README.md)
e [provas A1–A6/O1–O5](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/product/file-dependencies/contracts.md)
são canônicos (workspace: `../analysis-cfg/docs/product/file-dependencies/`).

## Gap verificado, sem mudança normativa preventiva

`Interactions.Resource` e LiteralTarget/ComputedTarget/ComputedResource existem;
`Artifacts.Relation` tem source ArtifactId e target artifact/literal.
`BindingWriter` exige resources/artifactRelations vazios; `BindingReader` também.
Logo o modelo não basta para transportar inventário declarativo. W1 precisa de
codec/validator/traversal/catalog coverage, mesmo se não criar novo tipo AIR.

Owner/conector/registro/uso ainda requer desenho bilateral D-AIR. A associação
tipada deve permitir responder A1–A6 sem ler COBOL, SP, localId ou texto de origem.
Antes de criar tipos, confirmar insuficiência das relações atuais e, se preciso,
alterar a autoridade analysis-ir em PR próprio. Gap de codec não autoriza inventar
conceitos normativos. Núcleo continua independente de COBOL/CFG/solver.

| Teste futuro de W1 | Obrigação independente |
| --- | --- |
| declaração sem uso | transporte não exige invoke fictício; recurso/owner/origens preservados |
| mesmo alvo, owners diferentes | identidade completa, sem fusão por grafia |
| recurso computado/unknown | referência/uncertainty íntegra, nunca target vazio exato |
| recursos e relações não vazios | encode→decode + consumer; negativos de refs/owners/capability |
| invoke FILE e invoke CALL juntos | ação/category/namespaces/outcomes distintos; CALL sem regressão |
| extensão não reconhecida | fallback/recusa explícitos conforme autoridade; sem tag ignorada |

Usar `air-json/.../CodecSuite`, `InvokeChecks`, `InvokeOracle`,
`RegionalChecks` e `PartialAnalysisChecks`; expectativas manuais, não só roundtrip.
`bash scripts/check.sh` é compile/model/codec/boundaries offline; `lean.py fast`
adiciona testes do harness. `qualification-local` é local/on-demand.
H0–H4 roda apenas `python3 -B scripts/harness/lean.py docs`.

Não adicionar DSNAME, runtime allocation, JCL state ou resolução física ao modelo.
Todos os significados e limites da capability permanecem no brief, sem cópias aqui.
