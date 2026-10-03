# Transporte de comparações de intervalo

- id: WORK-CONDITION-RANGE-TRANSPORT
- title: Transportar comparações ordenadas existentes da AIR
- status: IN_PROGRESS
- scope: codec AIR JSON, testes e documentação da cobertura

Regra: analysis-ir f8c723e5f023cde92a6f4650e3be9334ebbaba1a,
binding JSON §8 e §10.4, operadores LT/LE/GT/GE. O modelo e o Validator
já definem estes operadores. A modelagem de intervalos de condições 88 no
consumidor precisa transportá-los sem enumerar o domínio.

Algoritmo: adicionar quatro mappings explícitos aos frames iterativos existentes.
O custo continua linear nos nós, com memória proporcional à profundidade.
Não interpretar COBOL, não alterar versões ou semântica e não aceitar outros
operadores por conveniência. Oráculo independente: publicações com inteiros
arbitrariamente grandes, tokens wire escritos no teste, roundtrip exato,
operador inválido e incompatibilidade de domínio rejeitados.

Validação local: RED em `OrderedComparisonChecks` por IMPLEMENTATION_LIMIT;
GREEN com FAST completo (189 checks de modelo, suíte de codec e harness) e
`mvn package`. A fixture integrada de condições 88 percorreu SP → AIR JSON → CFG
→ dependências. Revisão humana pendente; nenhum merge autorizado.
