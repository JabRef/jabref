package org.jabref.logic.relatedwork;

import org.jabref.model.FieldChange;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface RelatedWorkInsertionResult {
    record Inserted(RelatedWorkMatchResult matchResult, FieldChange fieldChange) implements RelatedWorkInsertionResult {
    }

    record Unchanged(RelatedWorkMatchResult matchResult) implements RelatedWorkInsertionResult {
    }
}
