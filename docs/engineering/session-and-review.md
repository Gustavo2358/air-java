# Sessão, Git e revisão humana

## Início e retomada

Inspecione remote, branch/upstream, HEAD, status, staged/unstaged e work item.
Faça fetch explícito de origin e confirme a base main. Use branch dedicada a partir
da base sincronizada; se limpa e atrasada, fast-forward. Preserve dirty state alheio;
use checkout isolado quando necessário. Não executar reset/stash/clean/discard ou
force-push silencioso. Outros repos podem ser consultados; seu conteúdo não é editado.

A autorização explícita da sessão persiste. Registre seu escopo no manifesto e
execute as etapas necessárias até o limite solicitado. Checkpoint não cria pedido
de aprovação automático. Se houver discovery com aprovação exigida no contrato,
respeite essa fronteira específica; backlog não autoriza consumir etapas futuras.

Na retomada leia manifesto, state, Git real e evidência correspondente. Confirme
push/CI/PR por fonte remota quando necessário; arquivo local dizendo PASS não é
prova de CI. Não repita implementação já feita nem invente certificado perdido.

## Trabalho e revisão

Mantenha uma branch/PR por trabalho, inclusive correções de review. Antes de commit:
revise diff completo e staged, scopes/proteções, dependências, resultados dos gates,
restauração de falsificações e documentação canônica. `git --work <ID>` é verificação
local; não faz fetch/push, não certifica identidade humana e não consulta GitHub.

Após push autorizado, confirme SHA publicado e checks remotos desse SHA; um run
antigo ou outro head não vale. CI local e remoto são evidências distintas. Falha
reparável dentro do escopo é corrigida na mesma branch. Registre bloqueio real com
causa, tentativas que trouxeram informação e ação necessária; não repetir tentativas
sem progresso nem inventar GREEN para encerrar.

Review descreve entrada/trigger, observado versus esperado, regra, arquivo e impacto.
Self-review deve ser chamado de self-review; não declarar revisão independente sem
outro revisor. Este harness não exige múltiplos agentes ou recibos autorreferentes.

## Handoff

Entregar objetivo e diff, commit/PR, comandos/exit codes, checks realmente executados,
não executados e por quê, limites, findings e próximo passo não iniciado. Logs locais
ficam em `target/harness`; evidência durável resume resultados e identifica baseline.
Não gravar SHA futuro dentro do próprio commit nem fazer commit só para autoinscrever
seu resultado de CI. O PR pode carregar o recibo remoto do head.

Parar para revisão humana no PR quando solicitado. Abrir PR não significa aprovado,
mergeado ou artefato publicado; nunca ligar auto-merge/publicação por conveniência.
