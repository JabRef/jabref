package org.jabref.http;

import java.util.List;
import java.util.Optional;

import javafx.beans.binding.ObjectBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.command.CommandSelectionTab;
import org.jabref.logic.search.IndexManager;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class JabRefSrvStateManager implements SrvStateManager {

    @Override
    public ObservableList<BibDatabaseContext> getOpenDatabases() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public OptionalObjectProperty<BibDatabaseContext> activeDatabaseProperty() {
        return OptionalObjectProperty.empty();
    }

    @Override
    public ObservableList<BibEntry> getSelectedEntries() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public Optional<IndexManager> getIndexManager(BibDatabaseContext database) {
        return Optional.empty();
    }

    @Override
    public Optional<BibDatabaseContext> getActiveDatabase() {
        return Optional.empty();
    }

    @Override
    public List<String> getAllDatabasePaths() {
        return List.of();
    }

    @Override
    public ObjectBinding<Optional<CommandSelectionTab>> getActiveSelectionTabProperty() {
        return new ObjectBinding<>() {
            @Override
            protected Optional<CommandSelectionTab> computeValue() {
                return Optional.empty();
            }
        };
    }
}
