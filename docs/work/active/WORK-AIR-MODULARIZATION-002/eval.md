# Eval — 0C-I

Oracle: baseline 59120faebfe8381df445fd6ea74822bb83f08554, mesmos JDK/flags,
inventário integral de classfiles/bytes/Java major-minor/API pública/dependências,
fontes e inventário nominal ContractSuite preservados. EVAL-AIR-001–006.

GREEN: model/validation, validation → model, air-json → air-java vazio explícito.
RED: model → validation/air-json/Jackson/Gson/I/O/rede/reflexão/processos;
runtime Jackson sem uso Java (POM efetivo + árvore), cycle, terceiro/missing módulo,
root sources/classes, classes copiadas para JSON, classfile inesperado/ausente,
primeiro código JSON, suíte ausente/duplicada/reordenada/incompleta/skip.

Standalone offline + Maven compiled, root clean verify e -pl air-model -am verify.
Compatibilidade via JARs comparados e lower/CFG main exportados sob /tmp com POMs
/imports intactos e cache sem AIR anterior. MANIFEST, full, git e scope explícitos;
self-review não é review independente; CI do SHA publicado exige evidência remota.
Limites: sem transporte, E2E ou atualização de source lock/pins downstream.
