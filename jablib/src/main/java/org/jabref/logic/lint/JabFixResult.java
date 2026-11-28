package org.jabref.logic.lint;

import java.util.List;

import org.jabref.logic.lint.rule.Finding;
import org.jabref.model.FieldChange;

import org.jspecify.annotations.NullMarked;

/// What a [JabFix] run reported and changed.
///
/// @param findings everything the rules reported, in the order they were reported
/// @param changes  what the applied repairs changed, for an undo manager to collect
@NullMarked
public record JabFixResult(List<Finding> findings, List<FieldChange> changes) {
    public JabFixResult {
        findings = List.copyOf(findings);
        changes = List.copyOf(changes);
    }
}
