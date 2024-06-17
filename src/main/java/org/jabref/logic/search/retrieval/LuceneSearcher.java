package org.jabref.logic.search.retrieval;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.LuceneSearchResults;
import org.jabref.model.search.SearchResult;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LuceneSearcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneSearcher.class);
    private final BibDatabaseContext databaseContext;

    public LuceneSearcher(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
    }

    /**
     * Search for results matching a query in the Lucene search index
     *
     * @param query query to search for
     * @return a result map of all entries that have matches in any fields
     */
    public HashMap<BibEntry, LuceneSearchResults> search(SearchQuery query, IndexSearcher searcher) {
        Objects.requireNonNull(query);

        HashMap<BibEntry, LuceneSearchResults> results = new HashMap<>();
        try {
            TopDocs docs = searcher.search(query.getQuery(), Integer.MAX_VALUE);
            for (ScoreDoc scoreDoc : docs.scoreDocs) {
                SearchResult searchResult = new SearchResult(searcher, query.getQuery(), scoreDoc);
                for (BibEntry match : searchResult.getMatchingEntries(databaseContext)) {
                    if (searchResult.getLuceneScore() > 0) {
                        if (!results.containsKey(match)) {
                            results.put(match, new LuceneSearchResults());
                        }
                        results.get(match).addResult(searchResult);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error while searching", e);
        }
        return results;
    }
}
