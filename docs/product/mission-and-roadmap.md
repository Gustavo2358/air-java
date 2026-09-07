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

O harness e o discovery 0C-D foram mergeados. A Opção B do
[ADR-0002](../architecture/decisions/ADR-0002.md) foi aprovada; o
[item atual](../work/index.md) implementa 0C-I por autorização explícita:
parent air-java-parent, diretório air-model com artifactId air-java e validation,
mais air-json vazio com dependência direta no modelo. A entrega para review e as
[evidências](../quality/modularization-implementation.md) não significam merge do 0C-I.

0A/analysis-cfg e 0B/analysis-ir foram reconfirmados como mergeados. O pin normativo
local permanece 122ce54e1b9ef9b00646f93ece409ca8b63bc933; 0B também usa esse pin.
Codec/round-trip/E2E/1A continuam não iniciados e exigem autorização própria.
O [backlog](../work/backlog.md) mantém essas fronteiras.
