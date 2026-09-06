package org.jabref.logic.undo;

import org.jabref.model.undo.ApplyResult;

import org.jspecify.annotations.NullMarked;

/// What an undo or a redo did, for the message the user is shown.
///
/// Two things the caller cannot work out for itself: what the step was, which only the journal
/// knows because only it holds the stacks, and how much of it took effect, which only applying
/// it reveals.
///
/// @param description the step, named as the user would recognise it — see [BibChangeDescriber]
/// @param result      what of the step was applied, and what was not
@NullMarked
public record ChangeOutcome(String description, ApplyResult result) {
}
