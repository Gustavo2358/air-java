package io.github.gustavo2358.air.json;

import java.nio.charset.StandardCharsets;
import java.util.*;

final class Utf8InputChecks {
    private Utf8InputChecks() { }
    static void run() {
        var random=new Random(913357);
        for(int i=0;i<12000;i++) {
            String json="{\"root\":"+value(random,4)+"}";
            byte[] bytes=json.getBytes(StandardCharsets.UTF_8);
            compare(bytes,AirJson.Limits.defaults());
            if(i%3==0) { bytes[random.nextInt(bytes.length)]=(byte)random.nextInt(256);compare(bytes,AirJson.Limits.defaults()); }
            if(i%5==0)compare(bytes,new AirJson.Limits(Integer.MAX_VALUE,1+random.nextInt(5)));
        }
        for(String text:List.of("{\"a\":null,\"\\u0061\":false}","{\"\\u0061\":[],\"a\":{}}",
                "{\"Aa\":null,\"BB\":false,\"a\":true}","{\"x\":\"\\uD800\"}",
                "{\"x\":\"\\uD800\\uDC00\"}","{\"x\":\"\\uDC00\\uD800\"}","{} {}","\ufeff{}"))
            compare(text.getBytes(StandardCharsets.UTF_8),AirJson.Limits.defaults());
        // Exercise the field-name cache fallback and wide-object duplicate detection.
        var wide=new StringBuilder("{");
        for(int i=0;i<1000;i++){if(i>0)wide.append(',');wide.append('"').append("field-").append(i).append("\":null");}
        compare((wide+"}").getBytes(StandardCharsets.UTF_8),AirJson.Limits.defaults());
        compare((wide+",\"field-999\":true}").getBytes(StandardCharsets.UTF_8),AirJson.Limits.defaults());
        var codec=new AirJson();byte[] golden=codec.encode(GobackOracle.publication());
        var checked=codec.decodeChecked(golden);
        if(!checked.publication().equals(GobackOracle.publication())
                ||!checked.result().equals(codec.decodeForPartialAnalysis(golden).validation()))throw new AssertionError("checked decode");
        // Neither semantic mapping nor a parallel worker can mask a later physical failure.
        String bad=new String(golden,StandardCharsets.UTF_8).replace("\"bindingVersion\":\"1.0.0\"","\"bindingVersion\":null");
        byte[] corrupt=(bad+"\n").getBytes(StandardCharsets.UTF_8);corrupt[corrupt.length-1]=(byte)0xff;
        try { codec.decode(corrupt);throw new AssertionError("invalid UTF-8 accepted"); }
        catch(AirJsonException error) { if(!error.getMessage().contains("Invalid UTF-8"))throw error; }
    }
    private static String value(Random r,int depth) {
        String[] strings={"\"\"","\"ascii\"","\"Ω😀é\"","\"\\u00e9\\uD83D\\uDE00\"","\"\\\"\\\\\\n\\b\\t\\r\\f\\/\"","\"\\u0000\""};
        return switch(r.nextInt(depth>0?7:5)) {
            case 0 -> "null";case 1 -> "true";case 2 -> "false";
            case 3,4 -> strings[r.nextInt(strings.length)];
            case 5 -> "["+value(r,depth-1)+","+value(r,depth-1)+"]";
            default -> "{\"Aa\":"+value(r,depth-1)+",\"BB\":"+value(r,depth-1)+",\"\\u0063\":"+value(r,depth-1)+"}";
        };
    }
    private static Object outcome(byte[] bytes,AirJson.Limits limits,boolean direct) {
        try { return materialize(direct?Utf8Input.parse(bytes,limits):Json.parse(bytes,limits)); }
        catch(AirJsonException e) { return List.of(e.code(),e.path(),e.getMessage(),e.issues()); }
    }
    private static void compare(byte[] bytes,AirJson.Limits limits) {
        Object expected=outcome(bytes,limits,false),actual=outcome(bytes,limits,true);
        if(!expected.equals(actual))throw new AssertionError("physical mismatch: "+Arrays.toString(bytes)+"\n"+expected+"\n"+actual);
    }
    private static Json.Value materialize(Json.Value value) {
        if(!(value instanceof Utf8Input.Node node))return value;
        if(node.object()) {
            var fields=new LinkedHashMap<String,Json.Value>();node.fields().forEach((k,v)->fields.put(k,materialize(v)));return new Json.Obj(fields);
        }
        if(node.array())return new Json.Arr(node.values().stream().map(Utf8InputChecks::materialize).toList());
        if(node.text()!=null)return new Json.Text(node.text());
        return new Json.Bool(node.bool());
    }
}
