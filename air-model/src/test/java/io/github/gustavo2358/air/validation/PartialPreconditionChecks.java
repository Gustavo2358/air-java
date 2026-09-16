package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

public final class PartialPreconditionChecks {
    private PartialPreconditionChecks() { }
    public static void main(String[] args) { run(); System.out.println("EP_PARTIAL_PRECONDITIONS=PASS"); }
    static void require(boolean value,String message) { if(!value)throw new AssertionError(message); }
    static void run() {
        var actual=RegionalFitChecks.check(Memory.AsciiText.INSTANCE,4,4," ");
        require(actual.status()==ValidationResult.Status.INCOMPLETE_VALIDATION,"validity cannot be upgraded");
        var scope=actual.unprovedOperationPreconditions().orElseThrow();
        require(scope.size()==1&&scope.iterator().next().localId().equals("fit"),"exact operation scope from real validator");
        try{scope.clear();throw new AssertionError("mutable permission");}catch(UnsupportedOperationException expected){}
        var operation=scope.iterator().next();var statistics=actual.statistics();
        var issue=new ValidationIssue(ValidationIssue.Kind.VALIDATION_LIMIT,"PRECONDITION_NOT_DISCHARGED",Optional.of(operation),"unknown totality");
        var operand=new OperandId(new OperationOwner(operation),"read");
        require(new ValidationResult(List.of(new ValidationIssue(issue.kind(),issue.rule(),Optional.of(operand),issue.detail())),statistics).unprovedOperationPreconditions().orElseThrow().equals(scope),"operand owner scope");
        for(var kind:List.of(ValidationIssue.Kind.INVALID_IR,ValidationIssue.Kind.RESOURCE_LIMIT,ValidationIssue.Kind.UNSUPPORTED_CAPABILITY)) {
            var totals=new EnumMap<ValidationIssue.Kind,Long>(ValidationIssue.Kind.class);totals.put(issue.kind(),1L);totals.put(kind,1L);
            require(new ValidationResult(List.of(issue),statistics,new ValidationResult.Diagnostics(totals,true)).unprovedOperationPreconditions().isEmpty(),"omitted "+kind+" cannot grant permission");
        }
        for(var diagnostics:List.of(new ValidationResult.Diagnostics(Map.of(issue.kind(),2L),true),new ValidationResult.Diagnostics(Map.of(issue.kind(),1L),false)))
            require(new ValidationResult(List.of(issue),statistics,diagnostics).unprovedOperationPreconditions().isEmpty(),"truncated scope or interrupted validation");
        for(Optional<Id> owner:List.<Optional<Id>>of(Optional.empty(),Optional.of(new EntryId(operation.unit(),"entry"))))
            require(new ValidationResult(List.of(new ValidationIssue(issue.kind(),issue.rule(),owner,"unproved entry")),statistics).unprovedOperationPreconditions().isEmpty(),"entry/unknown owner stays outside policy");
        require(new ValidationResult(List.of(new ValidationIssue(issue.kind(),"OTHER_LIMIT",Optional.of(operation),"other")),statistics).unprovedOperationPreconditions().isEmpty(),"unclassified limit");
    }
}
