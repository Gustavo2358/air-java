# Arquitetura e fronteiras

## Autoridade

```text
Gustavo2358/analysis-ir
        ↓ especificação normativa
air-java
        ↓ modelo Java compartilhado
produtores e consumidores
```

Uma classe ou conveniência Java não pode criar semântica ausente da AIR. Records,
sealed interfaces, `Optional`, coleções e índices são representação. A versão
normativa consultada está em `docs/sources.lock.json`.

## Topologia física

O root produz `air-java-parent:pom` e agrega `air-model` e `air-json`.
`air-model` preserva `io.github.gustavo2358:air-java:0.1.0-SNAPSHOT` e contém
`io.github.gustavo2358.air.model` e `io.github.gustavo2358.air.validation`.
Validation permanece no mesmo JAR do modelo; não existe terceiro módulo.
`air-json:0.1.0-SNAPSHOT` depende diretamente de `air-java` e está vazio em 0C-I.
O primeiro código de transporte exige política/testes de 1A. O parent centraliza
Java/encoding/plugins, sem dependencies herdadas. [ADR-0002](docs/architecture/decisions/ADR-0002.md).

## Dependências

`model` não depende de `validation`; `validation` depende de `model`. Ambos usam
somente JDK/`java.base`. Testes e exemplos dependem dos dois.

Não há porta de infraestrutura obrigatória: `Publication` e `ValidationResult`
são valores entregues ao caller. O futuro codec JSON pertence ao módulo irmão
air-json. Arquivo, rede, CLI e frameworks permanecem fora do núcleo e sem
autorização antecipada no codec. O binding JSON da AIR continua DRAFT e não
define o runtime desta biblioteca.

Uma integração possível é:

```text
entrada semântica → produtor → Publication → consumidor
                         ↘ adapter externo opcional ↗
```

## Modelo compartilhado

O modelo preserva identidade completa e ocorrências de operandos. Targets de
execução não são resolvidos por inventário: são internal por `EntryId`, literal ou
computed. `ResourceId` descreve recurso declarado. Da mesma forma, `ContractRef`
é evidência de autoridade/versão, não chave de lookup. Assinatura externa, effects
e outcomes ficam na própria ocorrência de `invoke`.

`InvocationOutcomes` e `ControlEnvelope` são somas distintas. O primeiro tem
unicidade própria de chamada; o segundo representa também `jump`, `return` e,
somente no fallback de operação comum autorizado, `continue`. Terminadores nunca
ganham fallthrough pela ordem de sequências.

## Validator

O Validator constrói índices tipados uma vez. A resolução de `sameDomain` usa
união de classes de domínio e overlays por escopo estático, sem unificar lacunas
de tipo. Subjects de chamadas carregam o `OperationId` do site; evidência de uma
chamada não se transfere a outra por autoridade ou assinatura igual.

O cálculo verifica requisitos estruturais; não é lookup nominal, storage analysis,
CFG, effects analysis, reaching definitions ou possible values. `disjoint_storage`
é checado quanto a forma/fechamento, mas sua verdade física permanece obrigação da
autoridade. Envelopes são validados, não executados.

Limites operacionais de profundidade, entidades e diagnósticos pertencem ao
Validator e produzem `INCOMPLETE_VALIDATION`. Eles não truncam inventários nem
mudam naturais AIR em `int`/`long` semântico.

## Extensões

As capacidades padronizadas existentes são tipadas. Nome, versão e manifesto de
uma extensão governam tipos, codecs e políticas especializadas; uma autoridade de
chamada não fornece automaticamente normalização, codec ou igualdade.

Não existe payload semântico livre. Nova variante exige autoridade normativa,
capacidade/versionamento, traversal, validação, testes e catálogo atualizados.
