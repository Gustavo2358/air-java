# FILE-DEPENDENCIES — contrato/codec a qualificar em W1

H4 aprovado; core W0–W9/W11 autorizado, sem W10/merge. [Campanha/brief](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/product/file-dependencies/README.md)
e [provas core A1–A4/A6/O1–O5](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/product/file-dependencies/contracts.md)
são canônicos (workspace: `../analysis-cfg/docs/product/file-dependencies/`).

## Gap verificado, sem mudança normativa preventiva

`Interactions.Resource` e LiteralTarget/ComputedTarget/ComputedResource existem;
`Artifacts.Relation` tem source ArtifactId e target artifact/literal.
Baseline H4: `BindingWriter` e `BindingReader` recusavam resources/artifactRelations não vazios.
Logo o modelo não basta para transportar inventário declarativo. W1 precisa de
codec/validator/traversal/catalog coverage, mesmo se não criar novo tipo AIR.

Owner/conector/registro/uso ainda requer desenho bilateral D-AIR. A associação
tipada deve permitir responder A1–A4/A6 sem ler COBOL, SP, localId ou texto de origem.
A5 pertence à extensão D/W10 posterior, sem antecipar norma AIR no core N+C.
Antes de criar tipos, confirmar insuficiência das relações atuais e, se preciso,
alterar a autoridade analysis-ir em PR próprio. Gap de codec não autoriza inventar
conceitos normativos. Núcleo continua independente de COBOL/CFG/solver.

| Teste futuro de W1 | Obrigação independente |
| --- | --- |
| declaração sem uso | transporte não exige invoke fictício; recurso/owner/origens preservados |
| nome externo conhecido | transportar o namespace source-level fornecido pelo produtor; não promover assignment-name a mecanismo DD nem adicionar bindingMechanism UNKNOWN; sem regra COBOL no core AIR |
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

## FD-W1 — resource.bindings@1

D-AIR demonstrou perda de owner/objeto/uso antes da extensão; prova preservada no
E2E/w1 e norma proposta em analysis-ir PR #7, pin fb153ae50f343022db45d20d627e1afac85de916.
Resource mantém Target separado e recebe declaração opcional com owner, nome,
classificações, objetos e usos por papel; LocalResource não tem alvo externo.
UnknownResource exige lacuna DEPENDENCIES aplicável. I-RB-01/02/03 validam refs,
visibilidade resolvida, duplicatas, classificação e capability. Verdade dos papéis
é obrigação do produtor; não altera efeitos/controle ou resolve nomes.

Impacto BREAKING para consumers da nova capability; publicações antigas sem ela
conservam forma e semântica. Construtor Resource de três argumentos conserva
associação não publicada. Nenhum fallback inventa declaração vazia.
A1–A4/A6 manuais + wire independente + negativos: ResourceBindingOracle/Checks.
A-CODEC PASS: 187 checks model, 124 transporte. Consumer bilateral ainda NOT_RUN.
Gate fixa o novo pin normativo; allowlist de bytecode admite somente os accessors
ResourceDeclaration.name():String e ComputedResource.name():OperandId, sem liberar
Enum.name. Descriptor resource antes não implementado agora recusa shape ruim por
INPUT_ERROR. Nenhum gate foi omitido/relaxado.
