# Gates executáveis e limites

Entrypoint estável: `python3 -B scripts/harness/run.py <gate>` a partir de qualquer
diretório. Requer Python 3.10+, Git e JDK 21+ (java/javac/jar/jdeps); Maven para
`maven/full`. Sem pacote Python externo. Compila com release 21, sem preview.

| Gate | Contrato |
| --- | --- |
| docs | links/anchors locais, JSON sem chaves duplicadas, locks, IDs, evals, lifecycle, MANIFEST hashes/cobertura |
| harness | unittest com contracasos documentais, Git, saída de testes e bytecode sintético |
| fast | docs + harness; inclui JDK para contracasos compilados, sem Maven |
| architecture | topologia/POMs fechados + compilação isolada + ownership/classfiles 21/jdeps; JSON vazio explícito |
| semantic | script offline do reactor, confere cada check nominal, ordem, numeração e resumo |
| maven | root clean verify, owners completos, effective POM/grafo/bytecode/JAR e suíte nominal |
| git | branch/base/origin main local e diff no scope do --work explícito |
| scope | mesmo diff/scope sem política de branch; adequado a detached HEAD de CI |
| ci-scope | scope + diff da base do evento GitHub; permite encerramento só documental sem item ativo |
| full | docs + harness + architecture + semantic + maven, uma vez cada, para na primeira falha |
| performance | UNAVAILABLE: não existe gate focalizado de custo N/2N |
| transport | UNAVAILABLE: codec ainda não implementado |
| integration | UNAVAILABLE: equivalência arquivo/memória e E2E futuros |

[Estado estruturado](gates.json) é conferido contra executores. Exit codes: 0 PASS,
1 FAIL, 2 erro de ambiente/uso/metadados inválidos, 3 UNAVAILABLE. Agregador registra
etapas não executadas; não permite skip de gate obrigatório. `full` é perfil da
biblioteca atual, não conclusão do roadmap ou certificação AIR. Novo perfil precisa
adicionar gates explicitamente e provar seus contracasos antes de alegar proteção.

Exemplos:

```sh
python3 -B scripts/harness/run.py fast
python3 -B scripts/harness/run.py architecture
python3 -B scripts/harness/run.py semantic
python3 -B scripts/harness/run.py full
python3 -B scripts/harness/run.py git --work WORK-AIR-HARNESS-001
```

`AIR_MAVEN_REPO` escolhe diretório absoluto de cache isolado fora de target; default
`/tmp/air-java-harness-m2`. Maven pode precisar de rede para plugins. Nenhum gate faz
install/deploy, altera source lock ou baixa repositórios irmãos. Arquitetura e testes
do harness usam TemporaryDirectory; scripts/check.sh e Maven escrevem target.

Cada invocação salva `target/harness/<gate>.json`: HEAD observado (não identidade do
diff sujo), timestamp operacional, resultados, detalhe, exit code e etapas não
executadas. Maven clean remove resultados anteriores em target; full grava seu
relatório agregado depois da execução. Os relatórios não são certificados humanos.

CI adicional em [harness.yml](../../.github/workflows/harness.yml) roda ci-scope antes de full
no SHA checked out: autorização de diff falha antes de compilação/suíte/Maven.
Os dois steps são obrigatórios, sem continue-on-error ou condição de skip; falha
do scope impede full. O oracle em test_execution.py protege ordem e obrigatoriedade.
CI exige um item ativo explícito; sem ativo, aceita somente
encerramento documental dos itens da base do evento, com registro histórico e
ancestralidade do merge. Essa exceção não admite código, POM ou source lock.
O workflow Java contract checks executa check.sh e Maven, adaptados ao reactor.
Ambos os workflows configuram explicitamente Python 3.12: o build Maven requer
python3 já em validate, além dos gates do harness. Sem path filter que ignore novos arquivos,
sem permissões de escrita, publicação ou merge. Scope não presume branch em CI.

Limites: links externos não são consultados; Markdown suportado usa links inline ou
definições simples e headings, sem parser completo CommonMark. Docs não compreende
prosa, prova autenticidade da autorização ou estado remoto. jdeps inspeciona
referências diretas e não é análise de reflexão dinâmica. O inventário de testes
não prova as assertions. Review humano e oráculos de domínio continuam necessários.

Detalhes da implementação e contracasos por owner em
[modularization-gates](modularization-gates.md). `air-model` produz `air-java`;
`air-json` vazio é verificado, sem declarar transporte disponível.
