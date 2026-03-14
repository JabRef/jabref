package org.jabref.logic.relatedwork;

import java.util.Optional;

import org.jabref.model.FieldChange;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record RelatedWorkInsertionResult(
        RelatedWorkMatchResult matchResult,
        RelatedWorkInsertionStatus status,
        Optional<FieldChange> fieldChange
) {
    public boolean success() {
        return status == RelatedWorkInsertionStatus.INSERTED || status == RelatedWorkInsertionStatus.UNCHANGED;
    }
}
