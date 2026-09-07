# Estado — WORK-AIR-MODULARIZATION-002

## Onde estamos

Implementação 0C-I pronta para review: root air-java-parent:pom, air-model produz
GAV air-java preservado com model + validation, air-json vazio depende dele.
Branch feat/air-java-modularization, base main limpa sincronizada
59120faebfe8381df445fd6ea74822bb83f08554. Work anterior arquivado após merge real
PR #3 / 0C-D em 2026-09-07T16:44:20Z. Nenhum codec, lib JSON ou 1A iniciado.

## Verde conhecido

0B MERGED 51b4d9a8ae0364232bd97103cd73a77e1a34996c e
0A MERGED 141b8270f54558a24ee561281598e53c48a0ff6b reconfirmados no GitHub.
Source lock preservado; 42 fontes/testes movidos byte a byte e hashes do MANIFEST
mantidos nos novos paths. Comparação dos 308 classfiles reais: todos idênticos,
major 65/minor 0, mesmos packages/API/jdeps (4664 referências).
Full: docs/manifesto, 80 testes harness, architecture, 172 checks offline e
172 via Maven clean verify, exit 0. Root parent/model/json; seleção -pl air-model
-am verify e offline de cwd /tmp passaram. Exemplo público compilou/executou.
Git/scope explícitos PASS, 130 paths autorizados (ambos os lados dos moves).
MANIFEST cobre 126 arquivos, sem target/caches, preservando 42 hashes movidos.
Lower main: 540 + 239 checks; CFG main pós-0A: 102 testes, 0 falhas/erros/skips;
POMs/imports intactos em snapshots isolados e novo reactor instalado em cache
sem AIR anterior. Diff integral e dependências em self-review, sem alegar revisão
independente. Evidências e limites em [0C-I](../../../quality/modularization-implementation.md).

## Restante

Conferência final do staged diff e gates, commit/push, confirmação do SHA remoto,
CI desse head e PR contra main. Recibo pós-commit fica no PR, sem autoinscrição
de SHA futuro/CI no próprio commit. Parar no review humano; não fazer merge ou
iniciar 1A. Adoção de pins/proveniência lower/CFG pertence a work items downstream.

## Descobertas que afetam o plano

Jackson runtime sem bytecode reference: POM literal, effective POM e grafo
resolvido agora recusam o contracaso real. O Maven mantém skipTests=false na
propriedade do effective POM mesmo quando o launcher interpola true por CLI;
o primeiro challenge expôs log stale aceito. Corrigido com flags resolvidos em
scanner não ignorável e validação da configuração efetiva da suíte. Quatro flags
skip rejeitados por Maven real; restauração clean verify novamente GREEN.
Limites: forma Maven fechada em 0C-I, parent necessário no cache, SNAPSHOT exige
proveniência; jdeps não prova acesso dinâmico ou semântica AIR. Nenhuma mudança
necessária ao domínio aprovado e nenhum checkout irmão alterado.
