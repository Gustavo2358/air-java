package io.github.gustavo2358.air.json;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Physical names/offsets only: binding must not rebuild the same record index. */
final class FieldIndexOwnershipChecks {
    private FieldIndexOwnershipChecks() {}
    static void run() {
        try {
            var physical=Utf8Input.parse("{\"Aa\":\"first\",\"BB\":null,\"nested\":{\"value\":true}}".getBytes(StandardCharsets.UTF_8),AirJson.Limits.defaults());
            var at=java.util.Arrays.stream(BindingReader.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals("At")).findFirst().orElseThrow();
            var constructor=at.getDeclaredConstructor(BindingReader.class,Json.Value.class,at,String.class,int.class);constructor.setAccessible(true);
            var owner=constructor.newInstance(new BindingReader(),physical,null,null,-1);
            var object=at.getDeclaredMethod("object");object.setAccessible(true);
            Object first=object.invoke(owner);
            for(int i=0;i<100;i++)if(first!=object.invoke(owner))throw new AssertionError("record field index reconstructed");
            @SuppressWarnings("unchecked") var fields=(Map<String,Json.Value>)first;
            if(fields.size()!=3||!fields.containsKey("BB")||fields.get("BB")!=Json.Nil.INSTANCE||fields.get("missing")!=null)throw new AssertionError("presence or null changed");
            if(!"first".equals(((Utf8Input.Node)fields.get("Aa")).text())||!((Utf8Input.Node)fields.get("nested")).object())throw new AssertionError("colliding field keys or nested offsets changed");
            try {fields.clear();throw new AssertionError("mutable field index");}catch(UnsupportedOperationException expected){}
        } catch(ReflectiveOperationException failure) {throw new AssertionError(failure);}
    }
}
