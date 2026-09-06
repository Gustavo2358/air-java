package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Proofs {
    private Proofs() {}
    public sealed interface DomainSubject permits ObjectDomain, CellDomain, OperandDomain, ParameterDomain, ResultDomain, CallParameterDomain, CallResultDomain {}
    public record ObjectDomain(ObjectId object) implements DomainSubject {
        public ObjectDomain {
            object = Objects.requireNonNull(object, "object");
            
        }

    }
    public record CellDomain(StorageId cell) implements DomainSubject {
        public CellDomain {
            cell = Objects.requireNonNull(cell, "cell");
            
        }

    }
    public record OperandDomain(OperandId operand) implements DomainSubject {
        public OperandDomain {
            operand = Objects.requireNonNull(operand, "operand");
            
        }

    }
    public record ParameterDomain(EntryId entry, int position) implements DomainSubject {
        public ParameterDomain {
            entry = Objects.requireNonNull(entry, "entry");
            nonNegative(position, "position");
            
        }

    }
    public record ResultDomain(EntryId entry, int position) implements DomainSubject {
        public ResultDomain {
            entry = Objects.requireNonNull(entry, "entry");
            nonNegative(position, "position");
            
        }

    }
    public record CallParameterDomain(OperationId invocation, Interactions.SignatureTarget signature, int position) implements DomainSubject {
        public CallParameterDomain {
            invocation = Objects.requireNonNull(invocation, "invocation");
            signature = Objects.requireNonNull(signature, "signature");
            nonNegative(position, "position");
            
        }

    }
    public record CallResultDomain(OperationId invocation, Interactions.SignatureTarget signature, int position) implements DomainSubject {
        public CallResultDomain {
            invocation = Objects.requireNonNull(invocation, "invocation");
            signature = Objects.requireNonNull(signature, "signature");
            nonNegative(position, "position");
            
        }

    }
    public sealed interface DomainProofScope permits PublicationDomain, UnitDomain, EntryDomain, OperationDomain, InvocationDomain, Intersection {}
    public enum PublicationDomain implements DomainProofScope { INSTANCE }
    public record UnitDomain(UnitId unit) implements DomainProofScope {
        public UnitDomain {
            unit = Objects.requireNonNull(unit, "unit");
            
        }

    }
    public record EntryDomain(EntryId entry) implements DomainProofScope {
        public EntryDomain {
            entry = Objects.requireNonNull(entry, "entry");
            
        }

    }
    public record OperationDomain(OperationId operation) implements DomainProofScope {
        public OperationDomain {
            operation = Objects.requireNonNull(operation, "operation");
            
        }

    }
    public record InvocationDomain(OperationId invocation) implements DomainProofScope {
        public InvocationDomain {
            invocation = Objects.requireNonNull(invocation, "invocation");
            
        }

    }
    public record Intersection(DomainProofScope left, DomainProofScope right) implements DomainProofScope {
        public Intersection {
            left = Objects.requireNonNull(left, "left");
            right = Objects.requireNonNull(right, "right");
            
        }

    }
    public sealed interface ProofSite permits EntrySite, OperationSite, InvocationSite {}
    public record EntrySite(EntryId entry) implements ProofSite {
        public EntrySite {
            entry = Objects.requireNonNull(entry, "entry");
            
        }

    }
    public record OperationSite(OperationId operation) implements ProofSite {
        public OperationSite {
            operation = Objects.requireNonNull(operation, "operation");
            
        }

    }
    public record InvocationSite(OperationId invocation) implements ProofSite {
        public InvocationSite {
            invocation = Objects.requireNonNull(invocation, "invocation");
            
        }

    }
    public sealed interface Assertion permits SameDomain, DisjointStorage, SafetyAssertion {}
    public record SameDomain(DomainSubject left, DomainSubject right, DomainProofScope scope) implements Assertion {
        public SameDomain {
            left = Objects.requireNonNull(left, "left");
            right = Objects.requireNonNull(right, "right");
            scope = Objects.requireNonNull(scope, "scope");
            
        }

    }
    public record DisjointStorage(List<StorageId> storage, Scopes.FactScope scope) implements Assertion {
        public DisjointStorage {
            storage = List.copyOf(storage);
            scope = Objects.requireNonNull(scope, "scope");
            if(storage.size()<2 || new HashSet<>(storage).size()!=storage.size()) throw new IllegalArgumentException("separation needs distinct storage identities");
        }

    }
    public enum SafetyProperty { VALID_PURE_ACCESS, VALID_TEXT_SLICE, VALID_CODEC_WRITE, CHOICE_REMAINDER_DOMAIN, EXTENSION_EQUALITY_DEFINED }
    public record SafetyAssertion(SafetyProperty property, List<Id> subjects, DomainProofScope scope) implements Assertion {
        public SafetyAssertion {
            property = Objects.requireNonNull(property, "property");
            subjects = List.copyOf(subjects);
            scope = Objects.requireNonNull(scope, "scope");
            if(subjects.isEmpty()) throw new IllegalArgumentException("safety assertion needs subjects");
        }

    }
    public record Premise(PremiseId id, String authority, String justification, OriginId origin, Assertion assertion) {
        public Premise {
            id = Objects.requireNonNull(id, "id");
            authority = text(authority, "authority");
            justification = text(justification, "justification");
            origin = Objects.requireNonNull(origin, "origin");
            assertion = Objects.requireNonNull(assertion, "assertion");
            
        }

    }

}
