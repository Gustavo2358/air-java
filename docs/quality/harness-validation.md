# Evidência de criação do harness

Data: 2026-09-07. Baseline air-java: 6a4091e5394fc22b3d2ada9abbdb530eb3572a58.
Plano e escopo preservados no commit deste registro em
`docs/work/active/WORK-AIR-HARNESS-001/plan.md` e no manifesto do mesmo diretório.
Estado deste registro: validação local concluída; PR/CI são observações pós-commit
publicadas no handoff remoto, não certificadas por esta prosa.

## Ambiente e baseline

Linux, Python 3.14.4, Temurin 25.0.4 compilando com release 21, Maven 3.9.16.
CI adicional usa Temurin 21/Python 3.12. Plugins Maven resolvidos em
/tmp/air-java-harness-m2; nenhuma biblioteca instalada/publicada.

Cinco checkouts limpos e iguais à origin/main após fetch; referências reconfirmadas
antes da entrega. SHAs e hash do roadmap em [baseline](../sources/harness-baseline.json).
MANIFEST original íntegro antes da mudança. O script original e Maven verify
executaram os 172 checks no baseline. Resolução inicial de plugins falhou por
restrição de rede do ambiente; repetição com acesso à rede completou com exit 0.

## Resultado executado

| Comando / etapa | Resultado |
| --- | --- |
| `python3 -B scripts/harness/run.py full` | exit 0; cinco subgates reais, nenhum omitido |
| docs (via full) | links/anchors, metadados, 7 invariantes, 9 evals, lifecycle e escopos |
| harness (via full) | 54 testes unittest, zero falhas/skips; contracasos em fixtures isoladas |
| architecture (via full) | 308 classfiles Java 21 sem preview; 4.664 referências diretas inspecionadas |
| semantic (via full) | script original; 172 checks nominais completos e ordenados |
| maven (via full) | clean verify; os mesmos 172 checks e BUILD SUCCESS |
| `python3 -B scripts/harness/run.py git --work WORK-AIR-HARNESS-001` | exit 0; branch, ancestry, origin local, diff e escopo |
| `git diff --check` e comparação dos caminhos protegidos com a base | exit 0; produto intacto |

Contracasos incluem dependência inversa, I/O/rede/reflexão/processo, JSON ausente no
classpath, bytecode inválido, missing/duplicate/reordered checks, suíte vazia/skip,
child exit não zero, agregador interrompido, gate UNAVAILABLE, links/anchors/IDs,
autorização vazia, lifecycle, path externo, rename e todos os estados do diff Git.
Também há provas de fechamento documental sem item ativo e rejeição de mudança de
produto nessa exceção, além de base alterada para tentar esconder o diff do evento.
Os testes foram repetidos em GREEN após as correções do harness. Nenhuma mutação
foi aplicada aos fontes de produção do checkout ou dos repos irmãos.

## Escopo e revisão

Self-review do diff e dos limites executado; revisão independente/humana continua
pendente no PR. AGENTS e MANIFEST são os únicos arquivos preexistentes alterados;
os demais são adições do harness. MANIFEST é atualizado para incluir esses arquivos
e verificado com sha256sum antes do commit. Produto, POM, suíte, exemplos, source
lock normativo, documentos existentes e workflow original permanecem inalterados.
Impacto downstream: NONE para API/runtime; novo requisito operacional Python/JDK
para os gates do harness, explicitado em [gates](../engineering/gates.md).

## Não executado / não alegado

Suítes de proleap-poc/cobol-lower/analysis-cfg, novo mutation testing do Validator,
modularização, codec, transporte, E2E e certificação integral de perfis AIR.
Performance focalizada/transport/integration retornam UNAVAILABLE (exit 3), com
contracasos próprios; as propriedades de escala já existentes continuam na suíte.
Doc checker não prova autenticidade de autorização ou estado remoto, nem implementa
parser CommonMark completo. Bytecode cobre referências diretas, sem prova de acesso
dinâmico. Inventário de nomes não substitui assertions, falsificação ou review.
