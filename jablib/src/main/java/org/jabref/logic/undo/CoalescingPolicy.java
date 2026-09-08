package org.jabref.logic.undo;

import java.util.Objects;
import java.util.Optional;

import org.jabref.model.undo.BibChange;
import org.jabref.model.undo.UndoableFieldChange;

import org.jspecify.annotations.NullMarked;

/// Decides whether a change continues the step on top of the undo stack instead of starting a new
/// one.
///
/// Typing is what this exists for: a field editor records one change per keystroke, so without a
/// rule the user has to press Ctrl+Z once per character to take a title back. What counts as one
/// user action is editing policy, driven by how people type, and it changes independently of what
/// a change *is* — which is why it lives here rather than as a `merge` method on
/// [org.jabref.model.undo.ChangeSet]. A second rule can arrive without touching the value model.
///
/// The journal decides *when* to ask: never inside a recording block, never across a save, and
/// never across a boundary a caller drew with [UndoManager#endStep].
@FunctionalInterface
@NullMarked
public interface CoalescingPolicy {

    /// Every change is its own step.
    CoalescingPolicy NONE = (_, _) -> Optional.empty();

    /// Keystrokes in one field become one step.
    ///
    /// Two field changes are one action when they describe the same field of the same entry and
    /// the second starts where the first ended — the shape a run of keystrokes has. The merged
    /// change spans the run: from the value the field held before the first keystroke to the value
    /// it holds after the last.
    CoalescingPolicy CONSECUTIVE_FIELD_EDITS = (previous, next) -> {
        if (!(previous instanceof UndoableFieldChange first) || !(next instanceof UndoableFieldChange second)) {
            return Optional.empty();
        }
        boolean sameTarget = (first.entry() == second.entry()) && first.field().equals(second.field());
        // The run has to be unbroken: if what the first change produced is not what the second
        // takes as its prior value, something happened in between that this cannot describe.
        boolean continues = Objects.equals(first.after(), second.before());
        if (!sameTarget || !continues) {
            return Optional.empty();
        }
        return Optional.of(new UndoableFieldChange(first.entry(), first.field(), first.before(), second.after()));
    };

    /// @return the change to replace the top of the stack with, or empty to push `next` as a step
    ///         of its own
    Optional<BibChange> merge(BibChange previous, BibChange next);
}
