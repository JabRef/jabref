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
    ///
    /// Both backend slots use the in-memory supplier: the toggle is hardwired to
    /// `false`, but wiring in-memory into the SQL slot too keeps an unexpected
    /// backend swap harmless instead of throwing at runtime.
    @Override
    public SearchContext getSearchContext(BibDatabaseContext database) {
        Supplier<SearchBackend> inMemory = () -> new InMemorySearchBackend(database, preferences.getBibEntryPreferences());
        SimpleBooleanProperty usePostgres = new SimpleBooleanProperty(false);
        assert !usePostgres.get() : "Stand-alone HTTP server must never use the Postgres backend";
        return new SearchContext(usePostgres, inMemory, inMemory);
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
