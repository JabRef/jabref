package org.jabref.http;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.command.CommandSelectionTab;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.search.SearchBackend;
import org.jabref.logic.search.SearchContext;
import org.jabref.logic.search.inmemory.InMemorySearchBackend;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class JabRefSrvStateManager implements SrvStateManager {

    private final CliPreferences preferences;

    public JabRefSrvStateManager(CliPreferences preferences) {
        this.preferences = preferences;
    }

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

    /// Stand-alone server: always returns an in-memory [SearchContext]. No Postgres
    /// path because the CLI server has no preference toggle. Each call builds a
    /// fresh context; the in-memory backend has no startup cost.
    @Override
    public Optional<SearchContext> getSearchContext(BibDatabaseContext database) {
        Supplier<SearchBackend> inMemory = () -> new InMemorySearchBackend(database, preferences.getBibEntryPreferences());
        Supplier<SearchBackend> sqlNotAvailable = () -> {
            throw new UnsupportedOperationException("Postgres backend is not available in the stand-alone HTTP server");
        };
        return Optional.of(new SearchContext(new SimpleBooleanProperty(false), sqlNotAvailable, inMemory));
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
