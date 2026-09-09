package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;
import static io.github.gustavo2358.air.model.Types.*;

/** Only type signatures are checked. No constant propagation, name lookup or runtime inference. */
final class TypeResolver {
    final ValidationContext c;
    final Map<OperandId, Optional<TypeRef>> types = new LinkedHashMap<>();
    TypeResolver(ValidationContext c) { this.c=c; }
    Optional<TypeRef> type(Operand operand) {
        Optional<TypeRef> cached=types.get(operand.header().id()); if(cached!=null) return cached;
        Set<OperandId> active=new HashSet<>();
        Walk.run(operand,0,Operands::children,new Walk.Visitor<Operand>() {
            public boolean enter(Operand node,long depth) {
                c.depth(depth);
                // Duplicate/cyclic occurrence IDs were diagnosed by PublicationIndex.
                return !types.containsKey(node.header().id()) && active.add(node.header().id());
            }
            public void exit(Operand node,long depth) {
                Optional<TypeRef> result=calculate(node);
                result.ifPresent(t -> c.type(t,node.header().id()));
                types.put(node.header().id(),result); active.remove(node.header().id());
            }
        });
        return cached(operand);
    }
    private Optional<TypeRef> cached(Operand operand) {
        return types.getOrDefault(operand.header().id(),Optional.empty());
    }
    private Optional<TypeRef> calculate(Operand operand) {
        return switch(operand) {
            case Expressions.Literal l -> known(l.value().type());
            case Places.ObjectPlace p -> Optional.ofNullable(c.index.objects.get(p.object())).map(Memory.ObjectDeclaration::typeRef);
            case Places.Choice p -> Optional.of(p.typeRef());
            case Places.RegionSlice p -> Optional.of(p.typeRef());
            case Expressions.Read r -> cached(r.place());
            case Expressions.Unknown u -> Optional.of(u.typeRef());
            case Expressions.Unary u -> {
                Optional<TypeRef> t=cached(u.argument());
                yield switch(u.operator()) {
                    case NOT -> { expect(t,Builtin.BOOL,u.header().id()); yield known(Builtin.BOOL); }
                    case NEG -> { numeric(t,u.header().id()); yield t; }
                    case TO_DECIMAL -> { expect(t,Builtin.INT,u.header().id()); yield known(Builtin.DECIMAL); }
                    case LENGTH -> { textOrBytes(t,u.header().id()); yield known(Builtin.INT); }
                };
            }
            case Expressions.Binary b -> {
                Optional<TypeRef> l=cached(b.left()), r=cached(b.right());
                if(!sameKnown(l,r)) c.error("I-08",b.header().id(),"binary operator requires matching known domains, not shared unknown_type");
                yield switch(b.operator()) {
                    case EQ, NE -> known(Builtin.BOOL);
                    case LT, LE, GT, GE -> { numeric(l,b.header().id()); yield known(Builtin.BOOL); }
                    case AND, OR -> { expect(l,Builtin.BOOL,b.header().id()); expect(r,Builtin.BOOL,b.header().id()); yield known(Builtin.BOOL); }
                    case ADD, SUB, MUL -> { numeric(l,b.header().id()); numeric(r,b.header().id()); yield l; }
                    case CONCAT -> { textOrBytes(l,b.header().id()); textOrBytes(r,b.header().id()); yield l; }
                };
            }
            case Expressions.Quantize q -> { expect(cached(q.value()),Builtin.DECIMAL,q.header().id()); yield known(Builtin.DECIMAL); }
            case Expressions.FitText f -> { expect(cached(f.value()),Builtin.TEXT,f.header().id()); yield known(Builtin.TEXT); }
            case Expressions.SliceText s -> {
                expect(cached(s.value()),Builtin.TEXT,s.header().id());
                expect(cached(s.start()),Builtin.INT,s.header().id());
                expect(cached(s.count()),Builtin.INT,s.header().id()); yield known(Builtin.TEXT);
            }
            case Expressions.TrimRight t -> { expect(cached(t.value()),Builtin.TEXT,t.header().id()); yield known(Builtin.TEXT); }
        };
    }
    void expect(Optional<TypeRef> actual,Type expected,Id owner) {
        if(actual.isEmpty() || !(actual.get() instanceof Known k) || !sameType(k.type(),expected))
            c.error("I-08",owner,"requires known("+expected+"); actual: "+actual);
    }
    private void numeric(Optional<TypeRef> t,Id id) {
        if(!is(t,Builtin.INT) && !is(t,Builtin.DECIMAL)) c.error("I-08",id,"numeric operator requires known int or decimal");
    }
    private void textOrBytes(Optional<TypeRef> t,Id id) {
        if(!is(t,Builtin.TEXT) && !is(t,Builtin.BYTES)) c.error("I-08",id,"operator requires known text or bytes");
    }
    static boolean is(Optional<TypeRef> t,Type domain) { return t.isPresent() && t.get() instanceof Known k && sameType(k.type(),domain); }
    static boolean sameKnown(Optional<TypeRef> a,Optional<TypeRef> b) {
        return a.isPresent() && b.isPresent() && a.get() instanceof Known x && b.get() instanceof Known y && sameType(x.type(),y.type());
    }
    static boolean sameType(Type a,Type b) {
        if(a instanceof LabelType x && b instanceof LabelType y)
            return x.unit().equals(y.unit()) && new HashSet<>(x.labels()).equals(new HashSet<>(y.labels()));
        return a.equals(b);
    }
    static boolean sameRef(TypeRef a,TypeRef b) {
        return a instanceof Known x && b instanceof Known y ? sameType(x.type(),y.type()) : a.equals(b);
    }
    private static Optional<TypeRef> known(Type t) { return Optional.of(new Known(t)); }
}
