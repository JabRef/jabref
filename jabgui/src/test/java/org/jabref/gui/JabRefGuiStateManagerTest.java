package org.jabref.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.undo.UndoManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class JabRefGuiStateManagerTest {

    /// Each library gets a journal of its own, so that undo does not cross libraries and saving
    /// one does not stamp another's saved position.
    @Test
    void eachLibraryGetsItsOwnJournal() {
        // [utest->req~logic.undo.journal-per-library~1]
        JabRefGuiStateManager stateManager = new JabRefGuiStateManager();
        BibDatabaseContext one = new BibDatabaseContext();
        BibDatabaseContext another = new BibDatabaseContext();

        assertNotSame(stateManager.getUndoManager(one), stateManager.getUndoManager(another));
    }

    /// The same library has to resolve to the same journal every time, including after an entry is
    /// added: a context's hashCode changes when its database does, which is why the journals are
    /// keyed by its uid.
    @Test
    void aLibraryKeepsItsJournalAfterItsContentChanges() {
        JabRefGuiStateManager stateManager = new JabRefGuiStateManager();
        BibDatabaseContext context = new BibDatabaseContext();
        UndoManager journal = stateManager.getUndoManager(context);

        context.getDatabase().insertEntry(new BibEntry());

        assertSame(journal, stateManager.getUndoManager(context));
    }

    /// Closing a library discards its history, and the entries the recorded changes keep alive.
    @Test
    void closingALibraryDiscardsItsJournal() {
        // [utest->req~logic.undo.journal-per-library~1]
        JabRefGuiStateManager stateManager = new JabRefGuiStateManager();
        BibDatabaseContext context = new BibDatabaseContext();
        UndoManager journal = stateManager.getUndoManager(context);

        stateManager.removeUndoManager(context);

        assertNotSame(journal, stateManager.getUndoManager(context));
    }

    @Test
    void replacingActiveDatabaseContextNotifiesListeners() {
        JabRefGuiStateManager stateManager = new JabRefGuiStateManager();
        BibDatabaseContext loadingContext = new BibDatabaseContext();
        BibDatabaseContext loadedContext = new BibDatabaseContext();

        assertEquals(loadingContext, loadedContext);

        stateManager.setActiveDatabase(loadingContext);
        List<Optional<BibDatabaseContext>> changes = new ArrayList<>();
        stateManager.activeDatabaseProperty().addListener((_, _, newValue) -> changes.add(newValue));

        stateManager.replaceActiveDatabase(loadedContext);

        assertEquals(List.of(Optional.empty(), Optional.of(loadedContext)), changes);
    }

    @Test
    void switchingBetweenDifferentActiveDatabaseContextsNotifiesListenersOnce() {
        JabRefGuiStateManager stateManager = new JabRefGuiStateManager();
        BibDatabaseContext firstContext = new BibDatabaseContext();
        BibDatabaseContext secondContext = new BibDatabaseContext();
        firstContext.getDatabase().insertEntry(new BibEntry());

        stateManager.setActiveDatabase(firstContext);
        List<Optional<BibDatabaseContext>> changes = new ArrayList<>();
        stateManager.activeDatabaseProperty().addListener((_, _, newValue) -> changes.add(newValue));

        stateManager.setActiveDatabase(secondContext);

        assertEquals(List.of(Optional.of(secondContext)), changes);
    }
}
