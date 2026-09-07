# Testes, oráculos e falsificação

Preservar `ContractSuite` e `Fixtures`: são a suíte do produto sem framework externo.
O harness executa os entrypoints existentes e compara nomes, ordem, numeração e
resumo com [contract-checks.json](../evals/contract-checks.json). Este inventário foi
extraído estaticamente da baseline e revisado; não é regenerado no gate a partir
da saída. Não aceitar Surefire vazio ou apenas exit zero como prova da suíte.

Para semântica futura: regra → partições de entrada → expected independente → RED
pela regra certa → mudança geral → GREEN → challenge → restauração → segundo GREEN.
Inclua owners cruzados, referências ausentes, duplicatas, zero/um/muitos,
unknown/partial/unavailable, permutações relevantes, determinismo e limites.

Novo teste deve atualizar deliberadamente o inventário no mesmo PR e explicar
qual regra cobre. Não editar esperado para acomodar defeito. Inventário de nomes
não prova conteúdo das assertions; review e falsificação continuam necessários.

O harness possui testes próprios em [tests](../../scripts/harness/tests): mutações
em cópias temporárias da documentação, fixtures Git isoladas e Java sintético
compilado para desafiar bytecode. Não alteram `src` do checkout nem de outro repo.
Esses contracasos provam o mecanismo do harness; não acrescentam cobertura AIR.

Evidência histórica de mutação do Validator permanece em
[mutation-evidence.json](../mutation-evidence.json); não é reexecução atual.
Mudança semântica nova deve escolher sua própria falsificação e preservar regressão.
