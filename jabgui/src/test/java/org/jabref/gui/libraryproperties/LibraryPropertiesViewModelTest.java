package org.jabref.gui.libraryproperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.scene.Node;

import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.event.MetaDataChangeSource;
import org.jabref.model.metadata.event.MetaDataChangedEvent;
import org.jabref.model.undo.UndoablePreambleChange;

import com.google.common.eventbus.Subscribe;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class LibraryPropertiesViewModelTest {

    private final BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase());
    private final JabRefUndoManager undoManager = new JabRefUndoManager();

    /// A tab that writes what the dialog's tabs write: into the settings it is handed, recording
    /// nothing of its own.
    private record SettingTab(Consumer<MetaData> store) implements PropertiesTab {
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
        public void storeSettings(MetaData metaData) {
            store.accept(metaData);
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
                new SettingTab(metaData -> metaData.setMode(BibDatabaseMode.BIBLATEX)),
                new SettingTab(metaData -> metaData.setKeywordSeparator(';')))
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
                new SettingTab(metaData -> metaData.setMode(BibDatabaseMode.BIBLATEX)),
                new SettingTab(metaData -> undoManager.applyEdit(
                        new UndoablePreambleChange(databaseContext.getDatabase(), null, "preamble"))))
                .storeAllSettings();

        assertEquals(Optional.of("preamble"), databaseContext.getDatabase().getPreamble());

        undoManager.undo();

        assertEquals(Optional.empty(), databaseContext.getDatabase().getPreamble());
        assertEquals(Optional.empty(), databaseContext.getMetaData().getMode());
        assertFalse(undoManager.canUndo(), "the whole dialog is one step");
    }

    /// The point of the working copy: the library hears one change, and hears that the journal is
    /// behind it. A dozen setter events, each of which nothing could attribute, is what made the
    /// modified marker stick after undoing a settings change.
    @Test
    // [utest->req~logic.undo.library-settings-recorded~1]
    void theLibraryHearsOneChange_andHearsThatItWasRecorded() {
        List<MetaDataChangedEvent> events = new ArrayList<>();
        databaseContext.getMetaData().registerListener(new Object() {
            @Subscribe
            public void listen(MetaDataChangedEvent event) {
                events.add(event);
            }
        });

        viewModel(
                new SettingTab(metaData -> metaData.setMode(BibDatabaseMode.BIBLATEX)),
                new SettingTab(metaData -> metaData.setKeywordSeparator(';')),
                new SettingTab(metaData -> metaData.setLibrarySpecificFileDirectory("files")))
                .storeAllSettings();

        assertEquals(List.of(MetaDataChangeSource.JOURNAL),
                events.stream().map(MetaDataChangedEvent::getSource).distinct().toList());
    }

    /// Accepting the dialog without touching anything must not put an empty step on the stack:
    /// the next Ctrl+Z would take back the user's previous edit instead.
    @Test
    void acceptingTheDialogUnchangedRecordsNothing() {
        databaseContext.getMetaData().setMode(BibDatabaseMode.BIBTEX);

        viewModel(new SettingTab(metaData -> metaData.setMode(BibDatabaseMode.BIBTEX)))
                .storeAllSettings();

        assertFalse(undoManager.canUndo());
    }

    /// The dialog's step is undoable and redoable like any other.
    @Test
    void theStepCanBeRedone() {
        viewModel(new SettingTab(metaData -> metaData.setMode(BibDatabaseMode.BIBLATEX)))
                .storeAllSettings();

        undoManager.undo();
        undoManager.redo();

        assertEquals(Optional.of(BibDatabaseMode.BIBLATEX), databaseContext.getMetaData().getMode());
        assertTrue(undoManager.canUndo());
    }
}
