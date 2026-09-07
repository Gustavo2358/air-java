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

Somente a criação do harness está em execução. Não há módulo `air-json`, codec,
round-trip ou E2E entregue por este trabalho. Não escolher coordenadas futuras antes
do discovery. O [backlog local](../work/backlog.md) registra dependências sem ativá-las.

README/ARCHITECTURE atuais descrevem adapters externos ao domínio. O plano futuro
permite módulo irmão no mesmo repo, mantendo isolamento do núcleo. Essa evolução
será reconciliada nos documentos públicos pelo checkpoint que mudar a topologia;
este harness não reescreve o produto antecipadamente.
