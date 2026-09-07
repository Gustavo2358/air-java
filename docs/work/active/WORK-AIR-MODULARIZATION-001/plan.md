# Plano — 0C-D

1. Confirmar merge real do harness; main limpa e sincronizada; branch e SHA base;
   arquivar lifecycle antigo, criar este item antes do discovery executável.
2. Inventariar Maven, packages, layout, bytecode, scripts/CI e contratos; contar
   imports e inspecionar pins/builds de lower/CFG em leitura; verificar 0B remoto.
3. Comparar A/B/C e tentar quebrar compatibilidade em cópias sob /tmp: effective
   POM, reactor, classfiles/JAR, consumo direto/transitivo, parent ausente/colisão.
4. Documentar decisão canônica, plano de gates multi-módulo e evidência/limites.
5. Inverter dois steps; demonstrar RED da ordem antiga e GREEN da nova, sem
   alterar executor/semântica/permissões. Full + scope/git + MANIFEST; self-review.
6. Estado ready_for_review, commit focado, push, SHA remoto, PR e CI desse head.
   Parar no review. Recibo remoto no PR, sem commit autorreferente.

Riscos: GAV duplicado, plugin herdado roda suíte no parent/JSON, dependência
transitiva esconde vazamento, paths de target e lock de consumers deixam de valer.
Rollback da sessão: reverter apenas seu diff revisado. Rollback futuro de 0C-I
será definido no ADR; nunca descartar estado alheio nem alterar outros repos.
