package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.util.*;

/** EP-W0: independent source possibilities do not assert simultaneous exact contents. */
public final class EvidencePreservingEntryChecks {
    private EvidencePreservingEntryChecks() { }
    public static void main(String[] args) { run(); }
    static Fixtures fixture() {
        var f=PossibleEntryChecks.fixture();
        f.capabilities.remove(Capabilities.ENTRY_POSSIBILITIES);
        f.capabilities.add(new Capabilities.Capability("entry.possibilities", "2"));
        return f;
    }
    static void run() {
        var failures = new ArrayList<String>();
        for (boolean reverse : List.of(false, true)) {
            var f = fixture();
            var a = PossibleEntryChecks.possible(f, "a", 0, 4, "AAAA");
            var b = PossibleEntryChecks.possible(f, "b", 0, 4, "BBBB");
            f.state = new Entries.EntryState(reverse ? List.of(b, a) : List.of(a, b), List.of());
            if (!AirValidator.validate(f.build()).isStructurallyValid()) failures.add("possible overlap; reverse=" + reverse);

            f = fixture();
            a = PossibleEntryChecks.possible(f, "regional", 0, 4, "AAAA");
            var object = f.object("unproved-other", Types.known(Types.Builtin.TEXT));
            var place = new Places.ObjectPlace(f.entryOperand("other-place", Operand.Role.VALUE_WRITE), object);
            var literal = new Expressions.Literal(f.entryOperand("other-value", Operand.Role.VALUE_READ), new Values.TextValue("BBBB"));
            b = new Entries.InitialCondition(place, new Entries.PossibleLiterals(List.of(literal), f.uncertainty("other-remainder", "ENTRY_OPEN")), f.origin, List.of());
            f.state = new Entries.EntryState(reverse ? List.of(b, a) : List.of(a, b), List.of());
            if (!AirValidator.validate(f.build()).isStructurallyValid()) failures.add("possible unproved separation; reverse=" + reverse);
            if (!f.premises.isEmpty()) throw new AssertionError("test must not fabricate a disjoint premise");
        }
        var logical=fixture();
        var object=new io.github.gustavo2358.air.model.Ids.ObjectId(logical.unit,"logical-value");
        var bindingReason=logical.uncertainty("binding-open","STORAGE_OPEN");
        var claim=new Evidence.Claim(new Scopes.UnitScope(logical.unit),Evidence.PrecisionStatus.OPEN,List.of(bindingReason));
        var precision=new Evidence.Precision(claim,claim,claim,claim,claim);
        logical.objects.add(new Memory.ObjectDeclaration(object,Optional.of("logical-value"),Types.known(Types.Builtin.TEXT),
            new Memory.UnknownBinding(new Scopes.AllMemory(logical.pub,true),bindingReason),Memory.Visibility.UNKNOWN,logical.origin,Evidence.CoverageStatus.ABSTRACTED,precision));
        var value=new Expressions.Literal(logical.entryOperand("logical-candidate",Operand.Role.VALUE_READ),new Values.TextValue("PROGA"));
        var place=new Places.ObjectPlace(logical.entryOperand("logical-place",Operand.Role.VALUE_WRITE),object);
        logical.state=new Entries.EntryState(List.of(new Entries.InitialCondition(place,new Entries.PossibleLiterals(List.of(value),logical.uncertainty("logical-remainder","ENTRY_OPEN")),logical.origin,List.of())),List.of());
        if(!AirValidator.validate(logical.build()).isStructurallyValid())failures.add("supported logical value with unknown binding");
        if(logical.storage.stream().anyMatch(x->x instanceof Memory.Cell))throw new AssertionError("logical evidence fabricated a Cell");
        // Positive structural counterproof: contradictory simultaneous strong literals still fail I-17.
        var strong = AirValidator.validate(RegionalInitialChecks.publication(true, false));
        if (strong.issues().stream().noneMatch(i -> i.rule().equals("I-17"))) throw new AssertionError("strong contradiction was accepted");
        if (!failures.isEmpty()) throw new AssertionError("EP_ENTRY_COEXISTENCE_RED: " + failures);
        System.out.println("EP_ENTRY_COEXISTENCE=PASS");
    }
}
