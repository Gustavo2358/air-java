# Baseline e decisões de adaptação

Em 2026-09-07 os cinco repositórios estavam limpos e iguais à origin/main após
fetch. SHAs completos e hash do roadmap estão em [harness-baseline.json](harness-baseline.json).
O source lock normativo existente permanece byte a byte inalterado.

| Fonte | Trechos consultados | Aproveitamento / adaptação |
| --- | --- | --- |
| proleap-poc | AGENTS; engineering/gates, work-item-protocol, semantic-analysis-policy | índice curto, conhecimento progressivo, work item com cinco arquivos, oráculos e invariantes |
| analysis-cfg | AGENTS; sources/harness-adaptation; engineering/gates, work-item-protocol, performance, security-and-observability; scripts/harness e project/check_architecture | JSON com Python padrão, status honesto de gates e bytecode, contracasos de lifecycle |
| cobol-lower | AGENTS; engineering/gates, agent-session-protocol, git-and-review; sources/upstream-state; pom.xml | escopo explícito, autorização persistente, restauração, segundo GREEN e distinção local/remoto/humano |
| analysis-ir | README e referências do lock/binding nos documentos locais | autoridade AIR separada de representação Java e binding DRAFT |
| air-java | README, AGENTS, ARCHITECTURE, POM, scripts/check, CI, status, catálogo, suíte e evidências | preservar produto e envolver os entrypoints existentes |

Os snapshots podem ser lidos por `git show <commit>:<path>` usando os SHAs
registrados. Leituras dos harnesses são referências de engenharia, sem importar
sua autorização, active work ou alegação de CI.

Não copiar corpus/parser/ANTLR/Node do frontend, arquitetura application/adapters
de lower/CFG ou certificados/FREEZE e dependências Python do lower. Não fixar um
inventário exato de classes de produto que bloqueie extensões legítimas. Reutilizar
as regras que importam à biblioteca e testar seus mecanismos.

O roadmap externo é planejamento do usuário, identificado por SHA-256; não é
copiado nem alterado. As seções 1.3–1.5, 7 e 8 pedem codec separado no mesmo repo,
discovery antes da topologia e gates por módulo. [Backlog](../work/backlog.md)
retém essas dependências. Este trabalho não constitui o discovery 0C-D nem sua
aprovação, e o gate atual não alega proteger módulo ainda inexistente.

O MANIFEST.sha256 existente era íntegro na baseline. A única entrada preexistente
com conteúdo alterado é AGENTS; o manifesto será atualizado e incluirá os novos
arquivos do harness. Não mover documentos existentes evita quebrar seus caminhos.
