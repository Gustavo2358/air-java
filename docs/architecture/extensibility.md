# Extensibilidade sem duplicar contratos

Uma nova forma AIR exige seção normativa pinada, capacidade/versionamento,
representação tipada, traversal, fechamento/ownership, diagnósticos, oráculos e
atualização do [catálogo Java](../model-catalog.md) e do [status](../implementation-status.md).
Não adicionar payload livre `Map<String,Object>`, selecionar candidato ou usar
nome de classe Java como autoridade semântica.

Mudança de algoritmo do Validator deve registrar premissas, índices, terminação,
custo e limites. Construtores cuidam da forma local; Validator trata relações;
verdade do produtor continua obrigação externa. Evite validador paralelo em codec.

No roadmap, `air-json → modelo` é obrigatório. O codec compartilhado não leva
Jackson/Gson, I/O ou exceções de parsing para a API do núcleo. Transporte tem versão
própria e draft pinado; o modelo continua consumível diretamente em memória.
Extensão de gates acompanha a topologia aprovada, sem mover classes neste harness.
