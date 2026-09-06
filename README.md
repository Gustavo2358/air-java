# air-java

Modelo Java imutável e validação estática para **Analysis IR 2.0.0**.
A autoridade semântica é o repositório `Gustavo2358/analysis-ir`, fixado no commit
`0b2fbce7046010b22b32efa8cbc3e75ccba09442`. Esta biblioteca implementa uma
representação Java; não substitui nem modifica aquela especificação.

**JDK:** 21 ou superior, compilando com `--release 21`. **Coordenadas Maven:**
`io.github.gustavo2358:air-java:0.1.0-SNAPSHOT`. A versão da biblioteca não é a
versão semântica da AIR. A API inicial ainda deve passar por review antes de ser
estabilizada como release pública.

## Fronteira

```text
analysis-ir (especificação normativa; sem código)
                     |
                  air-java
           modelo + validação estática
              /                  \
     cobol-lowering          analysis-cfg
```

`src/main` depende **somente de java.base**. Não contém Jackson, Gson, JSON,
acesso a arquivos, CLI, COBOL, parser, CFG builder, resolução de nomes, reaching
definitions ou avaliação de valores. Nenhum campo do modelo é `Map<String,Object>`.

O lowerer retorna `Publication`; o CFG recebe esse mesmo tipo. O driver pode
chamar um adapter JSON de saída/entrada sem mudar o core. A biblioteca não cria
`Publish`/`Repository`/`MemoryReader` artificiais só para transportar um objeto.

## Conteúdo

- `model`: identidades tipadas, publicação/unidades/entradas/sequências, todas as
  variantes de operações core desta entrega, operandos, tipos/valores, memória,
  origens, gaps, cobertura, precisão e premissas `sameDomain` com escopos estáticos.
- `validation`: índices por identidade, assinaturas, integridade, verificações de
  tipo, prova finita de domínio comum e diagnósticos de limites/obrigações.
- `src/test`: suíte determinística sem dependência de framework de teste.
- `examples/MinimalPublication.java`: construção e validação executável de uma
  publicação sem transporte nem frontend.
- `docs/implementation-status.md`: cobertura real e limites do validador.
- `docs/model-catalog.md`: catálogo de tipos e campos desta implementação.
- `handoff/json-v1.md`: **proposta de contrato de transporte**, para mover para
  `analysis-ir/bindings/json-v1.md`; não é codec nem dependência da biblioteca.
  `handoff/` está ignorado pelo Git para evitar publicação acidental neste repo.

## Compilar e testar

Sem Maven, sem rede e sem bibliotecas de teste:

```sh
./scripts/check.sh
java -cp target/air-java-0.1.0-SNAPSHOT.jar examples/MinimalPublication.java
```

O script compila código e testes com warnings tratados como erros, executa a
suíte e inspeciona o JAR com `jdeps`: somente `java.base` é permitido.

Com Maven:

```sh
mvn verify
mvn install
```

A suíte `ContractSuite` é executada na fase `test` pelo `exec-maven-plugin`.
Ela usa verificações explícitas, não depende de `assert` habilitado nem de JUnit.
Não confundir eventual relatório vazio do Surefire com ausência desses testes:
a linha `PASS: ... deterministic contract checks` vem do runner obrigatório.
`-DskipTests=true` pula a suíte de forma explícita. Plugins Maven podem precisar
de download na primeira execução; o domínio não recebe essas dependências.

O build Maven está fornecido, mas **não foi executado no ambiente de geração**, que
não tinha Maven. O gate JDK, o exemplo e a inspeção de dependências foram executados;
veja `docs/validation-evidence.md` para a evidência exata.

## Consumir

```xml
<dependency>
  <groupId>io.github.gustavo2358</groupId>
  <artifactId>air-java</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```java
Publication publication = lowerer.lower(semanticInput);
ValidationResult check = AirValidator.validate(publication);
if (!check.isStructurallyValid()) {
    // INVALID_IR ou INCOMPLETE_VALIDATION: propagar diagnóstico; não reparar por texto.
    throw new IllegalStateException(check.toString());
}
// Obrigações semânticas ainda pertencem ao produtor; não são certificadas pelo check.
CfgBuildResult cfg = cfgBuilder.build(publication, options);
```

Os nomes `lowerer`, `cfgBuilder` e `CfgBuildResult` ilustram os outros projetos;
eles **não são implementados por esta biblioteca**.

## Contrato de validação

Construtores verificam forma local: campos obrigatórios, listas imutáveis,
intervalos/valores locais etc. Referências cruzadas e provas exigem
`AirValidator.validate(publication)`. Uma instância construível não é garantia
de AIR válida. O validador não modifica a publicação.

`STRUCTURALLY_VALID` significa aprovação dos checks estáticos implementados.
`INVALID_IR` identifica contradição detectada. `INCOMPLETE_VALIDATION` indica
versão/capacidade não interpretada, limite de recursos ou precondição que este
validador ainda não consegue verificar. **Nenhum desses nomes é certificação
integral de perfil `AIR-STRUCTURE@2` ou `AIR-SCALAR-FLOW@2`.**

A veracidade de uma premissa, a equivalência de lowering com uma linguagem,
a pureza de uma abstração e os resultados de CFG/dataflow exigem suas próprias
provas/oráculos. `SEMANTIC_OBLIGATION` mantém essa distinção visível.

## Decisões de representação

As listas são snapshots imutáveis. `ObjectId` não é `StorageId`.
`Sequence` contém instruções e exatamente um `Terminator` separado por tipo.
`TypeRef` não é um tipo universal; compartilhar `UncertaintyId` não prova
compatibilidade. Não há inferência de valores pela identidade de expressões.

A notação `sameDomain` é validada sobre identidades e premissas de escopo estático:
publicação, unidade, entrada, operação, invocação e interseção. Cópias não ganham
conversão/padding implícitos. `opaque`, efeitos externos e controles abertos
carregam envelopes explícitos. As extensões padronizadas têm variantes próprias
com fallback; não são executadas por esta biblioteca.

## Antes de fazer push

Copie **o conteúdo** desta pasta para a raiz do novo repositório, preservando
arquivos ocultos. Mova `handoff/json-v1.md` para o repositório da especificação,
revise a proposta lá e remova `handoff/` daqui. Execute o gate no seu ambiente,
revise o diff e faça commit. Este pacote não contém `.git`, não faz push nem
publica artefatos automaticamente.
