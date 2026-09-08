# Plano

1. Confirmar baseline e merge 1A; reconciliar lifecycle e criar branch 4B.
2. Escrever golden manual e oracle Java independente; RED antes do mapping.
3. Mapear explicitamente reader/writer, sem DTO novo ou scan de inventário.
4. Negativos, enums, ordenação, escala N/2N e preservação de regressões 1A.
5. Challenges compiláveis em cópia temporária, restore exato e segundo GREEN.
6. Gates, diff integral/staged, commits, push, PR e CI exact-head.

O mapping percorre cada array uma vez, referências viram IDs; Validator mantém
índices existentes. O(bytes + entidades + referências) para estas formas fixas,
com sorting de campos de cardinalidade limitada na camada física. Sem joins novos.
Terminação: AST finita e limits vigentes; sem cópias integrais adicionais ou API streaming.
Riscos: nullability/enum drift, classificação de construtores e fortalecimento de claims.
Rollback de falsificações em cópia descartável com hashes; preservar checkout original.
