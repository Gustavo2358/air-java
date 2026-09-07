package io.github.gustavo2358.air.model;

import java.util.ArrayList;
import java.util.List;

/** Exhaustive, non-semantic traversal. It does not evaluate expressions or derive effects. */
public final class Operands {
    private Operands() {}

    public static List<Operand> children(Operand operand) {
        return switch (operand) {
            case Expressions.Literal ignored -> List.of();
            case Expressions.Read read -> List.of(read.place());
            case Expressions.Unknown unknown -> List.copyOf(unknown.dependencies());
            case Expressions.Unary unary -> List.of(unary.argument());
            case Expressions.Binary binary -> List.of(binary.left(), binary.right());
            case Expressions.Quantize q -> List.of(q.value());
            case Expressions.FitText f -> List.of(f.value());
            case Expressions.SliceText s -> List.of(s.value(),s.start(),s.count());
            case Expressions.TrimRight t -> List.of(t.value());
            case Places.ObjectPlace ignored -> List.of();
            case Places.Choice choice -> List.copyOf(choice.candidates());
            case Places.RegionSlice slice -> List.of(slice.offset(),slice.length());
        };
    }

    public static List<Operand> roots(Operation op) {
        List<Operand> result = new ArrayList<>();
        switch(op) {
            case Operations.Assign a -> { result.add(a.destination()); result.add(a.value()); }
            case Operations.HavocMust h -> result.add(h.destination());
            case Operations.HavocMay ignored -> { }
            case Operations.Nop ignored -> { }
            case Operations.CopyBytes c -> {
                range(result,c.destination()); range(result,c.source());
            }
            case Operations.Jump ignored -> { }
            case Operations.Branch b -> result.add(b.predicate());
            case Operations.Dispatch d -> result.add(d.selector());
            case Operations.Invoke i -> {
                target(result,i.target());
                for(Interactions.Argument arg:i.arguments()) switch(arg) {
                    case Interactions.ValueArgument a -> result.add(a.value());
                    case Interactions.CopyArgument a -> result.add(a.value());
                    case Interactions.ReferenceArgument a -> result.add(a.place());
                }
                result.addAll(i.results());
                result.addAll(i.effectOperands());
            }
            case Operations.Return r -> result.addAll(r.values());
            case Operations.Raise r -> result.addAll(r.values());
            case Operations.Halt ignored -> { }
            case Operations.Opaque o -> {
                result.addAll(o.knownOperands());
            }
            case Operations.LocalInvoke ignored -> { }
            case Operations.LocalBoundary ignored -> { }
            case Operations.LocalResume ignored -> { }
            case Operations.LocalUnwind ignored -> { }
            case Operations.IndirectJump i -> result.add(i.target());
        }
        return List.copyOf(result);
    }
    private static void range(List<Operand> out,Memory.ByteRange range) {
        out.add(range.offset()); out.add(range.extent());
    }
    private static void target(List<Operand> out,Interactions.Target target) {
        if(target instanceof Interactions.ComputedTarget c) out.add(c.name());
    }
}
