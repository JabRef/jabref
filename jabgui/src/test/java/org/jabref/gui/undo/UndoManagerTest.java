package org.jabref.gui.undo;

import java.util.Optional;

import org.jabref.model.change.UndoableFieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndoManagerTest {

    private final UndoManager undoRedoManager = new UndoManager();
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        entry = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Einstein");
    }

    private UndoableFieldChange setAuthor(String value) {
        String before = entry.getField(StandardField.AUTHOR).orElse(null);
        entry.setField(StandardField.AUTHOR, value);
        return new UndoableFieldChange(entry, StandardField.AUTHOR, before, value);
    }

    @Test
    void aPushedChangeCanBeUndoneAndRedone() {
        undoRedoManager.addEdit(setAuthor("Bohr"));

        assertTrue(undoRedoManager.canUndo());
        undoRedoManager.undo();
        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());

        assertTrue(undoRedoManager.canRedo());
        undoRedoManager.redo();
        assertEquals("Bohr", entry.getField(StandardField.AUTHOR).orElseThrow());
    }

    @Test
    void undoingAnEmptyStackDoesNothing() {
        undoRedoManager.undo();

        assertFalse(undoRedoManager.canUndo());
        assertFalse(undoRedoManager.canRedo());
    }

    @Test
    void aNewChangeDiscardsTheRedoBranch() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        undoRedoManager.undo();
        assertTrue(undoRedoManager.canRedo());

        undoRedoManager.addEdit(setAuthor("Planck"));

        assertFalse(undoRedoManager.canRedo());
    }

    @Test
    void aRecordedBlockUndoesAsOneStep() {
        undoRedoManager.record("edit", recorder -> {
            recorder.record(entry.setField(StandardField.AUTHOR, "Bohr"));
            recorder.record(entry.setField(StandardField.TITLE, "Relativity"));
        });

        undoRedoManager.undo();

        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());
        assertEquals(Optional.empty(), entry.getField(StandardField.TITLE));
        assertFalse(undoRedoManager.canUndo());
    }

    @Test
    void aBlockThatChangesNothingIsNotPushed() {
        undoRedoManager.record("no-op", recorder ->
                // Setting the value it already has reports no change.
                recorder.record(entry.setField(StandardField.AUTHOR, "Einstein")));

        assertFalse(undoRedoManager.canUndo());
    }

    @Test
    void nestedBlocksProduceASingleStep() {
        undoRedoManager.record("outer", outer -> {
            outer.record(entry.setField(StandardField.AUTHOR, "Bohr"));
            undoRedoManager.record("inner", inner -> inner.record(entry.setField(StandardField.TITLE, "Relativity")));
        });

        undoRedoManager.undo();

        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());
        assertEquals(Optional.empty(), entry.getField(StandardField.TITLE));
        assertFalse(undoRedoManager.canUndo());
    }

    @Test
    void pushInsideABlockJoinsIt() {
        undoRedoManager.record("outer", outer -> {
            outer.record(entry.setField(StandardField.AUTHOR, "Bohr"));
            undoRedoManager.addEdit(setAuthor("Planck"));
        });

        undoRedoManager.undo();

        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());
        assertFalse(undoRedoManager.canUndo());
    }

    @Test
    void undoingBackToTheSavedPositionReportsUnchanged() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        undoRedoManager.markUnchanged();
        assertFalse(undoRedoManager.hasChanged());

        undoRedoManager.addEdit(setAuthor("Planck"));
        assertTrue(undoRedoManager.hasChanged());

        undoRedoManager.undo();
        assertFalse(undoRedoManager.hasChanged());
    }
}
