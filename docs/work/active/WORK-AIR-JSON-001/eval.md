# Evals

Oracles: golden manual do draft/0B + Publication Java manual; encode exato, decode
com igualdade profunda, ambos round-trips e recanonicalização. Contratos model
existentes continuam obrigatórios. Positivos variam códigos/textos/nomes opacos,
Unicode, inteiros arbitrários, proveniência null/includes e ordens de inventários.

Negativos: BOM/UTF-8/trailing/raiz, duplicatas inclusive escapadas, campos/kind/
versões, null/omissão, inteiros físicos, IDs completos/owner/domain/fechamento,
PARTIAL sem razão. Formas válidas fora da cobertura falham por suporte/limite.

Challenges: fortalecer coverage/precision, remover origins/gaps, ordenar arrays,
aceitar duplicates/numbers/versão/unknown fields, newline/escaping, ignorar forma
não suportada, omitir suíte/módulo, dependência JSON no model em compile/runtime/
optional, model copiado no JSON. Registrar RED identificado e restauração exata.

Gates: full, check.sh, root mvn clean verify, git/scope --work WORK-AIR-JSON-001,
MANIFEST; CI remoto do head. Sem alegar section 13 inteira, E2E, Base64/decimal
ou catálogo fora da cobertura, desempenho ou interoperabilidade independente.
