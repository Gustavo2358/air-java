package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Binding oracle independent of the writer's operator mapping. */
public final class OrderedComparisonChecks {
    public static void main(String[] args) { run(); }
    static void run() {
        var operators = List.of(Expressions.BinaryOperator.LT, Expressions.BinaryOperator.LE,
                Expressions.BinaryOperator.GT, Expressions.BinaryOperator.GE);
        var tokens = List.of("lt", "le", "gt", "ge");
        for (int i = 0; i < operators.size(); i++) {
            var p = W2cOracle.publication("full");
            var u = p.units().getFirst(); var s = u.sequences().getFirst();
            var old = (Operations.Branch) s.terminator();
            var left = new Expressions.Literal(W2cOracle.operand("if", "left", Operand.Role.VALUE_READ, "predicate"),
                    new Values.IntValue(new BigInteger("-99999999999999999999999999999")));
            var right = new Expressions.Literal(W2cOracle.operand("if", "right", Operand.Role.VALUE_READ, "predicate"),
                    new Values.IntValue(new BigInteger("99999999999999999999999999999")));
            var comparison = new Expressions.Binary(W2cOracle.operand("if", "compare", Operand.Role.PREDICATE, "predicate"),
                    operators.get(i), left, right);
            var sequences = new ArrayList<>(u.sequences());
            sequences.set(0, new Sequence(s.label(), s.instructions(),
                    new Operations.Branch(old.header(), comparison, old.trueDestination(), old.falseDestination()), s.origin()));
            var expected = W2cOracle.copy(p, List.of(W2cOracle.sequences(u, sequences)), p.storage(), p.premises());
            var codec = new AirJson(); var bytes = codec.encode(expected);
            if (!expected.equals(codec.decode(bytes)) || !Arrays.equals(bytes, codec.encode(codec.decode(bytes))))
                throw new AssertionError("ordered comparison facts changed");
            var wire = new String(bytes, StandardCharsets.UTF_8);
            var token = "\"operator\":\"" + tokens.get(i) + "\"";
            if (!wire.contains(token)) throw new AssertionError("missing normative token " + tokens.get(i));
            reject(codec, wire.replace(token, "\"operator\":\"bogus\""), AirJsonException.Code.INPUT_ERROR);
            reject(codec, wire.replace(token, "\"operator\":\"and\""), AirJsonException.Code.INVALID_IR);
        }
    }
    private static void reject(AirJson codec, String wire, AirJsonException.Code code) {
        try { codec.decode(wire.getBytes(StandardCharsets.UTF_8)); throw new AssertionError("invalid comparison accepted"); }
        catch (AirJsonException e) { if (e.code() != code) throw new AssertionError("wrong diagnostic", e); }
    }
}
