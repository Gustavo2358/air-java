# Fontes e precedência

1. AIR normativa no SHA de [sources.lock.json](../sources.lock.json) governa semântica.
2. Contratos locais de API, arquitetura e invariantes governam a implementação
   dentro dessa norma. Divergência é explícita e exige reconciliação.
3. Work item e autorização da sessão delimitam execução; roadmap/backlog planejam.
4. Fixtures, implementações irmãs e evidência histórica informam, sem redefinir AIR.

Consultar fonte pinada por URL GitHub com commit completo ou `git show SHA:path`
no checkout correspondente. Nunca usar main flutuante por conveniência nem atualizar
lock automaticamente ao fazer fetch. Confirmar seções normativas necessárias;
binding JSON 1.0.0 continua DRAFT e não altera AIR 2.0.0 por conta própria.

[Baseline do harness](harness-baseline.json) fixa snapshots usados para adaptação,
separadamente do lock normativo existente. Não exige diretórios irmãos no CI,
não executa conteúdo remoto e não copia seus AGENTS como instruções locais.
