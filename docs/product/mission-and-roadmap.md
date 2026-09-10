# Missão e evolução do air-java

`analysis-ir` define o contrato; `air-java` implementa valores Java imutáveis e
validação estrutural compartilhada; `cobol-lower` produz `Publication` a partir do
Semantic Product; `analysis-cfg` a consome. Este repo não reanalisa COBOL nem calcula CFG.

Hoje existe um JAR `io.github.gustavo2358:air-java:0.1.0-SNAPSHOT`, Java 21,
packages `model` e `validation`, sem dependências de runtime externas. A versão
AIR é 2.0.0. [Status atual](../implementation-status.md) delimita o Validator.

O roadmap externo de 7 de setembro de 2026 foi identificado por hash e seções na
[baseline](../sources/baseline.md). Ele prevê:

1. **0C-D:** discovery com veredito sobre topologia Maven, coordenadas, packages,
   consumo downstream, migração, riscos, rollback e gates. Reconfirmar origins.
2. **0C-I:** após a decisão humana do discovery, modularizar modelo e codec,
   preservando compatibilidade conforme o veredito. Estender gates ao reactor real.
3. **1A:** codec AIR JSON compartilhado, dependente do modelo, implementando draft
   pinado por 0B; erros de transporte separados de validação semântica.
4. Integração com lower/CFG, primeiro E2E GOBACK e depois vertical slices.

O harness, 0C-D e 0C-I foram mergeados. A Opção B do
[ADR-0002](../architecture/decisions/ADR-0002.md) está implementada; merge real
0C-I: 71937dfe88bac4dae10f6f195731acac638c2d29, PR #4.
O [item atual](../work/index.md) implementa 1A por autorização explícita:
codec compartilhado apenas em air-json, com [cobertura e limites](../engineering/air-json.md).

0B mergeado no analysis-ir foi reconfirmado em 51b4d9a8ae0364232bd97103cd73a77e1a34996c.
Norma e binding permanecem no pin 51b4d9a8ae0364232bd97103cd73a77e1a34996c, DRAFT.
O PR 1A não significa integração/2A/2B/E2E nem promoção normativa.
O [backlog](../work/backlog.md) mantém essas fronteiras.
