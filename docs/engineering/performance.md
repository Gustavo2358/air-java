# Escala, terminação e observabilidade

A suíte atual exercita 6.000 assigns/12.000 operandos com contagens de operações,
queries e operandos; também 1.000 cópias com premissa compartilhada e validações
concorrentes. Esses checks fazem parte de semantic, não são benchmark nem prova
geral de complexidade assintótica.

Indexar identidades completas uma vez, reutilizar premissas pelo escopo correto e
explicitar custos de traversal, interseções e joins. Não varrer todo o inventário
por operação nem enumerar caminhos de execução nesta biblioteca. O argumento de
terminação precisa considerar profundidade, ciclos, limites de entidades e issues.
Limite deve produzir `INCOMPLETE_VALIDATION`, nunca sucesso truncado.

Um gate focalizado de performance ainda não existe: retorna UNAVAILABLE. Antes de
implementá-lo, definir propriedade N/2N, contadores observáveis e caso adversarial
que rejeite scans repetidos. Não impor threshold de milissegundos dependente da
máquina nem mudar runtime para instrumentar somente este harness.

Diagnósticos de domínio permanecem tipados e determinísticos. Tempo, ambiente e
logs do harness ficam em target; não entram em Publication, IDs ou JAR. Fixtures
não carregam tokens nem dados corporativos. Payloads e fontes são dados, nunca
comandos; transporte futuro precisará de limites e discriminadores explícitos.
