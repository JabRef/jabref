package org.jabref.logic.undo;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.undo.UndoableFieldChange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A field editor records one change per keystroke, so without a rule Ctrl+Z takes back a letter
/// rather than a word. These are the cases the rule has to get right.
class KeystrokeCoalescingTest {

    private final JabRefUndoManager journal = new JabRefUndoManager();

    private BibEntry entry;

    @BeforeEach
    void setUp() {
        entry = new BibEntry(StandardEntryType.Article);
    }

    /// Types `text` one character at a time, the way an editor records it.
    private void type(String text) {
        type(StandardField.TITLE, text);
    }

    private void type(StandardField field, String text) {
        for (int length = 1; length <= text.length(); length++) {
            String value = text.substring(0, length);
            journal.applyEdit(new UndoableFieldChange(entry, field, entry.getField(field).orElse(null), value));
        }
    }

    private Optional<String> title() {
        return entry.getField(StandardField.TITLE);
    }

    // [utest->req~logic.undo.typing-is-one-step~1]
    @Test
    void aRunOfKeystrokesIsOneStep() {
        type("Relativity");

        assertEquals(Optional.of("Relativity"), title());

        journal.undo();

        assertEquals(Optional.empty(), title(), "undo took back one keystroke rather than the run");
        assertFalse(journal.canUndo(), "the run left more than one step behind");
    }

    /// And forward again: the merged step redoes as a whole.
    @Test
    void theRunRedoesAsAWhole() {
        type("Relativity");
        journal.undo();

        journal.redo();

        assertEquals(Optional.of("Relativity"), title());
    }

    /// The editor says when a run ends — it knows, the journal does not.
    @Test
    void aBoundaryEndsTheRun() {
        type("Rela");
        journal.endStep();
        type("tivity");

        journal.undo();

        assertEquals(Optional.of("Rela"), title());
        assertTrue(journal.canUndo());
    }

    /// Editing something else ends the run without anyone saying so, because the step on top is no
    /// longer the one being typed into.
    @Test
    void anotherFieldEndsTheRun() {
        type("Rela");
        type(StandardField.AUTHOR, "Einstein");
        type("tivity");

        journal.undo();

        assertEquals(Optional.of("Rela"), title(), "the second run merged into the first");
    }

    /// Saving mid-run is a position the user can ask to come back to, so the step it stands at is
    /// not merged into. Undoing once has to land on what was saved.
    @Test
    void arunIsNotMergedIntoAcrossASave() {
        type("Rela");
        journal.markUnchanged();
        assertFalse(journal.hasChanged());

        type("tivity");

        assertTrue(journal.hasChanged(), "the library was reported as saved after typing more");

        journal.undo();

        assertEquals(Optional.of("Rela"), title());
        assertFalse(journal.hasChanged(), "undoing landed somewhere other than the saved position");
    }

    /// Typing a word and deleting it again leaves the library where it was, so there is nothing to
    /// undo — and, since the marker follows the position, nothing to save either.
    @Test
    void aRunThatEndsWhereItStartedIsNoStepAtAll() {
        entry.setField(StandardField.TITLE, "Relativity");
        journal.markUnchanged();

        journal.applyEdit(new UndoableFieldChange(entry, StandardField.TITLE, "Relativity", "Relativit"));
        journal.applyEdit(new UndoableFieldChange(entry, StandardField.TITLE, "Relativit", "Relativity"));

        assertEquals(Optional.of("Relativity"), title());
        assertFalse(journal.canUndo(), "a step that does nothing is on the stack");
        assertFalse(journal.hasChanged(), "the library reports itself changed although it is not");
    }

    /// A command's step is a whole user action: it neither continues what was being typed nor
    /// invites the next keystroke to continue it.
    @Test
    void aRecordedBlockIsNeverContinued() {
        type("Rela");
        journal.addEdit("Cleanup", edit ->
                edit.applyEdit(new UndoableFieldChange(entry, StandardField.YEAR, null, "1905")));
        type("tivity");

        journal.undo();
        assertEquals(Optional.of("Rela"), title(), "typing continued the command's step");

        journal.undo();
        assertEquals(Optional.empty(), entry.getField(StandardField.YEAR));

        journal.undo();
        assertEquals(Optional.empty(), title());
    }

    /// Undoing ends the run, or typing after it would fold the redone-away text into the next step.
    @Test
    void undoEndsTheRun() {
        type("Rela");
        journal.undo();

        type("Quantum");

        journal.undo();

        assertEquals(Optional.empty(), title());
        assertFalse(journal.canUndo());
    }

    /// The rule is a policy, and a journal can be given a different one.
    @Test
    void withoutThePolicyEveryKeystrokeIsAStep() {
        JabRefUndoManager plain = new JabRefUndoManager(CoalescingPolicy.NONE);

        plain.applyEdit(new UndoableFieldChange(entry, StandardField.TITLE, null, "R"));
        plain.applyEdit(new UndoableFieldChange(entry, StandardField.TITLE, "R", "Re"));

        plain.undo();

        assertEquals(Optional.of("R"), title());
    }
}
