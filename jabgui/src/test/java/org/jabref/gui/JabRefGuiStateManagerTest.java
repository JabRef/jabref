package org.jabref.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JabRefGuiStateManagerTest {

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
