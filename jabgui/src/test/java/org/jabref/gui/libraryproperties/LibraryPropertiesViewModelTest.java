package org.jabref.gui.libraryproperties;

import java.util.List;
import java.util.Optional;

import javafx.scene.Node;

import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.undo.UndoablePreambleChange;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class LibraryPropertiesViewModelTest {

    private final BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase());
    private final JabRefUndoManager undoManager = new JabRefUndoManager();

    /// A tab that writes what the dialog's tabs write: straight into the library, recording
    /// nothing of its own.
    private record SettingTab(Runnable store) implements PropertiesTab {
        @Override
        public Node getBuilder() {
            throw new UnsupportedOperationException("The dialog is not built in this test");
        }

        @Override
        public String getTabName() {
            return "test";
        }

        @Override
        public void setValues() {
        }

        @Override
        public void storeSettings() {
            store.run();
        }

        @Override
        public boolean validateSettings() {
            return true;
        }
    }

    private LibraryPropertiesViewModel viewModel(PropertiesTab... tabs) {
        return new LibraryPropertiesViewModel(databaseContext, undoManager, List.of(tabs));
    }

    /// Seven tabs write to one metadata instance, so the step is a snapshot pair rather than a
    /// change per setting. Undoing it has to put every tab's setting back.
    @Test
    // [utest->req~logic.undo.library-settings-recorded~1]
    void acceptingTheDialogIsOneUndoStep() {
        databaseContext.getMetaData().setMode(BibDatabaseMode.BIBTEX);

        viewModel(
                new SettingTab(() -> databaseContext.getMetaData().setMode(BibDatabaseMode.BIBLATEX)),
                new SettingTab(() -> databaseContext.getMetaData().setKeywordSeparator(';')))
                .storeAllSettings();

        assertEquals(Optional.of(BibDatabaseMode.BIBLATEX), databaseContext.getMetaData().getMode());
        assertEquals(Optional.of(';'), databaseContext.getMetaData().getKeywordSeparator());

        undoManager.undo();

        assertEquals(Optional.of(BibDatabaseMode.BIBTEX), databaseContext.getMetaData().getMode());
        assertEquals(Optional.empty(), databaseContext.getMetaData().getKeywordSeparator());
        assertFalse(undoManager.canUndo(), "the whole dialog is one step");
    }

    /// What a tab records itself belongs to the same step, so that undoing the dialog does not
    /// leave half of what it wrote behind.
    @Test
    void whatATabRecordsJoinsTheSameStep() {
        viewModel(
                new SettingTab(() -> databaseContext.getMetaData().setMode(BibDatabaseMode.BIBLATEX)),
                new SettingTab(() -> undoManager.applyEdit(
                        new UndoablePreambleChange(databaseContext.getDatabase(), null, "preamble"))))
                .storeAllSettings();

        assertEquals(Optional.of("preamble"), databaseContext.getDatabase().getPreamble());

        undoManager.undo();

        assertEquals(Optional.empty(), databaseContext.getDatabase().getPreamble());
        assertEquals(Optional.empty(), databaseContext.getMetaData().getMode());
        assertFalse(undoManager.canUndo(), "the whole dialog is one step");
    }

    /// Accepting the dialog without touching anything must not put an empty step on the stack:
    /// the next Ctrl+Z would take back the user's previous edit instead.
    @Test
    void acceptingTheDialogUnchangedRecordsNothing() {
        databaseContext.getMetaData().setMode(BibDatabaseMode.BIBTEX);

        viewModel(new SettingTab(() -> databaseContext.getMetaData().setMode(BibDatabaseMode.BIBTEX)))
                .storeAllSettings();

        assertFalse(undoManager.canUndo());
    }

    /// The dialog's step is undoable and redoable like any other.
    @Test
    void theStepCanBeRedone() {
        viewModel(new SettingTab(() -> databaseContext.getMetaData().setMode(BibDatabaseMode.BIBLATEX)))
                .storeAllSettings();

        undoManager.undo();
        undoManager.redo();

        assertEquals(Optional.of(BibDatabaseMode.BIBLATEX), databaseContext.getMetaData().getMode());
        assertTrue(undoManager.canUndo());
    }
}
