# Integração das prioridades 1 e 2

Revisão aprovada em 2026-10-04. O merge do [PR #27](https://github.com/Gustavo2358/air-java/pull/27) efetiva DONE com os testes técnicos aprovados.

A AIR transporta comparações de faixa, conversões numéricas e texto simbólico.
O binding fixa DecimalPart.kind em DIGITS, SUPPRESS_SPACE, SUPPRESS_STAR,
INSERT, RADIX, SIGN e FLOAT_SIGN. O teste independente verifica os sete tokens,
a escrita canônica e rejeições; as duas mutações de caixa foram detectadas.

Autoridade integrada: analysis-ir #10, `09dd8ea1d5deb2ef4b1d16115ea953585752cfdf`.
A árvore é idêntica ao head normativo aprovado `84d3de3`; o repin não altera o contrato.
FAST do head revisado passou. O fechamento executa novamente FAST para o pin integrado.
A qualificação CardDemo de 73 fontes é reutilizada por equivalência de produção;
65 PARTIAL e 8 COMPLETE, sem alegação de cobertura integral de COBOL.
