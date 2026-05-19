package org.jabref.logic.search.sqlbased;

import java.io.IOException;
import java.util.List;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.search.LibrarySearcher;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabases;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// SQL/Lucene-backed implementation of [LibrarySearcher].
/// Boots an [IndexManager] (embedded Postgres + Lucene linked-files index) per instance.
public class SqlBasedLibrarySearcher implements LibrarySearcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlBasedLibrarySearcher.class);

    private final BibDatabaseContext databaseContext;
    private final IndexManager indexManager;

    public SqlBasedLibrarySearcher(BibDatabaseContext databaseContext,
                                   TaskExecutor taskExecutor,
                                   CliPreferences preferences,
                                   PostgreServer postgreServer) throws IOException {
        this.databaseContext = databaseContext;
        this.indexManager = new IndexManager(databaseContext, taskExecutor, preferences, postgreServer);
    }

    @Override
    public List<BibEntry> getMatches(SearchQuery query) {
        LOGGER.debug("Search term: {}", query);

        if (!query.isValid()) {
            LOGGER.warn("Search failed: invalid search expression");
            indexManager.closeAndWait();
            return List.of();
        }
        List<BibEntry> matchEntries = indexManager.search(query)
                                                  .getMatchedEntries()
                                                  .stream()
                                                  .map(entryId -> databaseContext.getDatabase().getEntryById(entryId))
                                                  .toList();
        indexManager.closeAndWait();
        return BibDatabases.purgeEmptyEntries(matchEntries);
    }
}
