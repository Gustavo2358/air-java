# AGENTS.md

## Autoridade e escopo

`docs/sources.lock.json` fixa a versão normativa em `Gustavo2358/analysis-ir`.
Comece por README e `docs/implementation-status.md`; carregue a seção normativa
necessária à mudança. Não use esta implementação como autoridade para reescrever AIR.

Esta biblioteca compartilha modelo e validação entre produtores/consumidores.
Não adicionar JSON/Jackson/Gson, arquivo, rede, framework, COBOL, CFG, effects,
RD ou values ao domínio. Adapters vivem fora daqui. `handoff/json-v1.md` é um
handoff temporário de especificação, não código de runtime.

## Regras

- Preservar imutabilidade transitiva, identidades completas e versões distintas.
- `unknown_type` não é wildcard; incerteza compartilhada não prova `sameDomain`.
- Não escolher candidatos, inferir storage independente, inserir zero, normalizar
  nomes externos ou criar fallthrough intersequence por conveniência.
- Novas variantes exigem atualizar traversal, validator, testes e catálogo.
- Verificador estrutural não certifica veracidade de premissas nem conformidade
  de consumidores. Checks não implementados precisam de limite explícito.
- Capacidade limita formas, não cardinalidade. Indexar joins; não introduzir scans
  repetidos sobre todo inventário ou todas as premissas para cada operação.
- Testes negativos devem falhar por regra identificável. Expected não deriva de
  saída do próprio validator sob teste. Preservar falsificações como regressão.

## Execução e handoff

Rode `./scripts/check.sh` e, com Maven disponível, `mvn verify`. Reporte o que foi
realmente executado. Antes do commit, revise o diff, dependências, estado do Git e
escopo. Preserve mudanças alheias; não faça reset/stash/discard silencioso.
Não faça merge ou publicação Maven sem autorização.
