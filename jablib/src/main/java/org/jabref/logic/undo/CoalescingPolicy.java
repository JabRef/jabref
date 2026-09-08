package org.jabref.logic.undo;

import java.util.Objects;
import java.util.Optional;

import org.jabref.model.undo.BibChange;
import org.jabref.model.undo.UndoableFieldChange;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Decides whether a change continues the step on top of the undo stack instead of starting a new
/// one.
///
/// Typing is what this exists for: a field editor records one change per keystroke, so without a
/// rule the user has to press Ctrl+Z once per character to take a title back. What counts as one
/// user action is editing policy, driven by how people type, and it changes independently of what
/// a change *is* — which is why it lives here rather than as a `merge` method on
/// [org.jabref.model.undo.ChangeSet]. A second rule can arrive without touching the value model.
///
/// [org.jabref.logic.util.CoarseChangeFilter] answers the same question for autosave, backup and
/// shared-database writes, and deliberately answers it differently: see there for why the two
/// rules are not shared.
///
/// The journal decides *when* to ask: only for a change recorded as [EditSource#TYPING], never
/// inside a recording block, never across a save, and never across a boundary a caller drew with
/// [UndoManager#endStep]. A command that writes the field the user was typing in is therefore its
/// own step, however well it would have fitted the rule below.
@FunctionalInterface
@NullMarked
public interface CoalescingPolicy {

    /// Every change is its own step.
    CoalescingPolicy NONE = (_, _) -> Optional.empty();

    /// Keystrokes in one field become one step, up to the end of a word.
    ///
    /// Two field changes are one action when they describe the same field of the same entry and
    /// the second starts where the first ended — the shape a run of keystrokes has. The merged
    /// change spans the run: from the value the field held before the first keystroke to the value
    /// it holds after the last.
    ///
    /// A completed word ends the run, the way every other editor breaks a run of typing. Without
    /// it, Ctrl+Z on a typo at the end of an abstract takes back the whole abstract: Ctrl+Y brings
    /// it back, but a user who has just watched a paragraph vanish does not think of Ctrl+Y first.
    CoalescingPolicy CONSECUTIVE_FIELD_EDITS = (previous, next) -> {
        if ((previous instanceof UndoableFieldChange first)
                && (next instanceof UndoableFieldChange second)
                && (first.entry() == second.entry())
                && first.field().equals(second.field())
                // The run has to be unbroken: if what the first change produced is not what the
                // second takes as its prior value, something happened in between that a single
                // change cannot describe.
                && Objects.equals(first.after(), second.before())
                && !endsInWhitespace(first.after())) {
            return Optional.of(new UndoableFieldChange(first.entry(), first.field(), first.before(), second.after()));
        }
        return Optional.empty();
    };

    /// @return the change to replace the top of the stack with, or empty to push `next` as a step
    ///         of its own
    Optional<BibChange> merge(BibChange previous, BibChange next);

    /// Whether the run so far ends on a word boundary. An absent field counts as empty: there is
    /// no word to have finished.
    private static boolean endsInWhitespace(@Nullable String value) {
        String run = Objects.toString(value, "");
        return !run.isEmpty() && Character.isWhitespace(run.charAt(run.length() - 1));
    }
}
