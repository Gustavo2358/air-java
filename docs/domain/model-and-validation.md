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
premissa por site não se transfere por nome/autoridade igual. Bases StorageId distintas são independentes no modelo; aliases/vistas
resolvem compartilhamento positivo. DisjointStorage permanece asserção redundante,
validada estruturalmente, sem nova obrigação de fidelidade física da fonte. Ordenação de sequences não cria edges.
Limite operacional não modifica cardinalidade semântica.

Na resolução W3-R1, Cell representa armazenamento lógico sem afirmar layout em bytes. UnknownBinding é incerteza semântica de localização limitada ao escopo publicado. Um ObjectPlace ou MemoryScope/MemoryBound usado executavelmente cuja cadeia de binding/scope não alcança Cell, Region/StorageId ou bound amplo explícito recebe INVALID_IR (`I-13`); isso inclui HavocMay, envelopes, efeitos externos, remainingReads e Choice remainder. Ciclo nominal não vira AllMemory. Uma declaração nominal sem uso executável pode reter o ciclo como cobertura. Repetição de membro já resolvido não é ciclo. O contrato normativo permanece em Analysis IR 03 §4.1.

O [status existente](../implementation-status.md) é a fonte de cobertura local;
contracasos não equivalem à certificação de todos os perfis AIR. Testar com
expectations vindas da regra, nunca corrigir a AIR a partir da conveniência Java.

## EP-W5 — escopo completo de precondições abertas

`ValidationResult.unprovedOperationPreconditions()` fornece uma permissão restrita
para a política AIR 08 §9. Exige travessia concluída, ausência de INVALID_IR,
RESOURCE_LIMIT e UNSUPPORTED_CAPABILITY nos totais, retenção de todos os limites,
regra PRECONDITION_NOT_DISCHARGED e proprietário OperationId (direto ou via OperandId).
Retorna conjunto imutável; entry forte e limite sem escopo não são admitidos.
O status de validação não muda. O consumidor deve manter esses motivos e usar
atualização fraca nas operações afetadas. A biblioteca não executa essa análise.
