# Fronteiras verificáveis hoje

```text
analysis-ir (normativo, SHA pinado)
        ↓
air-model (artifactId air-java): model ← validation
        ↑                         ↑
air-json (codec 1A)      callers / testes / exemplos
```

`Publication` e `ValidationResult` são valores em memória. Não há necessidade de
introduzir camadas application, ports, repository, DI ou adapters fictícios para
uma biblioteca de valores. A direção existente já é adequada.

O [mapa público](../../ARCHITECTURE.md) explica responsabilidades. O gate standalone
valida os três POMs e os dois owners, compila `air-model/src/main/java` em
diretório temporário e examina todos os classfiles
(Java 21, sem preview) e as referências diretas reportadas por `jdeps`, incluindo
classes aninhadas. Rejeita `model → validation`, packages de produto novos,
dependência não resolvida e bibliotecas externas.

A allowlist JDK fica em [architecture.py](../../scripts/harness/architecture.py):
`java.lang`, suporte de records/lambdas em `java.lang.invoke` e `java.lang.runtime`,
`java.math`, `java.util`, `java.util.function` e `java.util.stream`. `System`,
`Runtime`, `Process`, `ProcessBuilder` e `ServiceLoader` são excluídos. Arquivo,
rede, reflexão explícita, JSON, CLI, frameworks e frontend não entram nela.
Não banir `java.lang.invoke` indiscriminadamente: o compilador o usa para records.

Trata-se de política para o núcleo atual. Novo package JDK legítimo requer revisão
de necessidade e contracaso do gate; o gate não é análise transitiva de toda a JDK,
sandbox de execução ou prova contra acesso dinâmico por `Class`/method handles.
Review continua necessário para dependências ocultas e duplicação semântica.

Os gates não exigem inventário fixo de cada classe Java: variantes futuras podem
crescer dentro da fronteira. O inventário nominal da suíte é separado, com revisão
deliberada ao acrescentar/remover checks. O reactor tem inspeção por módulo
de POM efetivo, grafo Maven e JAR/bytecode em verify. O modelo rejeita qualquer dependência efetiva/resolvida, inclusive runtime
ou optional sem referências Java; JSON permite só a aresta direta no modelo.
[Política 1A](../engineering/modularization-gates.md) exige implementação/suíte JSON com ownership, módulo completo e nenhum output
de produto no root. O codec admite somente a cobertura declarada de 1A.

O JSON tem somente classes adicionais JDK para UTF-8/ByteBuffer em allowlist
exata; não abre java.nio.file, filesystem ou bibliotecas externas. Model e validation
conservam sua allowlist anterior. [Contrato do codec](../engineering/air-json.md).
