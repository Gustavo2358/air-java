# Cobertura implementada e limites

Baseline: AIR 2.0.0, commit `0b2fbce7046010b22b32efa8cbc3e75ccba09442`.
A biblioteca 0.1.0-SNAPSHOT é uma implementação inicial revisável. **Não declara
conformidade integral de Producer, Validator ou Consumer com todos os perfis AIR.**

## Modelo materializado

| Área normativa | Representação Java |
| --- | --- |
| Publicação/identidades | `Publication`, `Unit`, `Sequence`, `Ids`, `Entries` |
| Conhecimento de tipo | `Types.Known`, `Types.UnknownType`, `Types.ExtensionType` |
| Expressões/locais | `Expression`, `Expressions`, `Place`, `Places`, `Operand` |
| Valores | bool/int/decimal/text/bytes/label; inteiros arbitrários e bytes imutáveis |
| Armazenamento | célula, região, vista, alias, alternativas, associação desconhecida |
| Operações comuns | assign, havoc.must, havoc.may, nop, copy_bytes |
| Terminadores core | jump, branch, dispatch, invoke, return, raise, halt, opaque |
| Extensões padronizadas | local.invoke/boundary/resume/unwind e indirect.jump, com fallback |
| Incompletude | cinco dimensões de precisão, cobertura, incertezas e três envelopes |
| Origem | escrita, derivada, contratual e indisponível; coordenadas com unidade/base |
| Provas de domínio | sujeitos de objeto/célula/operando/assinatura e escopos fechados |
| Relações estruturais | artefatos, recursos e relações sem ponto de execução fictício |

`contracts` é uma forma Java explícita de materializar conteúdo de contratos externos;
não autoriza consulta preguiçosa. `ContractId`/`RelationId` dão identidades àquelas
publicações. Esses detalhes de organização não ampliam a semântica AIR.

## Checks estáticos implementados

Identidades completas/únicas; fechamento de referências; ownership de operações e
operandos; terminação por tipo; entradas e labels; visibilidade explícita; ciclos de
origem/alias/contenção; assinaturas ordenadas; tipo de operações; tipos de codec;
intervalos constantes; resultados somente quando existe retorno normal; alternativas
e tags de invocação; domínios de labels; fechamento de gaps/proveniência/coverage;
razões distintas para tipo e valor desconhecidos; capacidades requeridas.

`sameDomain` verifica bases de domínio conhecido, identidade, célula/alias exato,
leitura, premissas, composição e escopos. Os escopos não usam CFG nem ativações.
Uma premissa universal sobre um `choice` cobre candidatos e restante; provas
somente de candidatos não bastam para uma escolha aberta. Contradições conhecidas
são rejeitadas. A relação não altera os `TypeRef`, não prova valores iguais e não
libera aritmética/comparação de domínio desconhecido.

Os checks correspondem a partes verificáveis de I-01–I-13, I-17/I-18, I-20,
I-23/I-26, I-28–I-32, I-36/I-43/I-46 e I-49–I-53. Os identificadores nos diagnósticos
identificam a obrigação afetada; **não significam que todo esse invariant foi
certificado em todas as suas dimensões semânticas**.

## O que permanece explicitamente fora

1. **Preservação da linguagem de origem e verdade das premissas.** Pureza,
   ausência de conversão, independência física, totalidade e bounds contratuais
   precisam de autoridade do produtor. O validador registra `SEMANTIC_OBLIGATION`.
2. **Resultados de análise.** Nenhum oracle que exige CFG, matching de retornos
   em execução, efeitos calculados, strong/weak update, RD, possible values ou
   dependências finais é implementado por esta biblioteca.
3. **Todos os casos de memória/conversão.** Faixas simbólicas, validade de codec
   sobre conteúdo não literal, inicializações conflitantes em vistas parcialmente
   sobrepostas e domínio do restante em associações de storage abertas não são
   decididos integralmente. As formas pertinentes geram `INCOMPLETE_VALIDATION`
   quando falta evidência aplicável; contradição constante detectável gera `INVALID_IR`.
4. **Extensões arbitrárias.** Não existe payload livre semanticamente interpretado.
   Tipo/codec de extensão desconhecida não vira `TYPE_UNKNOWN`; permanece nomeado
   com incompatibilidade explícita. Literais/operações precisas de extensões próprias
   exigem representação e validador especializados antes de uma nova release.
5. **Limites superiores de envelopes e claims globais.** O código verifica forma,
   referências e assinaturas, não prova que limites declarados incluem todos os
   comportamentos do produtor nem calcula uma síntese global de precisão.
6. **Cobertura de entrada.** A biblioteca verifica os itens publicados, mas não
   conhece a entrada do lowerer para provar que nenhum statement-fonte foi omitido.
   Essa bijeção/correlação exige oráculo bilateral no `cobol-lowering`.
7. **Transporte.** JSON, schema checker, reader/writer e compatibilidade de bytes
   pertencem aos adapters. `handoff/json-v1.md` é proposta documental, não código.

## Decisões adicionais da representação Java

`Optional<PremiseId>` em acessos/recortes permite apontar a evidência de precondições
sem inventar análise local. `SafetyAssertion` explicita a alegação e seu site; é uma
forma de premissa rastreável da implementação, não uma prova da verdade externa.

Um `Entry` de corpo disponível precisa de label. Unidade sem corpo pode permanecer
sem entradas e ser referida como recurso/contrato externo. Formas de assinatura
sem corpo devem ser revisadas à luz do contrato antes de ganhar garantias adicionais.

`ReferenceChecks`, `TypeResolver` e `DomainProofEngine` são internos ao validator;
não constituem uma API alternativa ao modelo. Diagnósticos não são inseridos na AIR
recebida. O caller decide se rejeita, reporta limitação ou usa outra implementação
para verificar uma publicação que este slice não conseguiu certificar.

## Próximos slices recomendados

Review do modelo e desta matriz; aprovação do binding em `analysis-ir`; adapters
externos e prova arquivo/memória; corpus de conformidade normativo ampliado; checks
estáticos adicionais de memória e contratos. Só promover versionamento estável e
claims de perfil depois dos oráculos correspondentes, sem tornar JSON dependência
do modelo nem mover regras específicas de COBOL para esta biblioteca.
