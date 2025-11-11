package org.jabref.http;

import java.util.List;
import java.util.Optional;

import javafx.beans.binding.ObjectBinding;
import javafx.collections.ObservableList;

import org.jabref.logic.command.CommandSelectionTab;
import org.jabref.logic.search.IndexManager;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public interface SrvStateManager {

    ObservableList<BibDatabaseContext> getOpenDatabases();

    OptionalObjectProperty<BibDatabaseContext> activeDatabaseProperty();

    ObservableList<BibEntry> getSelectedEntries();

    Optional<IndexManager> getIndexManager(BibDatabaseContext database);

    Optional<BibDatabaseContext> getActiveDatabase();

    List<String> getAllDatabasePaths();

    ObjectBinding<Optional<CommandSelectionTab>> getActiveSelectionTabProperty();
}
