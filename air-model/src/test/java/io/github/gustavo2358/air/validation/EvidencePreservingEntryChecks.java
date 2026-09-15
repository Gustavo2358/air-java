package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.util.*;

/** EP-W0: independent source possibilities do not assert simultaneous exact contents. */
public final class EvidencePreservingEntryChecks {
    private EvidencePreservingEntryChecks() { }
    public static void main(String[] args) {
        var failures = new ArrayList<String>();
        for (boolean reverse : List.of(false, true)) {
            var f = PossibleEntryChecks.fixture();
            var a = PossibleEntryChecks.possible(f, "a", 0, 4, "AAAA");
            var b = PossibleEntryChecks.possible(f, "b", 0, 4, "BBBB");
            f.state = new Entries.EntryState(reverse ? List.of(b, a) : List.of(a, b), List.of());
            if (!AirValidator.validate(f.build()).isStructurallyValid()) failures.add("possible overlap; reverse=" + reverse);

            f = PossibleEntryChecks.fixture();
            a = PossibleEntryChecks.possible(f, "regional", 0, 4, "AAAA");
            var object = f.object("unproved-other", Types.known(Types.Builtin.TEXT));
            var place = new Places.ObjectPlace(f.entryOperand("other-place", Operand.Role.VALUE_WRITE), object);
            var literal = new Expressions.Literal(f.entryOperand("other-value", Operand.Role.VALUE_READ), new Values.TextValue("BBBB"));
            b = new Entries.InitialCondition(place, new Entries.PossibleLiterals(List.of(literal), f.uncertainty("other-remainder", "ENTRY_OPEN")), f.origin, List.of());
            f.state = new Entries.EntryState(reverse ? List.of(b, a) : List.of(a, b), List.of());
            if (!AirValidator.validate(f.build()).isStructurallyValid()) failures.add("possible unproved separation; reverse=" + reverse);
            if (!f.premises.isEmpty()) throw new AssertionError("test must not fabricate a disjoint premise");
        }
        // Positive structural counterproof: contradictory simultaneous strong literals still fail I-17.
        var strong = AirValidator.validate(RegionalInitialChecks.publication(true, false));
        if (strong.issues().stream().noneMatch(i -> i.rule().equals("I-17"))) throw new AssertionError("strong contradiction was accepted");
        if (!failures.isEmpty()) throw new AssertionError("EP_ENTRY_COEXISTENCE_RED: " + failures);
        System.out.println("EP_ENTRY_COEXISTENCE=PASS");
    }
}
