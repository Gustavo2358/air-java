# Estado — WORK-AIR-MODULARIZATION-001

## Onde estamos

Discovery documentado em ADR-0002: recomendo B, modelo/validation no JAR air-java,
parent distinto air-java-parent e futuro air-json. Base c487b80dd5548236de9114d8b3ec370eabb25c30;
branch discovery/air-java-modularization. Lifecycle anterior arquivado com merge
real do PR #2; 0B PR #3 ainda aberto no snapshot observado. Workflow invertido;
entrega local pronta para review.

## Verde conhecido

Full final: docs, 56 testes harness; bytecode 308 classes/4664 referências;
172 checks offline e 172 Maven clean verify, exit 0. Git e scope explícitos PASS.
Self-review do diff completo e staged concluído; imports conferidos por arquivo,
MANIFEST íntegro cobrindo 116 paths. Produto e quatro checkouts irmãos intactos.
Workflow RED da ordem antiga, GREEN e cinco mutações rejeitadas. Probes em /tmp:
B passa 172 checks e preserva bytes dos 308 classfiles; lower 540+239 checks e CFG
82 testes com POMs/fontes originais. Parent ausente e GAV duplicado falham pela
causa correta; restauração passa. A constrói mas perde o JAR que os callers usam.
Rollback single-module reinstalado e consumer verde. Evidência canônica em
[modularization-discovery](../../../quality/modularization-discovery.md).

## Restante

Commit/push e confirmação de SHA remoto, PR e CI desse head são verificações
pós-commit; seus recibos ficam no PR, sem commit autorreferente. Parar no review humano;
nenhum próximo checkpoint autorizado, sem merge, 0C-I, 1A ou release.

## Descobertas que afetam o plano

exec:java não muda cwd no reactor: dois scans da suíte quebram; fork com cwd do
model resolveu no probe. Jackson runtime sem uso no bytecode passa no gate atual:
0C-I precisa inspecionar grafo efetivo/CP além do bytecode. Root/child GAV idênticos
são rejeitados por Maven. Consumer B exige parent POM instalado. Hash do JAR muda
pelo POM embutido, não pelos classfiles. Caches/pins downstream precisam continuar
provando fonte, mesmo com versão SNAPSHOT preservada. Nada disso foi implementado
no produto; source lock e repos irmãos permanecem intactos.
