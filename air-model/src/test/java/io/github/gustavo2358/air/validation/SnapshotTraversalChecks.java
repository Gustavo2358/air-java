package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Independent occurrence-order oracles; these fixtures do not claim structural validity. */
final class SnapshotTraversalChecks {
    private SnapshotTraversalChecks() { }
    static void closedVariants() {
        var f = new Fixtures(); var op = f.op("op"); var h = f.header(op);
        var header = f.operand(op,"parent",Operand.Role.VALUE_READ);
        var object = f.object("object",Fixtures.known(Types.Builtin.TEXT));
        var left = f.text(op,"left","a"); var right = f.text(op,"right","b");
        var third = f.text(op,"third","c");
        var place = f.place(op,"place",object,Operand.Role.VALUE_READ);
        var other = f.place(op,"other",object,Operand.Role.VALUE_WRITE);
        var reason = f.uncertainty("reason","VALUE_UNKNOWN");
        var storage = new StorageId(f.pub,"region");
        Operand[] operands = {
            new Expressions.Literal(header,new Values.TextValue("a")),
            new Expressions.Read(header,place),
            new Expressions.Unknown(header,Fixtures.known(Types.Builtin.TEXT),List.of(left,right),Scopes.NoMemory.INSTANCE,reason),
            new Expressions.Unary(header,Expressions.UnaryOperator.LENGTH,left),
            new Expressions.Binary(header,Expressions.BinaryOperator.CONCAT,left,right),
            new Expressions.Quantize(header,left,BigInteger.ZERO,Expressions.Rounding.TOWARD_ZERO),
            new Expressions.ParseInteger(header,left,right),
            new Expressions.FormatDecimal(header,left,List.of(new DecimalText.Part(DecimalText.Kind.DIGITS,BigInteger.ONE,"",""))),
            new Expressions.IntegerDigits(header,left,BigInteger.ONE),
            new Expressions.WrapInteger(header,left,BigInteger.ONE,false),
            new Expressions.FitDecimal(header,left,BigInteger.ONE,BigInteger.ZERO,false),
            new Expressions.FillText(header,left,BigInteger.ONE),
            new Expressions.FitText(header,left,BigInteger.ONE," "),
            new Expressions.SliceText(header,left,right,third),
            new Expressions.TrimRight(header,left," "),
            new Places.ObjectPlace(header,object),
            new Places.Choice(header,List.of(place,other),Scopes.NoMemory.INSTANCE,Fixtures.known(Types.Builtin.TEXT)),
            new Places.RegionSlice(header,storage,left,right,Memory.AsciiText.INSTANCE,Fixtures.known(Types.Builtin.TEXT))
        };
        List<List<String>> expectedChildren = List.of(
            List.of(),List.of("place"),List.of("left","right"),List.of("left"),List.of("left","right"),
            List.of("left"),List.of("left","right"),List.of("left"),List.of("left"),List.of("left"),
            List.of("left"),List.of("left"),List.of("left"),List.of("left","right","third"),
            List.of("left"),List.of(),List.of("place","other"),List.of("left","right"));
        for (int i=0;i<operands.length;i++) {
            var fixture = new Fixtures();
            fixture.sequence("start",List.of(),new Operations.Opaque(h,"synthetic",List.of(operands[i]),List.of(),f.envelope(null)));
            try (var snapshot = AirSnapshot.fromPublication(fixture.build())) {
                long operation = operation(snapshot,false);
                long parent = snapshot.element(snapshot.field(operation,AirShape.OPERATIONS_OPAQUE,2),AirShape.OPERAND,0);
                var actual = new ArrayList<String>();
                SnapshotOperands.children(snapshot,parent,value -> actual.add(local(snapshot,value)));
                eq(expectedChildren.get(i),actual);
            }
        }
        var target = new Interactions.ComputedTarget("CALL","namespace",third,Interactions.ExactName.INSTANCE,f.origin);
        var arguments = List.<Interactions.Argument>of(new Interactions.ValueArgument(left),new Interactions.CopyArgument(right),new Interactions.ReferenceArgument(place));
        var invoke = invoke(f,h,target,arguments,List.of(other),List.of(place),reason);
        Operation[] operations = {
            new Operations.Assign(h,other,left),new Operations.HavocMust(h,other,reason),
            new Operations.HavocMay(h,new Scopes.VisibleMemory(f.unit,true),reason),new Operations.Nop(h),
            new Operations.CopyBytes(h,new Memory.ByteRange(storage,left,right),new Memory.ByteRange(storage,third,left),BigInteger.ONE,f.envelope(null)),
            new Operations.Jump(h,f.label("start")),new Operations.Branch(h,left,f.label("start"),f.label("start")),
            new Operations.Dispatch(h,left,List.of(),f.label("start")),invoke,
            new Operations.Return(h,List.of(left,right)),new Operations.Raise(h,"tag",List.of(right,left)),
            new Operations.Halt(h,Operations.HaltKind.NORMAL),
            new Operations.Opaque(h,"uninterpreted",List.of(place,left,other),List.of(left.header().id()),f.envelope(null)),
            new Operations.LocalInvoke(h,f.label("start"),List.of(new CompletionPortId(f.unit,"port")),f.label("start"),f.envelope(null)),
            new Operations.LocalBoundary(h,new CompletionPortId(f.unit,"port"),f.label("start"),f.envelope(null)),
            new Operations.LocalResume(h,f.envelope(null)),
            new Operations.LocalUnwind(h,BigInteger.ONE,f.label("start"),f.envelope(null)),
            new Operations.IndirectJump(h,left,new Types.LabelType(f.unit,List.of(f.label("start"))),f.envelope(null))
        };
        List<List<String>> expectedRoots = List.of(
            List.of("other","left"),List.of("other"),List.of(),List.of(),List.of("left","right","third","left"),
            List.of(),List.of("left"),List.of("left"),List.of("third","left","right","place","other","place"),
            List.of("left","right"),List.of("right","left"),List.of(),List.of("place","left","other"),
            List.of(),List.of(),List.of(),List.of(),List.of("left"));
        for (int i=0;i<operations.length;i++) assertRoots(operations[i],expectedRoots.get(i));
        assertRoots(invoke(f,h,new Interactions.InternalTarget(f.entry()),List.of(),List.of(),List.of(),reason),List.of());
        assertRoots(invoke(f,h,new Interactions.LiteralTarget("CALL","ns","literal",Interactions.ExactName.INSTANCE,f.origin),List.of(),List.of(),List.of(),reason),List.of());
    }
    static void streamingAndFailureLifetimes() {
        var source = new StreamingSource(100_000,100_000);
        try (var snapshot = AirSnapshot.attach(source,1)) {
            long[] seen = {0};
            SnapshotOperands.roots(snapshot,2,value -> {
                eq(100+seen[0],value); seen[0]++;
                SnapshotOperands.children(snapshot,value,child -> { throw new AssertionError("literal child"); });
            });
            eq(100_000L,seen[0]); eq(100_001L,source.advances);
            eq(1,source.maxActive); eq(0,source.active); eq(1,source.closedCursors);
            var primary = new IllegalStateException("callback failure");
            source.failClose = true;
            try {
                SnapshotOperands.roots(snapshot,2,value -> { throw primary; });
                throw new AssertionError("callback failure missing");
            } catch (IllegalStateException failure) {
                if (failure != primary) throw new AssertionError("primary failure replaced",failure);
                eq(1,failure.getSuppressed().length);
            }
            eq(0,source.active); eq(AirShape.PUBLICATION,snapshot.shape(1));
        }
        eq(true,source.closed);
        // Primitive cardinality is not narrowed to int; early callback abandonment is legal.
        var huge = new StreamingSource(1L<<42,1L<<42);
        try (var snapshot = AirSnapshot.attach(huge,1)) {
            long[] seen = {0}; var primary = new IllegalArgumentException("stop after three");
            try {
                SnapshotOperands.roots(snapshot,2,value -> { if (++seen[0]==3) throw primary; });
                throw new AssertionError("callback failure missing");
            } catch (IllegalArgumentException failure) {
                if (failure != primary) throw new AssertionError("wrong primary",failure);
            }
            eq(3L,seen[0]); eq(3L,huge.advances); eq(0,huge.active);
        }
        for (long[] lengths : new long[][]{{0,1},{1,0},{2,3},{3,2}}) {
            var broken = new StreamingSource(lengths[0],lengths[1]);
            try (var snapshot = AirSnapshot.attach(broken,1)) {
                try {
                    SnapshotOperands.roots(snapshot,2,value -> { });
                    throw new AssertionError("inconsistent source accepted");
                } catch (IllegalStateException expected) {
                    eq(0,broken.active); eq(1,broken.closedCursors);
                    eq(AirShape.PUBLICATION,snapshot.shape(1));
                }
            }
        }
    }
    /** Synthetic storage law port, deliberately not a whole structurally valid publication. */
    private static final class StreamingSource implements AirSnapshot.Source {
        final long declared,actual; long advances; int active,maxActive,closedCursors;
        boolean failClose,closed;
        StreamingSource(long declared,long actual) { this.declared=declared; this.actual=actual; }
        @Override public AirShape shape(long handle) {
            if (closed) throw new IllegalStateException("source closed");
            if (handle==1) return AirShape.PUBLICATION;
            if (handle==2) return AirShape.OPERATIONS_RETURN;
            if (handle==3) return AirShape.LIST;
            if (handle>=100) return AirShape.EXPRESSIONS_LITERAL;
            throw new AssertionError("unexpected handle "+handle);
        }
        @Override public long length(long handle) { return handle==2?2:declared; }
        @Override public long child(long handle,long index) {
            if (handle==2 && index==1) return 3;
            throw new AssertionError("indexed list read forbidden");
        }
        @Override public long scalar(long handle) { throw new AssertionError("scalar not needed"); }
        @Override public int characters(long handle,long offset,char[] out,int start,int count) { throw new AssertionError("text not needed"); }
        @Override public AirSnapshot.Elements elements(long handle) {
            if (handle!=3) throw new AssertionError("wrong list");
            active++; maxActive=Math.max(maxActive,active);
            return new AirSnapshot.Elements() {
                long visited; boolean closed;
                @Override public boolean advance() {
                    if (closed) throw new IllegalStateException("cursor closed");
                    advances++; if (visited==actual) return false; visited++; return true;
                }
                @Override public long value() { return 99+visited; }
                @Override public void close() {
                    if (!closed) { closed=true; active--; closedCursors++;
                        if (failClose) throw new IllegalStateException("injected raw close failure"); }
                }
            };
        }
        @Override public void close() { if(active!=0) throw new AssertionError("cursor leaked"); closed=true; }
    }
    private static Operations.Invoke invoke(Fixtures f,Operations.Header h,Interactions.Target target,List<Interactions.Argument> args,List<Place> results,List<Place> effects,UncertaintyId reason) {
        return new Operations.Invoke(h,"CALL",target,args,results,new Interactions.ExternalSignature(f.signature(List.of(),List.of())),effects,f.effects(),
            new Control.InvocationOutcomes(List.of(new Control.Normal(f.label("start"))),Scopes.NoControl.INSTANCE),new Interactions.UnknownContract(reason));
    }
    private static void assertRoots(Operation op,List<String> expected) {
        var f = new Fixtures(); boolean instruction = op instanceof Instruction;
        if (op instanceof Instruction value) f.linear(value); else f.sequence("start",List.of(),(Terminator)op);
        try (var snapshot = AirSnapshot.fromPublication(f.build())) {
            long handle = operation(snapshot,instruction); var actual = new ArrayList<String>();
            SnapshotOperands.roots(snapshot,handle,value -> actual.add(local(snapshot,value)));
            eq(expected,actual);
            // Wrong kinds must fail instead of silently yielding an empty occurrence set.
            fails(() -> SnapshotOperands.children(snapshot,handle,value -> { throw new AssertionError("unexpected child"); }));
        }
    }
    private static long operation(AirSnapshot s,boolean instruction) {
        long unit = s.element(s.field(s.root(),AirShape.PUBLICATION,4),AirShape.UNIT,0);
        long sequence = s.element(s.field(unit,AirShape.UNIT,5),AirShape.SEQUENCE,0);
        return instruction ? s.element(s.field(sequence,AirShape.SEQUENCE,1),AirShape.INSTRUCTION,0) : s.field(sequence,AirShape.SEQUENCE,2);
    }
    private static String local(AirSnapshot s,long operand) {
        long header = s.field(operand,s.shape(operand),0);
        long id = s.field(header,AirShape.OPERAND_HEADER,0);
        long text = s.field(id,AirShape.IDS_OPERAND_ID,1);
        char[] chars = new char[Math.toIntExact(s.characterCount(text))];
        s.readCharacters(text,0,chars,0,chars.length); return new String(chars);
    }
    private static void eq(Object expected,Object actual) { if (!expected.equals(actual)) throw new AssertionError("expected="+expected+" actual="+actual); }
    private static void fails(Runnable action) { try { action.run(); } catch (IllegalArgumentException expected) { return; } throw new AssertionError("expected wrong-kind rejection"); }
}
