# Spec — 0C-I

Problema: separar topologia de transporte antes de 1A sem alterar domínio/API AIR.
Implementar Opção B aprovada: root air-java-parent:pom; air-model produz air-java:jar
com model + validation intactos; air-json:jar vazio depende diretamente de air-java.
Entradas: baseline real pós-merge 0C-D, ADR-0002 e mains atuais dos consumers.
Classes de risco: ownership, dependência runtime invisível em bytecode, inversão,
JAR stale, reactor incompleto, suíte omitida/duplicada/reordenada e cwd incorreto.

Preservar INV-AIR-001–007, packages, APIs, bytes de fontes e contratos. Source lock
normativo não muda. JSON vazio é política explícita de 0C-I que rejeita primeiro código.
Fora de escopo: codec/binding/libs JSON, domínio novo, release, E2E, 1A e consumers reais.
