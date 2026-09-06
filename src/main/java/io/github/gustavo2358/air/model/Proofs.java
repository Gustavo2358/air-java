package io.github.gustavo2358.air.model;

import java.math.BigInteger;
import java.util.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Normative AIR premises and the closed static scope algebra for sameDomain. */
public final class Proofs {
    private Proofs() {}

    public sealed interface DomainSubject permits ObjectDomain, CellDomain, OperandDomain,
            ParameterDomain, ResultDomain, CallParameterDomain, CallResultDomain,
            ExternalParameterDomain, ExternalResultDomain {}
    public record ObjectDomain(ObjectId object) implements DomainSubject {
        public ObjectDomain { object = Objects.requireNonNull(object, "object"); }
    }
    public record CellDomain(StorageId cell) implements DomainSubject {
        public CellDomain { cell = Objects.requireNonNull(cell, "cell"); }
    }
    public record OperandDomain(OperandId operand) implements DomainSubject {
        public OperandDomain { operand = Objects.requireNonNull(operand, "operand"); }
    }
    public record ParameterDomain(EntryId entry, BigInteger position) implements DomainSubject {
        public ParameterDomain {
            entry = Objects.requireNonNull(entry, "entry");
            position = Objects.requireNonNull(position, "position");
            nonNegative(position, "position");
        }
    }
    public record ResultDomain(EntryId entry, BigInteger position) implements DomainSubject {
        public ResultDomain {
            entry = Objects.requireNonNull(entry, "entry");
            position = Objects.requireNonNull(position, "position");
            nonNegative(position, "position");
        }
    }
    public record CallParameterDomain(OperationId invocation, EntryId entry,
                                      BigInteger position) implements DomainSubject {
        public CallParameterDomain {
            invocation = Objects.requireNonNull(invocation, "invocation");
            entry = Objects.requireNonNull(entry, "entry");
            position = Objects.requireNonNull(position, "position");
            nonNegative(position, "position");
        }
    }
    public record CallResultDomain(OperationId invocation, EntryId entry,
                                   BigInteger position) implements DomainSubject {
        public CallResultDomain {
            invocation = Objects.requireNonNull(invocation, "invocation");
            entry = Objects.requireNonNull(entry, "entry");
            position = Objects.requireNonNull(position, "position");
            nonNegative(position, "position");
        }
    }
    public record ExternalParameterDomain(OperationId invocation,
                                          BigInteger position) implements DomainSubject {
        public ExternalParameterDomain {
            invocation = Objects.requireNonNull(invocation, "invocation");
            position = Objects.requireNonNull(position, "position");
            nonNegative(position, "position");
        }
    }
    public record ExternalResultDomain(OperationId invocation,
                                       BigInteger position) implements DomainSubject {
        public ExternalResultDomain {
            invocation = Objects.requireNonNull(invocation, "invocation");
            position = Objects.requireNonNull(position, "position");
            nonNegative(position, "position");
        }
    }

    public sealed interface DomainProofScope permits PublicationDomain, UnitDomain,
            EntryDomain, OperationDomain, InvocationDomain, Intersection {}
    public enum PublicationDomain implements DomainProofScope { INSTANCE }
    public record UnitDomain(UnitId unit) implements DomainProofScope {
        public UnitDomain { unit = Objects.requireNonNull(unit, "unit"); }
    }
    public record EntryDomain(EntryId entry) implements DomainProofScope {
        public EntryDomain { entry = Objects.requireNonNull(entry, "entry"); }
    }
    public record OperationDomain(OperationId operation) implements DomainProofScope {
        public OperationDomain { operation = Objects.requireNonNull(operation, "operation"); }
    }
    public record InvocationDomain(OperationId invocation) implements DomainProofScope {
        public InvocationDomain { invocation = Objects.requireNonNull(invocation, "invocation"); }
    }
    public record Intersection(DomainProofScope left, DomainProofScope right)
            implements DomainProofScope {
        public Intersection {
            left = Objects.requireNonNull(left, "left");
            right = Objects.requireNonNull(right, "right");
        }
    }

    public sealed interface ProofSite permits EntrySite, OperationSite, InvocationSite {}
    public record EntrySite(EntryId entry) implements ProofSite {
        public EntrySite { entry = Objects.requireNonNull(entry, "entry"); }
    }
    public record OperationSite(OperationId operation) implements ProofSite {
        public OperationSite { operation = Objects.requireNonNull(operation, "operation"); }
    }
    public record InvocationSite(OperationId invocation) implements ProofSite {
        public InvocationSite { invocation = Objects.requireNonNull(invocation, "invocation"); }
    }

    public sealed interface Assertion permits SameDomain, DisjointStorage {}
    public record SameDomain(DomainSubject left, DomainSubject right,
                             DomainProofScope scope) implements Assertion {
        public SameDomain {
            left = Objects.requireNonNull(left, "left");
            right = Objects.requireNonNull(right, "right");
            scope = Objects.requireNonNull(scope, "scope");
        }
    }
    public record DisjointStorage(List<StorageId> storage) implements Assertion {
        public DisjointStorage { storage = List.copyOf(storage); }
    }
    public record Premise(PremiseId id, String authority, String justification,
                          OriginId origin, Assertion assertion) {
        public Premise {
            id = Objects.requireNonNull(id, "id");
            authority = text(authority, "authority");
            justification = text(justification, "justification");
            origin = Objects.requireNonNull(origin, "origin");
            assertion = Objects.requireNonNull(assertion, "assertion");
        }
    }
}
