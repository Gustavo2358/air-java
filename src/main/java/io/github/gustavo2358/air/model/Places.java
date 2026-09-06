package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Places {
    private Places() {}
    public record ObjectPlace(Operand.Header header, ObjectId object) implements Place {
        public ObjectPlace {
            header = Objects.requireNonNull(header, "header");
            object = Objects.requireNonNull(object, "object");
            
        }

    }
    public record Choice(Operand.Header header, List<Place> candidates, Scopes.MemoryBound remainder, Types.TypeRef typeRef, Optional<PremiseId> knownRemainderDomainProof) implements Place {
        public Choice {
            header = Objects.requireNonNull(header, "header");
            candidates = List.copyOf(candidates);
            remainder = Objects.requireNonNull(remainder, "remainder");
            typeRef = Objects.requireNonNull(typeRef, "typeRef");
            knownRemainderDomainProof = Objects.requireNonNull(knownRemainderDomainProof, "knownRemainderDomainProof");
            if (candidates.isEmpty() && remainder instanceof Scopes.NoMemory) throw new IllegalArgumentException("empty closed choice");
        }

    }
    public record RegionSlice(Operand.Header header, StorageId region, Expression offset, Expression length, Memory.Codec codec, Types.TypeRef typeRef, Optional<PremiseId> accessProof) implements Place {
        public RegionSlice {
            header = Objects.requireNonNull(header, "header");
            region = Objects.requireNonNull(region, "region");
            offset = Objects.requireNonNull(offset, "offset");
            length = Objects.requireNonNull(length, "length");
            codec = Objects.requireNonNull(codec, "codec");
            typeRef = Objects.requireNonNull(typeRef, "typeRef");
            accessProof = Objects.requireNonNull(accessProof, "accessProof");
            
        }

    }

}
