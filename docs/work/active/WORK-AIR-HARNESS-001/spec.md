# Spec — harness do air-java

Problema: a biblioteca tem checks e regras locais, mas falta roteamento progressivo,
lifecycle, escopo verificável e contracasos do próprio harness para a evolução futura.

Resultado: AGENTS como índice, famílias canônicas pequenas, gates nomeados e
executáveis, branch isolada e PR humano. Entradas do harness são documentos locais,
metadados, diff Git, fontes compiladas e saída real da suíte existente.

Classes: baseline válida; referências/scope/estado inválidos; ferramenta indisponível;
gate planejado sem executor. Incerteza permanece ERROR/UNAVAILABLE, nunca PASS.
Autorização permite completar todo o harness, sem aprovação intermediária artificial.

Preservar todos os arquivos de aplicação, POM, suíte, exemplos e locks. Somente
AGENTS e MANIFEST podem mudar dentre os arquivos existentes. Não iniciar discovery
0C, modularização, codec, integração, alterações normativas ou repositórios irmãos.
