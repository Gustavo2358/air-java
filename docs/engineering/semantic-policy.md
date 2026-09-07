# Política para mudanças semânticas

Antes de código não trivial, registrar no work item: regra AIR e SHA, domínio de
entradas, classes positivas/negativas/ambíguas, premissas, algoritmo geral,
terminação, complexidade, limites e oracle independente. Distinguir premissa
normativa de fato observado em corpus e de incerteza; as duas últimas não viram
precondição universal silenciosa.

Preferir algoritmo exato; aproximação conservadora precisa de argumento de
soundness e limites de completeness. Regex, grafia externa, ordem de declaração,
quantidade observada em fixture ou nearest-match não definem semântica AIR.

Para mudanças no Validator, separar forma local, índice/ownership, prova decidível,
limite operacional e obrigação externa. Diagnóstico traz regra e identidade/site;
mensagem textual não substitui tipo nem vira API de interpretação downstream.
Alterações de diagnostic ordering/determinismo também exigem análise de impacto.

Registre ambiguidades contra a autoridade adequada e prossiga no trabalho
independente autorizado. Não inventar default para tornar input válido. Uma correção
que exija alterar contrato ou escopo deve tornar essa necessidade explícita.
