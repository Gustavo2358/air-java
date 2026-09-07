# Plano executável — harness

1. **Discovery do harness:** confirmar clean HEAD/origin dos cinco repos, registrar
   SHAs, ler padrões e estado real do air-java/roadmap; baseline offline e Maven.
2. **Conhecimento:** AGENTS curto, rotas para docs existentes, missão, fronteiras,
   invariantes, políticas de engenharia, lifecycle, templates e backlog não ativo.
3. **Execução:** Python padrão; docs/IDs/manifestos, contratos existentes, bytecode
   por classe, Git/scope; full com agregação explícita e gates futuros indisponíveis.
4. **Prova:** contracasos em cópias temporárias, Java sintético e repos Git isolados;
   rodar full e scope, revisar original/proposta e integridade. Revalidar após correção.
5. **Entrega:** atualizar estado/evidência, commit/push na branch dedicada, PR com
   plano/resultado/testes/limites e CI do head; parar na revisão humana.

Superfície: apenas documentos e scripts do harness, CI adicional, template do PR,
AGENTS e MANIFEST. Produto, Maven e entrypoints existentes permanecem intactos.
[ADR-0001](../../../architecture/decisions/ADR-0001.md) explica alternativas.

Riscos: gate arquitetural amplo demais (contracasos e whitelist JDK documentada),
expectations derivados do próprio teste (inventário nominal estático revisado),
metadados tratados como autorização (review/contexto continuam autoridade), suite
verde incompleta (verificação nominal), duplicação de docs (links ao conteúdo atual).

Rollback: reverter o commit do harness por commit de reversão revisável; os gates
originais continuam independentes. Sem migração de API/artifactId ou lock neste plano.
