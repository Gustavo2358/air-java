package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.Publication;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Independent physical golden: scope sharing is ownership, never fact merging. */
final class ScopedIdentityChecks {
    private ScopedIdentityChecks() {}
    static void run() {
        try {
            byte[] golden = Files.readAllBytes(Path.of("src/test/resources/goback.canonical.json"));
            var codec = new AirJson();
            var first = codec.decode(golden);
            require(first.equals(GobackOracle.publication()), "complete independent facts");
            owned(first);
            var second = codec.decode(golden);
            owned(second);
            require(first.id() != second.id() && first.units().get(0).id() != second.units().get(0).id(), "per-read owner");
            require(Arrays.equals(golden, codec.encode(first)), "physical golden preserved");
            // Same Java hash, distinct exact namespaces and unit names must remain separate.
            String text = new String(golden, StandardCharsets.UTF_8);
            String collision = text.replace("goback-0b-manual", "Aa")
                .replace("\"domain\":\"artifact\",\"localId\":\"original\",\"publication\":\"Aa\"", "\"domain\":\"artifact\",\"localId\":\"original\",\"publication\":\"BB\"")
                .replace("\"unit\":\"unit\"", "\"unit\":\"BB\"");
            // Bind only: deliberately cross-owned references must survive for Validator rejection.
            var read = new BindingReader().envelope(Json.parse(collision.getBytes(StandardCharsets.UTF_8), AirJson.Limits.defaults()));
            require(read.artifacts().get(0).id().publication().localId().equals("BB"), "namespace collision merged");
            require(read.units().get(0).id().publication().localId().equals("Aa"), "namespace owner lost");
            require(read.units().get(0).entries().get(0).id().unit().localId().equals("BB"), "unit owner repaired");
            String mixed = text.replace("goback-0b-manual", "Aa")
                .replace("\"domain\":\"unit\",\"localId\":\"unit\",\"publication\":\"Aa\"", "\"domain\":\"unit\",\"localId\":\"unit\",\"publication\":\"BB\"");
            var mixedRead = new BindingReader().envelope(Json.parse(mixed.getBytes(StandardCharsets.UTF_8), AirJson.Limits.defaults()));
            var declaration = mixedRead.units().get(0).id();
            var reference = mixedRead.units().get(0).entries().get(0).id().unit();
            require(declaration.localId().equals(reference.localId()) && !declaration.publication().equals(reference.publication()), "full unit key lost publication");
            require(declaration != reference, "cross-publication unit identity merged");
            String malformed = text.replaceFirst("\"domain\":\"artifact\"", "\"domain\":\"artifact\",\"unexpected\":true");
            try { codec.decode(malformed.getBytes(StandardCharsets.UTF_8)); throw new AssertionError("cached scope bypassed field checks"); }
            catch (AirJsonException expected) { require(expected.path().contains("unexpected"), "field diagnostic changed"); }
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }
    static void owned(Publication publication) {
        for (var artifact : publication.artifacts()) require(artifact.id().publication() == publication.id(), "artifact publication owner");
        for (var unit : publication.units()) {
            require(unit.id().publication() == publication.id(), "unit publication owner");
            for (var entry : unit.entries()) require(entry.id().unit() == unit.id(), "entry unit owner");
            for (var sequence : unit.sequences()) {
                require(sequence.label().unit() == unit.id(), "label unit owner");
                require(sequence.terminator().header().id().unit() == unit.id(), "operation unit owner");
            }
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
