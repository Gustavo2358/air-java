# Plano

1. Confirmar merges e baseline limpa; reconciliar 0C-I e registrar autorização.
2. Resolver binding/norma no pin; decidir parsing/canonical writing e fronteiras.
3. Golden manual e Publication oracle independentes do encoder; obter RED.
4. Implementar mapeamento explícito em air-json; materialização seguida de Validator.
5. Substituir política JSON vazia por ownership, bytecode, grafo e suíte obrigatórios.
6. GREEN, challenges reais com hashes antes/depois, segundo GREEN; docs e limites.
7. Full/check.sh/root clean verify, Git/scope/MANIFEST; self-review integral/staged,
   commits focalizados, push, CI do head e PR humano. Não iniciar integração.

Riscos: duplicata escapada, UTF-8/surrogate, limites numéricos, perda de evidence,
fortalecimento de PARTIAL e defaults, ordem física, suite omitida/stale.
Algoritmo: árvore JSON fechada com parsing linear, arrays preservados, escrita de
propriedades ordenada por escalares, mapeamento de fatos explícito e índices do
Validator. Limites operacionais explícitos de documento/profundidade; sem truncar.
Rollback: reverter mudanças deste checkpoint em trabalho autorizado, mantendo
modelo/consumers e pin; nenhuma remoção de alterações alheias.
