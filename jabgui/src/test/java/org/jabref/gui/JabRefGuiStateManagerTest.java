package org.jabref.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.undo.GuiUndoManager;
import org.jabref.gui.undo.HeadlessGuiUndoManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class JabRefGuiStateManagerTest {

    /// The journal the application shares is the one every library resolves to, so a caller that
    /// names its library records where the rest of the application does.
    @Test
    void everyLibraryResolvesToTheJournalTheStateManagerWasGiven() {
        GuiUndoManager sharedJournal = new HeadlessGuiUndoManager();
        JabRefGuiStateManager stateManager = new JabRefGuiStateManager(sharedJournal);

        assertSame(sharedJournal, stateManager.getUndoManager(new BibDatabaseContext()));
        assertSame(sharedJournal, stateManager.getGuiUndoManager(new BibDatabaseContext()));
    }

    @Test
    void aStateManagerWithoutOneUsesAJournalOfItsOwn() {
        assertNotSame(new JabRefGuiStateManager().getUndoManager(new BibDatabaseContext()),
                new JabRefGuiStateManager().getUndoManager(new BibDatabaseContext()));
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
