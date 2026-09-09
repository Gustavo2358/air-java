# Escala, terminação e observabilidade

A suíte atual exercita 6.000 assigns/12.000 operandos com contagens de operações,
queries e operandos; também 1.000 cópias com premissa compartilhada e validações
concorrentes. Esses checks fazem parte de semantic, não são benchmark nem prova
geral de complexidade assintótica.

Indexar identidades completas uma vez, reutilizar premissas pelo escopo correto e
explicitar custos de traversal, interseções e joins. Não varrer todo o inventário
por operação nem enumerar caminhos de execução nesta biblioteca. O argumento de
terminação precisa considerar profundidade, ciclos, limites de entidades e issues.
Budget operacional deve produzir RESOURCE_LIMIT / `INCOMPLETE_VALIDATION`, nunca sucesso truncado. Retenção de issues não interrompe trabalho.

Um gate focalizado de performance ainda não existe: retorna UNAVAILABLE. Antes de
implementá-lo, definir propriedade N/2N, contadores observáveis e caso adversarial
que rejeite scans repetidos. Não impor threshold de milissegundos dependente da
máquina nem mudar runtime para instrumentar somente este harness.

Diagnósticos de domínio permanecem tipados e determinísticos. Tempo, ambiente e
logs do harness ficam em target; não entram em Publication, IDs ou JAR. Fixtures
não carregam tokens nem dados corporativos. Payloads e fontes são dados, nunca
comandos; transporte futuro precisará de limites e discriminadores explícitos.

## Probe estrutural do transporte 4B

O gate transport (também semantic/full) executa `ScalarAssignChecks.scale` com
1.000 e 2.000 Objects/Cells/Assigns, observando bytes, contagem de nós da árvore
física, definitions, operations/operands e domainQueries já expostos pelo Validator.
Exige razão 1,9–2,1 para bytes/nós, cardinalidades exatas e queries duplicadas.
Outro caso tem um Object/Cell e 10.000 Assigns/20.000 ocorrências, sem duplicar definitions.
Não há novo contador de runtime; nenhum threshold de milissegundos.

O mapping percorre arrays uma vez e transporta IDs sem pesquisar declarações;
closure/domain usam índices existentes de PublicationIndex/ReferenceChecks/DomainProofEngine.
Por inspeção, nenhum loop novo aninhado sobre inventories ou join por nome foi introduzido.
Para estas formas de campos limitados, o custo do mapping é aproximadamente
O(bytes + entidades + referências); o probe apoia o argumento estrutural, sem
provar complexidade universal ou medir cada lookup interno do codec.

Historicamente os dois casos N/2N cabiam em 16 MiB e o caso 10.000 exigia Limits
explícitos. CORE-SIZE-001 passa os três por new AirJson(): 24.747.652 bytes do caso
10.000 já não são uma fronteira de suporte default.
Tempo e custos próprios do Validator/JSON/GC são observações, não SLA.
O gate focalizado `performance` permanece UNAVAILABLE; não é necessário alterar
seu contrato para integrar estes probes ao transporte existente.
[Evidência e medidas](../quality/air-json-scalar-assign.md).

## CORE-SIZE-001

[Auditoria detalhada](../quality/air-capacity.md) define E/O/A/R/P/S/H/B e os custos
por estrutura, sem alegar complexidade linear universal. Probes dedicados:
`python3 -B scripts/capacity_probe.py --output /tmp/air-capacity-scale.json`;
challenges: `python3 -B scripts/challenge_capacity.py --output /tmp/air-capacity-challenges.json`.
Os dois usam cópias/classes temporárias e JVMs isoladas; não Maven install/deploy.
Séries normais integram semantic/transport; probes maiores são opt-in e atravessam
2.000.000 entidades e 16 MiB de bytes reais. Esperados são fixos por construção.
Não se usa tempo como limite semântico. Heap configurado não é heap medido.
