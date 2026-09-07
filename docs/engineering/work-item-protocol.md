# Lifecycle de work items

[registry.json](../work/registry.json) e [índice](../work/index.md) roteiam o estado.
Item ativo usa `docs/work/active/WORK-AIR-<ÁREA>-<NNN>/` com exatamente cinco arquivos:
`work-item.json`, `spec.md`, `plan.md`, `eval.md`, `state.md`.

Estados ativos: `active`, `blocked`, `ready_for_review`. `completed` pertence apenas
a history após confirmação confiável do merge/encerramento. Não declarar conclusão
humana no commit que apenas abre PR. Após merge, arquivar resumo curto com merge SHA,
remover active e reconciliar registry/index na próxima manutenção autorizada.
Conhecimento durável fica em arquitetura/domínio/engenharia/evals, não em tasklists.

## Manifesto

Campos exatos validados por [docs.py](../../scripts/harness/docs.py): id, title, status,
authorization, authorization_evidence, goal, checkpoint, base_commit, branch,
must_read, change_scope, must_not_change, invariants, evals, gates, stop_condition.
Authorization é `discovery`, `harness` ou `implementation`; texto registra a instrução
recebida, não comprova sua autenticidade. `base_commit` é SHA completo, branch é dedicada.

Scopes são paths literais relativos: `arquivo` ou `diretorio/` (subárvore), sem glob,
path absoluto ou `..`. Novos caminhos são permitidos no change_scope; must_read exige
existência. must_not_change prevalece. Uma nova base exige revisão explícita do diff;
não atualizar base para esconder mudanças. `git/scope` incluem mudanças commitadas,
staged, unstaged, untracked e ambos os lados de rename desde essa base.

Harness/discovery não autorizam `src/`, `examples/` ou `pom.xml`. Para implementação
futura, delimitar fontes/testes/docs concretos no mesmo contrato. Gates obrigatórios
ausentes ou não executados não são PASS. Gate de escopo é explícito com `--work`;
não adivinha qual item autoriza um diff quando houver vários ativos.

## Documentos curtos

Spec: problema, resultado observável, entradas/classes, premissas, incerteza e fora
de escopo. Plan: etapas, dependências, superfícies, riscos e rollback. Eval: oráculos,
positivos, negativos, regressões, challenge e limites. State usa apenas os quatro
headings: Onde estamos, Verde conhecido, Restante, Descobertas que afetam o plano.

[Templates](../templates/README.md) começam sem autorização nem baseline válida;
são preenchidos antes de criar item ativo. Propostas permanecem no backlog.
Validação offline verifica coerência documental, não aprova código ou confirma merge.
CI aceita um item ativo por vez. O encerramento sem novo item admite só docs,
AGENTS e MANIFEST (source lock protegido), exige histórico dos itens anteriores
e merge SHA ancestral. Manutenção subsequente exige novo work item autorizado.
