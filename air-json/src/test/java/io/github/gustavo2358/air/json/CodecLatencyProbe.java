package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.validation.AirValidator;
import io.github.gustavo2358.air.validation.ValidationOptions;
import java.nio.file.Files;
import java.nio.file.Path;

/** Standalone benchmark: AIR path, worker count (0 = legacy binding / installed full decode), iterations, full|phases. */
public final class CodecLatencyProbe {
    private CodecLatencyProbe() {}
    private static volatile Object retained;
    public static void main(String[] args) throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of(args[0]));
        int workers = Integer.parseInt(args[1]);
        int iterations = Integer.parseInt(args[2]);
        for (int iteration = 0; iteration < iterations; iteration++) {
            long start = System.nanoTime();
            double parse = 0, bind = 0, validate = 0;
            if (args[3].equals("phases")) {
                var tree = Utf8Input.parse(bytes, AirJson.Limits.defaults());
                long parsed = System.nanoTime();
                io.github.gustavo2358.air.model.Publication publication;
                if (workers == 0) publication = new BindingReader().envelope(tree);
                else try (var blocks = new OrderedBlocks(workers)) {
                    publication = new BindingReader(blocks).envelope(tree);
                }
                long bound = System.nanoTime();
                retained = AirValidator.validate(publication);
                long validated = System.nanoTime();
                parse = (parsed - start) / 1e6; bind = (bound - parsed) / 1e6; validate = (validated - bound) / 1e6;
            } else {
                var codec = workers == 0 ? new AirJson() : new AirJson(AirJson.Limits.defaults(),
                        ValidationOptions.defaults(), new AirJson.DecodeOptions(workers));
                retained = codec.decodeForPartialAnalysis(bytes);
            }
            double total = (System.nanoTime() - start) / 1e6;
            System.out.printf(java.util.Locale.ROOT,
                    "{\"iteration\":%d,\"workers\":%d,\"totalMs\":%.3f,\"parseMs\":%.3f,\"bindMs\":%.3f,\"validateMs\":%.3f}%n",
                    iteration, workers, total, parse, bind, validate);
            retained = null;
            System.gc(); // Outside measured interval, consistently for baseline and candidate.
        }
    }
}
