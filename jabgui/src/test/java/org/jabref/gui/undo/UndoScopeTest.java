package org.jabref.gui.undo;

import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndoScopeTest {

    private final UndoManager undoManager = new UndoManager();
    private BibEntry entry;
    private UndoScope undoScope;

    @BeforeEach
    void setUp() {
        entry = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Einstein");
        undoScope = new UndoScope(undoManager);
    }

    @Test
    void recordedChangesCanBeUndoneAsOneStep() {
        undoScope.record("edit", recorder -> {
            recorder.record(entry.setField(StandardField.AUTHOR, "Bohr"));
            recorder.record(entry.setField(StandardField.TITLE, "Relativity"));
        });

        assertTrue(undoManager.canUndo());
        undoManager.undo();

        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());
        assertEquals(Optional.empty(), entry.getField(StandardField.TITLE));
        assertFalse(undoManager.canUndo());
    }

    @Test
    void redoReappliesTheWholeStep() {
        undoScope.record("edit", recorder -> recorder.record(entry.setField(StandardField.AUTHOR, "Bohr")));

        undoManager.undo();
        undoManager.redo();

        assertEquals("Bohr", entry.getField(StandardField.AUTHOR).orElseThrow());
    }

    @Test
    void aScopeThatChangesNothingIsNotPushed() {
        undoScope.record("no-op", recorder -> {
            // Setting the value it already has reports no change.
            recorder.record(entry.setField(StandardField.AUTHOR, "Einstein"));
        });

        assertFalse(undoManager.canUndo());
    }

    @Test
    void nestedScopesProduceASingleUndoStep() {
        undoScope.record("outer", outer -> {
            outer.record(entry.setField(StandardField.AUTHOR, "Bohr"));
            undoScope.record("inner", inner -> inner.record(entry.setField(StandardField.TITLE, "Relativity")));
        });

        undoManager.undo();

        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());
        assertEquals(Optional.empty(), entry.getField(StandardField.TITLE));
        assertFalse(undoManager.canUndo());
    }

    @Test
    void theStepIsNamedAfterTheOutermostScope() {
        undoScope.record("Manage keywords", recorder -> recorder.record(entry.setField(StandardField.AUTHOR, "Bohr")));

        assertTrue(undoManager.getUndoPresentationName().contains("Manage keywords"));
    }
}
