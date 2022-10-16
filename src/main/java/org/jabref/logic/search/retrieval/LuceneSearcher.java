package org.jabref.logic.search.retrieval;

import java.io.IOException;
import java.util.HashMap;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.search.LuceneSearchResults;
import org.jabref.model.pdf.search.SearchResult;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LuceneSearcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);

    private final BibDatabaseContext databaseContext;
    private final Directory indexDirectory;

    private LuceneSearcher(BibDatabaseContext databaseContext, Directory indexDirectory) {
        this.databaseContext = databaseContext;
        this.indexDirectory = indexDirectory;
    }

    public static LuceneSearcher of(BibDatabaseContext databaseContext) throws IOException {
        return new LuceneSearcher(databaseContext, new NIOFSDirectory(databaseContext.getFulltextIndexPath()));
    }

    /**
     * Search for results matching a query in the Lucene search index
     *
     * @param query query to search for
     * @return a result map of all entries that have matches in any fields
     */
    public HashMap<BibEntry, LuceneSearchResults> search(SearchQuery query) {
        HashMap<BibEntry, LuceneSearchResults> results = new HashMap<>();
        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
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
            LOGGER.warn("Could not open Index at: '{}'!\n{}", indexDirectory, e.getMessage());
        }
        return results;
    }
}
