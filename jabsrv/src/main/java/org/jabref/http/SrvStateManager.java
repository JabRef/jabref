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

    ObservableList<BibEntry> getSelectedEntries();

    Optional<BibDatabaseContext> getActiveDatabase();

    OptionalObjectProperty<BibDatabaseContext> activeDatabaseProperty();

    ObservableList<BibDatabaseContext> getOpenDatabases();

    ObjectBinding<Optional<CommandSelectionTab>> getActiveSelectionTabProperty();

    List<String> getAllDatabasePaths();

    Optional<IndexManager> getIndexManager(BibDatabaseContext database);
}
