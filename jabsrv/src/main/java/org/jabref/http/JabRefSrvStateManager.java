package org.jabref.http;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.command.CommandSelectionTab;
import org.jabref.logic.search.SearchBackend;
import org.jabref.logic.search.SearchContext;
import org.jabref.logic.search.inmemory.InMemorySearchBackend;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;

/// [SrvStateManager] for the stand-alone HTTP server.
///
/// Unlike the GUI, the server serves a fixed set of libraries known at startup and has no
/// preference toggle. It therefore parses every library once (see
/// [org.jabref.http.server.Server]) and builds an in-memory [SearchContext] for each in
/// this constructor; [#getSearchContext] then only ever returns these pre-built contexts
/// (handled by [AbstractSrvStateManager]). There is no Postgres path: the in-memory backend
/// has no startup cost, and the toggle is hardwired to `false`.
public class JabRefSrvStateManager extends AbstractSrvStateManager {

    private final ObservableList<BibDatabaseContext> openDatabases;

    public JabRefSrvStateManager(BibEntryPreferences bibEntryPreferences, List<BibDatabaseContext> databases) {
        this.openDatabases = FXCollections.observableArrayList(databases);
        for (BibDatabaseContext database : databases) {
            Supplier<SearchBackend> inMemory = () -> new InMemorySearchBackend(database, bibEntryPreferences);
            SimpleBooleanProperty usePostgres = new SimpleBooleanProperty(false);
            assert !usePostgres.get() : "Stand-alone HTTP server must never use the Postgres backend";
            // The in-memory supplier is wired into the SQL slot too: should the toggle ever
            // flip, an unexpected backend swap stays harmless instead of throwing at runtime.
            setSearchContext(database, new SearchContext(usePostgres, inMemory, inMemory));
        }
    }

    @Override
    public ObservableList<BibDatabaseContext> getOpenDatabases() {
        return openDatabases;
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
    public Optional<BibDatabaseContext> getActiveDatabase() {
        return Optional.empty();
    }

    @Override
    public List<String> getAllDatabasePaths() {
        return openDatabases.stream()
                            .map(BibDatabaseContext::getDatabasePath)
                            .flatMap(Optional::stream)
                            .map(path -> path.toAbsolutePath().toString())
                            .toList();
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
