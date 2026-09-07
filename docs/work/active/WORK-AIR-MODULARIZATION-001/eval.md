# Eval — 0C-D

Oráculos: POM e Git reais; inventário de imports por arquivo; inventário estático
172 checks; bytecode/JAR compilados isoladamente; resolução Maven fora do reactor.
EVAL-AIR-001–006 aplicáveis. Novo desenho não alega gate de transporte disponível.

Positivos: GAV atual resolve model e validator na opção B; classes equivalentes;
JSON depende só do modelo no scaffold; consumidores continuam compilando sem
renomear imports. Negativos: root/child GAV iguais, parent omitido no consumo,
modelo com dependência JSON mesmo sem referência, módulo ignorado, suíte omitida.
Os últimos cenários não executados serão explicitamente contratos de 0C-I.

Workflow: oracle da ordem e obrigatoriedade, RED com workflow antigo, GREEN com
ci-scope → full; challenge com ordem invertida/step removido ou condicional.
Full real, scope/git explícitos e integridade MANIFEST; revisão de todo diff e
prova de src/POM/locks preservados. CI real do SHA publicado é evidência separada.
Não executar produto em checkout irmão; probes usam /tmp. Não alegar E2E ou
suite downstream completa se somente compile/resolução foi medida.
