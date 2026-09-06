package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.util.*;

/**
 * Stateless validation entry point. No mutation, transport, frontend callbacks, or derived analyses.
 * Inspect status AND issues. STRUCTURALLY_VALID is not a producer/consumer conformance certificate.
 */
public final class AirValidator {
    private AirValidator() {}
    public static ValidationResult validate(Publication publication) {
        return validate(publication,ValidationOptions.defaults());
    }
    public static ValidationResult validate(Publication publication,ValidationOptions options) {
        Objects.requireNonNull(publication,"publication"); Objects.requireNonNull(options,"options");
        ValidationContext c=new ValidationContext(publication,options);
        try {
            if(!publication.airVersion().equals(SemanticVersion.AIR_2_0_0)) {
                c.unsupported("AIR_VERSION",publication.id(),"this library targets AIR 2.0.0, not "+publication.airVersion());
            } else {
                c.index.build();
                ReferenceChecks refs=new ReferenceChecks(c); refs.run();
                TypeResolver types=new TypeResolver(c);
                for(Operand operand:c.index.operands.values()) types.type(operand);
                DomainProofEngine domains=new DomainProofEngine(c,types); domains.initialize();
                new OperationChecks(c,refs,types,domains).run();
            }
        } catch(ValidationContext.Limit limit) {
            // Even when diagnostic capacity is exhausted, the limit must remain observable.
            c.issues.add(new ValidationIssue(ValidationIssue.Kind.VALIDATION_LIMIT,"ANALYSIS_LIMIT",Optional.of(publication.id()),limit.getMessage()));
        }
        return new ValidationResult(c.issues,new ValidationResult.Statistics(c.index.identities.size(),c.index.operands.size(),c.index.operations.size(),c.domainQueries));
    }
}
