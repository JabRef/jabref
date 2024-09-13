package org.jabref.logic.search;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabases;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseSearcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSearcher.class);

    private final BibDatabaseContext databaseContext;
    private final SearchQuery query;
    private final LuceneManager luceneManager;

    // get rid of task executor here or add a constuctor overload?
    public DatabaseSearcher(SearchQuery query, BibDatabaseContext databaseContext, TaskExecutor taskExecutor, FilePreferences filePreferences) throws IOException {
        this.databaseContext = databaseContext;
        this.query = Objects.requireNonNull(query);
        this.luceneManager = new LuceneManager(databaseContext, taskExecutor, filePreferences);
    }

    /**
     * @return The matches in the order they appear in the library.
     */
    public List<BibEntry> getMatches() {
        LOGGER.debug("Search term: {}", query);

        if (!query.isValid()) {
            LOGGER.warn("Search failed: invalid search expression");
            luceneManager.closeAndWait();
            return Collections.emptyList();
        }
        List<BibEntry> matchEntries = luceneManager.search(query)
                                                   .getMatchedEntries()
                                                   .stream()
                                                   .map(entryId -> databaseContext.getDatabase().getEntryById(entryId))
                                                   .toList();
        luceneManager.closeAndWait();
        return BibDatabases.purgeEmptyEntries(matchEntries);
    }
}
