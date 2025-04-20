package org.jabref.logic.search;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabases;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseSearcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSearcher.class);

    private final BibDatabaseContext databaseContext;
    private final SearchQuery query;
    private final IndexManager indexManager;

    // get rid of task executor here or add a constructor overload?
    public DatabaseSearcher(SearchQuery query, BibDatabaseContext databaseContext, TaskExecutor taskExecutor, CliPreferences preferences) throws IOException {
        this.databaseContext = databaseContext;
        this.query = Objects.requireNonNull(query);
        this.indexManager = new IndexManager(databaseContext, taskExecutor, preferences);
    }

    /**
     * @return The matches in the order they appear in the library.
     */
    public List<BibEntry> getMatches() {
        LOGGER.debug("Search term: {}", query);

        if (!query.isValid()) {
            LOGGER.warn("Search failed: invalid search expression");
            indexManager.closeAndWait();
            return Collections.emptyList();
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
