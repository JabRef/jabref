package org.jabref.gui.undo;

import java.util.Optional;

import org.jabref.model.change.FieldEdit;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The manager updates its properties on the JavaFX thread, so the toolkit has to be up.
@ExtendWith(ApplicationExtension.class)
class UndoManagerTest {

    private final UndoManager undoRedoManager = new UndoManager();
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        entry = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Einstein");
    }

    private FieldEdit setAuthor(String value) {
        String before = entry.getField(StandardField.AUTHOR).orElse(null);
        entry.setField(StandardField.AUTHOR, value);
        return new FieldEdit(entry, StandardField.AUTHOR, before, value);
    }

    @Test
    void aPushedChangeCanBeUndoneAndRedone() {
        undoRedoManager.push(setAuthor("Bohr"));

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
        undoRedoManager.push(setAuthor("Bohr"));
        undoRedoManager.undo();
        assertTrue(undoRedoManager.canRedo());

        undoRedoManager.push(setAuthor("Planck"));

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
            undoRedoManager.push(setAuthor("Planck"));
        });

        undoRedoManager.undo();

        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());
        assertFalse(undoRedoManager.canUndo());
    }

    /// The property updates are posted to the JavaFX thread rather than applied inline, so
    /// each assertion has to let that queue drain first.
    @Test
    void propertiesFollowTheStacks() {
        assertFalse(undoRedoManager.undoableProperty().get());

        undoRedoManager.push(setAuthor("Bohr"));
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(undoRedoManager.undoableProperty().get());
        assertFalse(undoRedoManager.redoableProperty().get());

        undoRedoManager.undo();
        WaitForAsyncUtils.waitForFxEvents();
        assertFalse(undoRedoManager.undoableProperty().get());
        assertTrue(undoRedoManager.redoableProperty().get());
    }

    @Test
    void undoingBackToTheSavedPositionReportsUnchanged() {
        undoRedoManager.push(setAuthor("Bohr"));
        undoRedoManager.markUnchanged();
        assertFalse(undoRedoManager.hasChanged());

        undoRedoManager.push(setAuthor("Planck"));
        assertTrue(undoRedoManager.hasChanged());

        undoRedoManager.undo();
        assertFalse(undoRedoManager.hasChanged());
    }
}
