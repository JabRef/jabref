package org.jabref.logic.search;

import java.util.List;
import java.util.function.Supplier;

import javafx.beans.property.BooleanProperty;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// **Abstraction** role of the GoF Bridge pattern (cf. _Design Patterns: Elements
/// of Reusable Object-Oriented Software_, Gamma et al., 1994, pp. 151-161),
/// where [SearchBackend] is the Implementor.
///
/// `SearchContext` decouples the GUI's view of search from the concrete backend.
/// Callers always work against this abstraction; the backend reference may be
/// swapped live (e.g. when the user toggles the "use Postgres-backed search"
/// preference). When the backing preference changes, the context closes the
/// previous backend and instantiates a new one via the supplier passed in at
/// construction time. Switching to the SQL backend triggers a full re-index
/// because the [org.jabref.logic.search.sqlbased.IndexManager] constructor
/// indexes all current entries on creation.
///
/// All methods are synchronized so concurrent search calls and a live swap do
/// not interleave. The lock is held for the duration of each forwarded call.
public class SearchContext implements SearchBackend {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchContext.class);

    private final BooleanProperty usePostgresProperty;
    private final Supplier<SearchBackend> sqlFactory;
    private final Supplier<SearchBackend> inMemoryFactory;
    private final Subscription preferenceSubscription;
    private SearchBackend backend;

    public SearchContext(BooleanProperty usePostgresProperty,
                         Supplier<SearchBackend> sqlFactory,
                         Supplier<SearchBackend> inMemoryFactory) {
        this.usePostgresProperty = usePostgresProperty;
        this.sqlFactory = sqlFactory;
        this.inMemoryFactory = inMemoryFactory;
        this.backend = buildBackend();
        this.preferenceSubscription = EasyBind.listen(usePostgresProperty, (_, _, _) -> swapBackend());
    }

    private SearchBackend buildBackend() {
        return usePostgresProperty.get() ? sqlFactory.get() : inMemoryFactory.get();
    }

    private synchronized void swapBackend() {
        LOGGER.info("Swapping search backend (usePostgres={})", usePostgresProperty.get());
        try {
            backend.close();
        } catch (RuntimeException e) {
            LOGGER.warn("Closing previous search backend threw", e);
        }
        backend = buildBackend();
    }

    @Override
    public synchronized SearchResults search(SearchQuery query) {
        return backend.search(query);
    }

    @Override
    public synchronized boolean isEntryMatched(BibEntry entry, SearchQuery query) {
        return backend.isEntryMatched(entry, query);
    }

    @Override
    public synchronized void addToIndex(List<BibEntry> entries) {
        backend.addToIndex(entries);
    }

    @Override
    public synchronized void removeFromIndex(List<BibEntry> entries) {
        backend.removeFromIndex(entries);
    }

    @Override
    public synchronized void updateEntry(FieldChangedEvent event) {
        backend.updateEntry(event);
    }

    @Override
    public synchronized void rebuildFullTextIndex() {
        backend.rebuildFullTextIndex();
    }

    @Override
    public synchronized void close() {
        preferenceSubscription.unsubscribe();
        backend.close();
    }
}
