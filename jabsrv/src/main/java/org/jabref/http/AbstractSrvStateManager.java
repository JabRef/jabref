package org.jabref.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.search.NoOpSearchBackend;
import org.jabref.logic.search.SearchContext;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Base [SrvStateManager] implementation that owns the per-database [SearchContext] registry.
///
/// Both the GUI ([org.jabref.gui.JabRefGuiStateManager]) and the stand-alone HTTP server
/// ([JabRefSrvStateManager]) keep one [SearchContext] per open library, keyed by
/// [BibDatabaseContext#getUid()]. They only differ in *when* a context is registered:
/// the GUI registers one lazily when a library tab opens, the server registers all of
/// them up front in its constructor. The lookup itself and the "must be registered
/// before the database is served" invariant are identical, so they live here.
public abstract class AbstractSrvStateManager implements SrvStateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSrvStateManager.class);

    private final Map<String, SearchContext> searchContexts = new ConcurrentHashMap<>();

    /// Registers the [SearchContext] for the given database. Must be called before the
    /// database is exposed via [#getOpenDatabases()].
    public void setSearchContext(BibDatabaseContext database, SearchContext searchContext) {
        searchContexts.put(database.getUid(), searchContext);
    }

    @Override
    public SearchContext getSearchContext(BibDatabaseContext database) {
        SearchContext context = searchContexts.get(database.getUid());
        if (context != null) {
            return context;
        }
        assert false : "No SearchContext registered for database '" + database.getUid() + "'";
        LOGGER.error("No SearchContext registered for database '{}'. Returning inert fallback. A context must be registered via setSearchContext before the database is served.", database.getUid());
        return new SearchContext(
                new SimpleBooleanProperty(false),
                NoOpSearchBackend::new,
                NoOpSearchBackend::new);
    }
}
