# Modelo e validação: mapa de autoridade

Consultar os arquivos abaixo no commit de [sources.lock.json](../sources.lock.json),
conforme [política de fontes](../sources/authority.md). Este mapa não duplica a spec.

| Mudança | Seção AIR | Evidência Java existente |
| --- | --- | --- |
| IDs e owners | especificacao/01-modelo-e-identidades.md; conformidade/01-invariantes.md | Ids, PublicationIndex, ReferenceChecks |
| Tipos, unknown e sameDomain | especificacao/02-tipos-valores-e-operandos.md | Types, DomainProofEngine, TypeResolver |
| Storage, aliases e disjoint_storage | especificacao/03-memoria-e-aliases.md | Memory, Proofs, ScopeRules |
| Operações e operandos | especificacao/04-operacoes.md | Operations, Traversal, OperationChecks |
| Controle, invoke e return | especificacao/05-controle-e-invocacoes.md | Control, Interactions, OperationChecks |
| Coverage/provenance e limites | especificacao/06-incompletude-e-proveniencia.md | Evidence, Origins, ValidationResult |
| Capabilities e claims | especificacao/09-extensibilidade-e-compatibilidade.md; especificacao/10-perfis-de-conformidade.md | Capabilities, ValidationIssue |

Construtores validam forma local. `AirValidator.validate` verifica fechamento,
ownership e precondições decidíveis da publicação. `STRUCTURALLY_VALID` significa
que não encontrou erro ou limite; `INVALID_IR` identifica contradição;
`INCOMPLETE_VALIDATION` preserva versão/capacidade não interpretada ou limite.
`SEMANTIC_OBLIGATION` é um kind de issue, não quarto status de sucesso.
RESOURCE_LIMIT é kind operacional; diagnostics.traversalCompleted informa se
houve interrupção. Status e hasIssues usam contagens totais, não só o prefixo
retido em issues. INVALID_IR já provado pode coexistir com traversal incompleto;
nenhum deles permite isStructurallyValid=true.

Unknown de tipo não é wildcard. Compartilhar uncertainty não prova sameDomain;
premissa por site não se transfere por nome/autoridade igual. Disjunção estrutural
não prova independência física de storage. Ordenação de sequences não cria edges.
Limite operacional não modifica cardinalidade semântica.

O [status existente](../implementation-status.md) é a fonte de cobertura local;
contracasos não equivalem à certificação de todos os perfis AIR. Testar com
expectations vindas da regra, nunca corrigir a AIR a partir da conveniência Java.
