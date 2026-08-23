package org.jabref.gui.undo;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.undo.UndoableFieldChange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Unlike [UndoManagerTest], this needs a live toolkit: the point of this class is the hop onto
/// the JavaFX thread, and `Platform.runLater` is what requires one. The properties themselves
/// would not.
@ExtendWith(ApplicationExtension.class)
class UndoManagerPropertiesTest {

    private final UndoManager undoManager = new UndoManager();
    private UndoManagerProperties properties;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        properties = new UndoManagerProperties(undoManager);
        entry = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Einstein");
    }

    private UndoableFieldChange setAuthor(String value) {
        String before = entry.getField(StandardField.AUTHOR).orElse(null);
        entry.setField(StandardField.AUTHOR, value);
        return new UndoableFieldChange(entry, StandardField.AUTHOR, before, value);
    }

    @Test
    void propertiesStartOutMatchingAnEmptyManager() {
        assertFalse(properties.undoableProperty().get());
        assertFalse(properties.redoableProperty().get());
    }

    /// The update is posted to the JavaFX thread rather than applied inline, so each assertion
    /// has to let that queue drain first.
    @Test
    void propertiesFollowTheStacks() {
        undoManager.addEdit(setAuthor("Bohr"));
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(properties.undoableProperty().get());
        assertFalse(properties.redoableProperty().get());

        undoManager.undo();
        WaitForAsyncUtils.waitForFxEvents();
        assertFalse(properties.undoableProperty().get());
        assertTrue(properties.redoableProperty().get());

        undoManager.redo();
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(properties.undoableProperty().get());
        assertFalse(properties.redoableProperty().get());
    }
}
