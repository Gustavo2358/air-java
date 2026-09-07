# Estado — WORK-AIR-JSON-001

## Onde estamos

Remediação solicitada em review humano no PR #5, mesma branch
feat/air-json-codec-mvp. Head de entrada local/remoto confirmado:
b2c923230b2a857a5cb3beb8baadcb5eb9f77aef; working tree inicialmente limpa.
O patch local da rodada interrompida foi preservado nesta continuação.
Sem checkout de main, rebase, force-push ou nova branch/PR.
Baseline 0C-I: 71937dfe88bac4dae10f6f195731acac638c2d29, merge real do PR #4
confirmado às 2026-09-07T18:08:56Z; lifecycle anterior arquivado após confirmação.
Remediação publicada em ab25ea0c9579d91d307118b3b88f7ccf82490a99, por push normal;
SHA remoto confirmado. CI push/PR verde e recibos atualizados no PR #5. Review
independente desse head retornou request changes: novo blocker de classificação
nas demais precondições de Span. Esse blocker foi registrado conforme seção 7 e
resolvido pela nova decisão humana e pelas guardas: item ready_for_review local. Os sete registros
documentais locais de entrada foram preservados; ainda sem novo commit/push.

## Verde conhecido

0B PR #3 mergeado em 51b4d9a8ae0364232bd97103cd73a77e1a34996c;
handoff operacional nesse merge, autoridade apenas analysis-ir pin
122ce54e1b9ef9b00646f93ece409ca8b63bc933: analysis-ir-json / 1.0.0 / AIR 2.0.0 DRAFT.

Finding 1: mappings explícitos para 20 tokens, oracle literal e guarda de bytecode
contra name/toString/String.valueOf. Finding 2: INVALID_IR local com ValidationIssue
regra/detail explícitos e path do site; issues originais do AirValidator intactos.
Decisão humana recebida para três gaps de representabilidade: bases >1,
EntityScope vazio e Text blank admitido pelo pin retornam IMPLEMENTATION_LIMIT
por condições explícitas. Nenhum catch genérico classifica falha de construtor.

Full do head anterior ab25ea0 exit 0: docs/MANIFEST, 86 harness tests, 308 model + 17 JSON classes,
4664/420 arestas, 172 model + 51 transport via check.sh e root clean verify.
Transport exit 0, 51 checks (43 originais preservados, 8 acrescentados na remediação).
Docs/MANIFEST, Git/scope e diff --check passaram após atualização da evidência.
21 mutações de codec RED, fontes restauradas byte a byte e segundo GREEN de 51.
Seis challenges de gate e probe Maven skip preservados como evidência anterior;
os contracasos permanentes do harness continuam executados no full atual.
Golden, air-model e pin intactos; grafos externos vazios e JSON → air-java compile.
[Evidência](../../../quality/air-json-implementation.md) e
[recibo dos challenges](../../../quality/air-json-challenges.json).

## Restante

Guardas e seis regressões novas implementadas e gates locais verdes. Diff integral
desde ab25ea0 revisado; revisar staged e criar commit focalizado na branch existente,
push normal, confirmar SHA/CI e atualizar PR #5. Retomar review independente do novo head e
corrigir request changes até liberação técnica, depois parar para review humano.
Sem merge/auto-merge, integração, mudança no model ou ampliação de formas.

## Descobertas que afetam o plano

O blocker anterior foi resolvido por decisão explícita do usuário: AIR válida no
pin mas não representável pelos três predicados extras conhecidos do Java é limite
de implementação, nunca INVALID_IR sem regra inventada. A investigação normativa
incluiu consulta independente; AIR 06 §5 não restringe bases a 0/1, Text não recebe
nonBlank genérico, e FactScope não herda a restrição de DomainProofScope de I-52.
[BACKLOG-AIR-006 / AIR-MODEL-DRIFT](../../backlog.md#backlog-air-006--air-model-drift)
registra as três dívidas e alternativas futuras. Não modifica o model nem bloqueia
o GOBACK E2E atual; execução desse backlog permanece não autorizada.

Novas falhas inesperadas de constructor mantêm identidade para investigação;
não são reinterpretadas pelo texto da exception. Teste de injeção e challenge
contra catch genérico protegem essa fronteira. Campos Text auditados estão na
política; contentDigest blank representável é preservado e tokens/versões mantêm
seus diagnósticos. Limite identificado não certifica os demais fatos da entrada.
JSON segue sem biblioteca externa, check.sh offline; formas ampliadas, Base64 e
decimal DEFERRED. Binding continua DRAFT, sem claim de interoperabilidade integral.

Blocker histórico de ab25ea0: trocar somente start.line de "4" para "5" no primeiro
span do golden (fim permanece 4:16) faz AirJson.decode lançar IllegalArgumentException
crua, sem path tipado. Revisor reproduziu 13 variantes: inversão de linha/coluna,
coordenadas abaixo da base e IncludeFrame.site. A implementação repetiu o caso
mínimo em /tmp/air-1a/remediation-new-span-blocker.log, exit 1, sem editar produto.

Binding §§3/10.3 no pin define Natural/Position/Span e preservação de base/unidade;
AIR 06 §5 exige convenção e I-36 proíbe span fabricado. A consulta não identificou
regra explícita que resolva esses predicados adicionais. Naquele momento a
classificação ficou pendente; foi resolvida pela decisão explícita abaixo para
o checkpoint/pin atual. A falha concreta não
pode mais ser tratada apenas como sentinel abstrato de exceção inesperada.

O review independente de ab25ea0 confirmou full, 21 challenges restaurados e CI;
não encontrou outro request change na remediação. Evidência isolada em
/tmp/air-json-review-r2-smomvs (span-probe.py/log, SpanRepro.java, full.log,
challenges.json/log). Os GREEN daquele head não eliminavam o blocker, agora tratado abaixo.

Decisão humana recebida nesta retomada: classificar explicitamente as coordenadas
abaixo da base e intervalos invertidos como IMPLEMENTATION_LIMIT no pin atual.
Blocker resolvido por essa decisão e pelas guardas; item ready_for_review local. Registros locais anteriores
revisados e preservados; head remoto de entrada reconfirmado ab25ea0. Foram
implementadas apenas as guardas autorizadas, os testes pela API pública e a
separação das dívidas de clarificação normativa e de drift Java. Nenhum novo commit/push nesta retomada ainda.

GREEN desta continuação: transport 57 checks, 27 challenges detectados e restaurados
byte a byte, segundo GREEN de 57. As novas guardas cobrem os dois sites de Span,
sem alterar as classificações físicas ou os INVALID_IR comprovados. BACKLOG-AIR-007
separa clarificação normativa de coordinate >= base/start <= end do drift Java já
registrado em BACKLOG-AIR-006. Full desta continuação exit 0: 86 harness, 172 model
+ 57 transporte via check.sh e root clean verify, arquitetura 308/17 classes e
4664/420 arestas. Docs/MANIFEST, Git/scope e diff --check passaram. Sem alteração
do golden, model, writer ou pin. Novo commit/push/CI e review ainda pendentes.
