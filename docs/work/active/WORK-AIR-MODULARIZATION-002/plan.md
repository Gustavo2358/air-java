# Plano — 0C-I

1. Reconciliar merge 0C-D e registrar autorização/base/branch; reconfirmar 0A/0B
   e contratos downstream em leitura. Capturar oracle compilado da baseline real.
2. RED dos gates multi-módulo, implementação de POMs/moves/launcher e script offline.
3. Ownership fechado, grafo efetivo Maven e bytecode; regressões e challenges em
   cópias temporárias; GREEN e segundo GREEN após restauração. Sem recursão.
4. Reactor completo/seletivo, equivalência binária/API e probes atuais lower/CFG
   com cache isolado sem AIR antigo. Documentar pins pendentes sem alterá-los.
5. Docs públicas, manifesto integral, full/git/scope, self-review integral/staged,
   commits focados, push, SHA remoto, CI desse head e PR. Parar no review humano.

Riscos: parent não instalado, POM runtime sem referência, suíte cwd/skip/herança,
classpath misturado e SNAPSHOT stale. Rollback: reverter mudança completa em branch
posterior autorizada, restaurando single-module/GAV e repetindo gates/consumers;
nenhum discard alheio, merge ou atualização de pins nesta sessão.
