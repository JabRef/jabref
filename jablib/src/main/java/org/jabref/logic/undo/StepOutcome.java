package org.jabref.logic.undo;

import org.jspecify.annotations.NullMarked;

/// What an undo or a redo did: which step it moved, and whether all of it went through.
///
/// Both are things the caller cannot work out for itself — the name because only the journal
/// holds the stacks, the completeness because only applying the step reveals it.
///
/// @param name     the step, named as the user would recognise it — see [BibChangeDescriber]
/// @param complete whether every change in the step could be applied
@NullMarked
public record StepOutcome(String name, boolean complete) {
}
