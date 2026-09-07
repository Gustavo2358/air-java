# Eval — evidência exigida

Baseline: 172 checks via script original e Maven; docs existentes preservadas.
Harness válido aceita a árvore atual e rejeita mutações pela causa identificável.

Contracasos: link e anchor quebrados, duplicate JSON, ID ausente, lifecycle/registry
incoerente, completed em active, scope de produto, path externo, saída sem testes,
check faltante/duplicado, resumo incorreto, bytecode com dependência inversa,
filesystem, rede ou JSON, major/preview inválido, classe inesperada, diff fora de
escopo (committed/staged/unstaged/untracked e rename).

Positivos: valores JDK e record/lambda support, validation → model, modelo sem I/O,
metadados válidos, Git no scope e todos os 172 checks reais. Contracasos não alteram
src do checkout; cópias isoladas são descartadas ao terminar. Reexecutar GREEN após
correções do próprio harness.

Gates: full e git, integridade MANIFEST e diff final. CI anterior + harness no head
publicado, sem alegar aprovação humana. Não executar suítes de outros repos,
round-trip/E2E inexistente ou falsificações novas no Validator de produção.
