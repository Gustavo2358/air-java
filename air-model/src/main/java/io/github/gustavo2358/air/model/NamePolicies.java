package io.github.gustavo2358.air.model;

import java.util.HashSet;
import java.util.Set;

/** Inventories opaque, versioned name policies; never interprets resource names. */
public final class NamePolicies {
    private NamePolicies() { }
    public static Set<Capabilities.Capability> extensions(Publication publication) {
        var result = new HashSet<Capabilities.Capability>();
        for (var unit : publication.units()) for (var sequence : unit.sequences())
            if (sequence.terminator() instanceof Operations.Invoke invoke) {
                if (invoke.target() instanceof Interactions.LiteralTarget t) add(result, t.namePolicy());
                if (invoke.target() instanceof Interactions.ComputedTarget t) add(result, t.namePolicy());
            }
        for (var resource : publication.resources()) {
            if (resource.description() instanceof Interactions.LiteralTarget t) add(result, t.namePolicy());
            if (resource.description() instanceof Interactions.ComputedResource t) add(result, t.namePolicy());
        }
        return Set.copyOf(result);
    }
    private static void add(Set<Capabilities.Capability> result, Interactions.NamePolicy policy) {
        if (policy instanceof Interactions.ExtensionName e)
            result.add(new Capabilities.Capability(e.name(), e.version()));
    }
}
