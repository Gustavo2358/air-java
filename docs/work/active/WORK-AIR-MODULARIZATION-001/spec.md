# Spec — 0C-D

Problema: um único JAR precisa futuramente hospedar model/validation e codec em
módulos separados sem quebrar os consumidores. Resultado observável: veredito
obrigatório A/B/C, coordenadas, destino de validation, matriz de impacto contado,
plano executável 0C-I, gates por bytecode, riscos e rollback.

Entradas: POM/fontes/gates atuais, POM/imports/pins de lower e CFG, autoridade e
binding DRAFT do analysis-ir, roadmap externo identificado por hash. Classes:
consumo Maven direto/transitivo, pin de fonte, parent/aggregator, dependência
inversa ou oculta, suíte omitida, módulo vazio. Incerteza exige probe ou limite.

Invariantes INV-AIR-001–007 preservados. Transporte não redefine semântica;
validation → model; JSON → modelo; modelo sem JSON/I/O/rede. Packages públicos
preservados por padrão. Única alteração executável: ordem ci-scope antes de full
na CI e regressão estritamente necessária. Sem semântica nova de ci-scope.

Fora de escopo: produto/POM/source moves, reactor permanente, codec, libs JSON,
source lock, outros repos, binding, E2E, publicação/release, merge, 0C-I e 1A.
