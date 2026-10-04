package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.DecimalText;
import io.github.gustavo2358.air.model.Expressions;
import io.github.gustavo2358.air.model.Ids.OperandId;
import io.github.gustavo2358.air.model.Ids.OperationOwner;
import io.github.gustavo2358.air.model.Operand;
import io.github.gustavo2358.air.model.Operations;
import io.github.gustavo2358.air.model.Publication;
import io.github.gustavo2358.air.model.Sequence;
import io.github.gustavo2358.air.model.Unit;
import io.github.gustavo2358.air.model.Values;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Literal wire oracles from the binding, independent of both codec mappings. */
public final class DecimalPartBindingChecks {
    private DecimalPartBindingChecks() { }

    private record Example(String token, DecimalText.Kind kind, String wire,
                           BigInteger count, String text, String negative) { }

    private static final List<Example> EXAMPLES = List.of(
        new Example("DIGITS", DecimalText.Kind.DIGITS,
            "{\"count\":\"4\",\"kind\":\"DIGITS\",\"negative\":\"\",\"text\":\"\"}",
            BigInteger.valueOf(4), "", ""),
        new Example("SUPPRESS_SPACE", DecimalText.Kind.SUPPRESS_SPACE,
            "{\"count\":\"4\",\"kind\":\"SUPPRESS_SPACE\",\"negative\":\"\",\"text\":\"\"}",
            BigInteger.valueOf(4), "", ""),
        new Example("SUPPRESS_STAR", DecimalText.Kind.SUPPRESS_STAR,
            "{\"count\":\"4\",\"kind\":\"SUPPRESS_STAR\",\"negative\":\"\",\"text\":\"\"}",
            BigInteger.valueOf(4), "", ""),
        new Example("INSERT", DecimalText.Kind.INSERT,
            "{\"count\":\"1\",\"kind\":\"INSERT\",\"negative\":\"\",\"text\":\"/\"}",
            BigInteger.ONE, "/", ""),
        new Example("RADIX", DecimalText.Kind.RADIX,
            "{\"count\":\"1\",\"kind\":\"RADIX\",\"negative\":\"\",\"text\":\".\"}",
            BigInteger.ONE, ".", ""),
        new Example("SIGN", DecimalText.Kind.SIGN,
            "{\"count\":\"1\",\"kind\":\"SIGN\",\"negative\":\"-\",\"text\":\"+\"}",
            BigInteger.ONE, "+", "-"),
        new Example("FLOAT_SIGN", DecimalText.Kind.FLOAT_SIGN,
            "{\"count\":\"1\",\"kind\":\"FLOAT_SIGN\",\"negative\":\"-\",\"text\":\"+\"}",
            BigInteger.ONE, "+", "-")
    );
    private static final String SENDING_HEADER =
        "{\"id\":{\"domain\":\"operand\",\"localId\":\"decimal-source\","
        + "\"owner\":{\"kind\":\"operation\",\"localId\":\"set-program\"},"
        + "\"publication\":\"cp4b-scalar-manual\",\"unit\":\"alpha\"},"
        + "\"origin\":{\"domain\":\"origin\",\"localId\":\"literal\","
        + "\"publication\":\"cp4b-scalar-manual\"},\"role\":\"VALUE_READ\"}";

    public static void main(String[] args) {
        run();
        System.out.println("DECIMAL_PART_BINDING=PASS tokens=7 reads=7 canonicalWrites=7 rejections=21");
    }

    static void run() {
        var codec = new AirJson();
        // Existing hand-authored canonical envelope, not output from the writer under test.
        var base = manualFixture();
        var original = "\"kind\":\"literal\",\"value\":{\"kind\":\"text\",\"value\":\"PROGA\"}";
        if (!base.contains(original) || base.indexOf(original) != base.lastIndexOf(original)) {
            throw new AssertionError("manual scalar fixture must contain exactly one sending literal");
        }
        for (var example : EXAMPLES) {
            // A mandatory digit suffix makes every individual segment a valid format.
            var parts = "[" + example.wire() + "," + EXAMPLES.getFirst().wire() + "]";
            var format = "\"kind\":\"format_decimal\",\"parts\":" + parts
                + ",\"value\":{\"header\":" + SENDING_HEADER
                + ",\"kind\":\"literal\",\"value\":{\"coefficient\":\"1234\",\"kind\":\"decimal\",\"scale\":\"0\"}}";
            var input = base.replace(original, format);
            var expected = publication(example);
            if (!expected.equals(codec.decode(input.getBytes(StandardCharsets.UTF_8)))) {
                throw new AssertionError("reader differs from literal binding oracle: " + example.token());
            }
            if (!Arrays.equals(input.getBytes(StandardCharsets.UTF_8), codec.encode(expected))) {
                throw new AssertionError("writer differs from literal canonical oracle: " + example.token());
            }
            var mixed = example.token().substring(0, 1) + example.token().substring(1).toLowerCase(Locale.ROOT);
            for (var invalid : List.of(example.token().toLowerCase(Locale.ROOT), mixed, "UNKNOWN_SEGMENT")) {
                var rejected = input.replace("\"kind\":\"" + example.token() + "\"", "\"kind\":\"" + invalid + "\"");
                try {
                    codec.decode(rejected.getBytes(StandardCharsets.UTF_8));
                    throw new AssertionError("out-of-catalog decimal token accepted: " + invalid);
                } catch (AirJsonException exception) {
                    if (exception.code() != AirJsonException.Code.INPUT_ERROR
                            || !exception.getMessage().contains("unknown decimal format segment")) {
                        throw exception;
                    }
                }
            }
        }
    }

    private static String manualFixture() {
        try {
            return Files.readString(Path.of("src/test/resources/scalar-assign.canonical.json"));
        } catch (IOException exception) {
            throw new AssertionError("cannot read independent scalar fixture", exception);
        }
    }

    private static Publication publication(Example example) {
        var base = ScalarAssignOracle.publication();
        var unit = base.units().getFirst();
        var sequence = unit.sequences().getFirst();
        var assign = (Operations.Assign) sequence.instructions().getFirst();
        var header = new Operand.Header(new OperandId(new OperationOwner(assign.header().id()), "decimal-source"),
            Operand.Role.VALUE_READ, assign.value().header().origin());
        var format = new Expressions.FormatDecimal(assign.value().header(),
            new Expressions.Literal(header, new Values.DecimalValue(BigInteger.valueOf(1234), BigInteger.ZERO)),
            List.of(new DecimalText.Part(example.kind(), example.count(), example.text(), example.negative()),
                new DecimalText.Part(DecimalText.Kind.DIGITS, BigInteger.valueOf(4), "", "")));
        var updated = new Unit(unit.id(), unit.containingUnit(), unit.objects(), unit.visibleObjects(), unit.entries(),
            List.of(new Sequence(sequence.label(), List.of(new Operations.Assign(assign.header(), assign.destination(), format)),
                sequence.terminator(), sequence.origin())),
            unit.completionPorts(), unit.body(), unit.bodyUnavailable(), unit.coverage(), unit.origin());
        return new Publication(base.id(), base.airVersion(), base.capabilities(), base.artifacts(), List.of(updated),
            base.storage(), base.resources(), base.artifactRelations(), base.origins(), base.coverage(),
            base.uncertainties(), base.premises());
    }
}
