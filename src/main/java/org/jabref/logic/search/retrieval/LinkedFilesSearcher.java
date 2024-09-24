package org.jabref.logic.search.retrieval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.search.LuceneIndexer;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.search.SearchFieldConstants;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchResult;
import org.jabref.model.search.query.SearchResults;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LinkedFilesSearcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFilesSearcher.class);

    private final FilePreferences filePreferences;
    private final BibDatabaseContext databaseContext;
    private final SearcherManager searcherManager;

    public LinkedFilesSearcher(BibDatabaseContext databaseContext, LuceneIndexer linkedFilesIndexer, FilePreferences filePreferences) {
        this.searcherManager = linkedFilesIndexer.getSearcherManager();
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
    }

    public SearchResults search(Query searchQuery, EnumSet<SearchFlags> searchFlags) {
        boolean shouldSearchInLinkedFiles = searchFlags.contains(SearchFlags.FULLTEXT) && filePreferences.shouldFulltextIndexLinkedFiles();
        if (!shouldSearchInLinkedFiles) {
            return new SearchResults();
        }

        LOGGER.debug("Searching in linked files with query: {}", searchQuery);
        try {
            IndexSearcher linkedFilesIndexSearcher = acquireIndexSearcher(searcherManager);
            SearchResults searchResults = search(linkedFilesIndexSearcher, searchQuery);
            releaseIndexSearcher(searcherManager, linkedFilesIndexSearcher);
            return searchResults;
        } catch (IOException | IndexSearcher.TooManyClauses e) {
            LOGGER.error("Error during linked files search execution", e);
        }
        return new SearchResults();
    }

    private SearchResults search(IndexSearcher indexSearcher, Query searchQuery) throws IOException {
        TopDocs topDocs = indexSearcher.search(searchQuery, Integer.MAX_VALUE);
        StoredFields storedFields = indexSearcher.storedFields();
        LOGGER.debug("Found {} matching documents", topDocs.totalHits.value);
        return getSearchResults(topDocs, storedFields, searchQuery);
    }

    private SearchResults getSearchResults(TopDocs topDocs, StoredFields storedFields, Query searchQuery) throws IOException {
        SearchResults searchResults = new SearchResults();
        long startTime = System.currentTimeMillis();

        Map<String, List<String>> linkedFilesMap = getLinkedFilesMap();
        Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter("<b>", "</b>"), new QueryScorer(searchQuery));

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document document = storedFields.document(scoreDoc.doc);
            String fileLink = getFieldContents(document, SearchFieldConstants.PATH);

            if (!fileLink.isEmpty()) {
                List<String> entriesWithFile = linkedFilesMap.get(fileLink);
                if (entriesWithFile != null && !entriesWithFile.isEmpty()) {
                    SearchResult searchResult = new SearchResult(
                            scoreDoc.score,
                            fileLink,
                            getFieldContents(document, SearchFieldConstants.CONTENT),
                            getFieldContents(document, SearchFieldConstants.ANNOTATIONS),
                            Integer.parseInt(getFieldContents(document, SearchFieldConstants.PAGE_NUMBER)),
                            highlighter);
                    searchResults.addSearchResult(entriesWithFile, searchResult);
                }
            }
        }
        LOGGER.debug("Getting linked files results took {} ms", System.currentTimeMillis() - startTime);
        return searchResults;
    }

    private Map<String, List<String>> getLinkedFilesMap() {
        // fileLink to List of entry IDs
        Map<String, List<String>> linkedFilesMap = new HashMap<>();
        for (BibEntry bibEntry : databaseContext.getEntries()) {
            for (LinkedFile linkedFile : bibEntry.getFiles()) {
                linkedFilesMap.computeIfAbsent(linkedFile.getLink(), k -> new ArrayList<>()).add(bibEntry.getId());
            }
        }
        return linkedFilesMap;
    }

    private static String getFieldContents(Document document, SearchFieldConstants field) {
        return Optional.ofNullable(document.get(field.toString())).orElse("");
    }

    private static IndexSearcher acquireIndexSearcher(SearcherManager searcherManager) throws IOException {
        searcherManager.maybeRefreshBlocking();
        return searcherManager.acquire();
    }

    private static void releaseIndexSearcher(SearcherManager searcherManager, IndexSearcher indexSearcher) throws IOException {
        searcherManager.release(indexSearcher);
    }
}
