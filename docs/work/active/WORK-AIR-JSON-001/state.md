# Estado — WORK-AIR-JSON-001

## Onde estamos

Implementação 1A preparada para review na branch feat/air-json-codec-mvp.
Baseline main limpa sincronizada: 71937dfe88bac4dae10f6f195731acac638c2d29,
PR #4 / 0C-I confirmado MERGED por gh direto às 2026-09-07T18:08:56Z e por ls-remote.
Lifecycle anterior arquivado após confirmação real; autorização implementation
registrada antes do produto. Codec/API/testes somente em air-json; modelo intacto.

## Verde conhecido

0B PR #3 confirmado MERGED em 51b4d9a8ae0364232bd97103cd73a77e1a34996c;
handoff lido nesse merge por API. Normativos/binding resolvidos só no pin
122ce54e1b9ef9b00646f93ece409ca8b63bc933; analysis-ir-json / 1.0.0 / AIR 2.0.0 DRAFT.
Nenhuma dependência externa adicionada; grafos model vazio e JSON → air-java compile.

Golden manual anterior ao encoder + Publication oracle independente; encode/decode
exatos, round-trips, PARTIAL/evidence/scopes/ordem, UTF-8/Unicode e negativos.
Full local exit 0: docs/MANIFEST, 86 harness tests, 308 model/16 JSON classfiles,
4664/408 arestas, 172 model + 43 transport checks via check.sh e root clean verify.
12 mutações do codec e 6 de gate RED, restauração byte a byte e segundo GREEN.
Maven real com skipTests=true foi RED mesmo após build GREEN com logs presentes.
[Evidência](../../../quality/air-json-implementation.md) e
[recibo de challenges](../../../quality/air-json-challenges.json).

## Restante

Git/scope/MANIFEST passaram e o diff integral/staged recebeu self-review.
Restam commit/push autorizados e confirmação de SHA remoto e CI do head no PR;
parar para review humano. Recibo remoto fica no PR, sem gravar SHA futuro ou CI
ainda não executada no próprio commit.
Sem merge/auto-merge, publicação Maven remota, integração lower/CFG, 2A/2B ou E2E.

## Descobertas que afetam o plano

Primeira consulta de PR #4 retornou OPEN; consulta direta subsequente resolveu
estado MERGED real antes de iniciar produto. Nenhum blocker de merge permanece.
Challenge encontrou duplicata com null mascarando ausência da guarda por erro de
tipo posterior: contracaso corrigido para duplicar mesmo valor válido e exigir
regra de duplicatas, incluindo chave escapada. Mutação então ficou RED.

JSON sem biblioteca externa mantém check.sh offline; parser/canonical writer explícitos
exigem manter contracasos. Tetos configuráveis de bytes/profundidade são operacionais.
Formas fora da cobertura falham; Base64/decimal/literais e catálogo ampliado DEFERRED.
Namespace/IDs/sourceKeys do golden são manuais/opacos; não simula IDs do lower.
Nenhuma segunda implementação, qualificação §13 inteira ou promoção DRAFT alegada.
