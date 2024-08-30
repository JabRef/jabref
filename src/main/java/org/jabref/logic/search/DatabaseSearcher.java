package org.jabref.logic.search;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.search.indexing.BibFieldsIndexer;
import org.jabref.logic.search.indexing.DefaultLinkedFilesIndexer;
import org.jabref.logic.search.retrieval.LuceneSearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabases;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.LuceneIndexer;
import org.jabref.model.search.SearchQuery;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseSearcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSearcher.class);
    private static final BackgroundTask<Void> DUMMY_TASK = BackgroundTask.wrap(() -> null);

    private final SearchQuery query;
    private final LuceneIndexer bibFieldsIndexer;
    private final LuceneIndexer linkedFilesIndexer;
    private final LuceneSearcher luceneSearcher;

    public DatabaseSearcher(SearchQuery query, BibDatabaseContext databaseContext, FilePreferences filePreferences) throws IOException {
        this.query = Objects.requireNonNull(query);
        bibFieldsIndexer = new BibFieldsIndexer(databaseContext);
        bibFieldsIndexer.updateOnStart(DUMMY_TASK);

        linkedFilesIndexer = new DefaultLinkedFilesIndexer(databaseContext, filePreferences);
        linkedFilesIndexer.updateOnStart(DUMMY_TASK);

        this.luceneSearcher = new LuceneSearcher(databaseContext, bibFieldsIndexer, linkedFilesIndexer);
    }

    /**
     * @return The matches in the order they appear in the library.
     */
    public List<BibEntry> getMatches() {
        LOGGER.debug("Search term: {}", query);

        if (!query.isValid()) {
            LOGGER.warn("Search failed: invalid search expression");
            return Collections.emptyList();
        }
        List<BibEntry> matchEntries = luceneSearcher.search(query.getParsedQuery(), query.getSearchFlags()).getMatchedEntries().stream().toList();
        bibFieldsIndexer.close();
        linkedFilesIndexer.close();
        return BibDatabases.purgeEmptyEntries(matchEntries);
    }
}
