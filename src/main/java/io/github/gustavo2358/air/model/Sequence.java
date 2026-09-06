package io.github.gustavo2358.air.model;


/** A representation sequence, not a computed maximal basic block. */
public record Sequence(Ids.LabelId label, java.util.List<Instruction> instructions, Terminator terminator,
                       Ids.OriginId origin) {
    public Sequence {
        java.util.Objects.requireNonNull(label,"label");
        instructions=java.util.List.copyOf(instructions);
        java.util.Objects.requireNonNull(terminator,"terminator");
        java.util.Objects.requireNonNull(origin,"origin");
    }
    public java.util.List<Operation> operations() {
        java.util.ArrayList<Operation> result = new java.util.ArrayList<>(instructions.size()+1);
        result.addAll(instructions); result.add(terminator); return java.util.List.copyOf(result);
    }
}
