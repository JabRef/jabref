package org.jabref.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import org.jabref.logic.search.IndexManager;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefCliStateManager implements CliStateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefCliStateManager.class);
    protected final ObservableList<BibDatabaseContext> openDatabases = FXCollections.observableArrayList();
    protected final OptionalObjectProperty<BibDatabaseContext> activeDatabase = OptionalObjectProperty.empty();
    protected final ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList();
    protected final ObservableMap<String, IndexManager> indexManagers = FXCollections.observableHashMap();

    @Override
    public ObservableList<BibDatabaseContext> getOpenDatabases() {
        return openDatabases;
    }

    @Override
    public OptionalObjectProperty<BibDatabaseContext> activeDatabaseProperty() {
        return activeDatabase;
    }

    @Override
    public ObservableList<BibEntry> getSelectedEntries() {
        return selectedEntries;
    }

    @Override
    public Optional<IndexManager> getIndexManager(BibDatabaseContext database) {
        return Optional.ofNullable(indexManagers.get(database.getUid()));
    }

    @Override
    public Optional<BibDatabaseContext> getActiveDatabase() {
        return activeDatabase.get();
    }

    @Override
    public List<String> collectAllDatabasePaths() {
        List<String> list = new ArrayList<>();
        getOpenDatabases().stream()
                          .map(BibDatabaseContext::getDatabasePath)
                          .forEachOrdered(pathOptional -> pathOptional.ifPresentOrElse(
                                  path -> list.add(path.toAbsolutePath().toString()),
                                  () -> list.add("")));
        return list;
    }

    @Override
    public boolean isRunningInCli() {
        return true;
    }
}
