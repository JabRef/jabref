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
import org.jabref.model.search.SearchQuery;
import org.jabref.model.search.SearchResult;
import org.jabref.model.search.SearchResults;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LuceneSearcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneSearcher.class);

    private final FilePreferences filePreferences;
    private final BibDatabaseContext databaseContext;
    private final SearcherManager bibFieldsSearcherManager;
    private final SearcherManager linkedFilesSearcherManager;

    public LuceneSearcher(BibDatabaseContext databaseContext, LuceneIndexer bibFieldsIndexer, LuceneIndexer linkedFilesIndexer, FilePreferences filePreferences) {
        this.bibFieldsSearcherManager = bibFieldsIndexer.getSearcherManager();
        this.linkedFilesSearcherManager = linkedFilesIndexer.getSearcherManager();
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
    }

    public boolean isEntryMatched(BibEntry entry, SearchQuery searchQuery) {
        BooleanQuery booleanQuery = buildBooleanQueryForEntry(entry, searchQuery);
        SearchResults results = search(booleanQuery, searchQuery.getSearchFlags());
        return results.getSearchScoreForEntry(entry) > 0;
    }

    private BooleanQuery buildBooleanQueryForEntry(BibEntry entry, SearchQuery searchQuery) {
        Query parsedQuery = searchQuery.getParsedQuery();
        TermQuery entryIdQuery = new TermQuery(new Term(SearchFieldConstants.ENTRY_ID.toString(), entry.getId()));
        return new BooleanQuery.Builder()
                .add(parsedQuery, BooleanClause.Occur.MUST)
                .add(entryIdQuery, BooleanClause.Occur.MUST)
                .build();
    }

    public SearchResults search(Query searchQuery, EnumSet<SearchFlags> searchFlags) {
        LOGGER.debug("Executing search with query: {}", searchQuery);
        try {
            boolean shouldSearchInLinkedFiles = searchFlags.contains(SearchFlags.FULLTEXT) && filePreferences.shouldFulltextIndexLinkedFiles();
            return performSearch(searchQuery, shouldSearchInLinkedFiles);
        } catch (IOException | IndexSearcher.TooManyClauses e) {
            LOGGER.error("Error during search execution", e);
        }
        return new SearchResults();
    }

    private SearchResults performSearch(Query searchQuery, boolean shouldSearchInLinkedFiles) throws IOException {
        if (shouldSearchInLinkedFiles) {
            return searchInBibFieldsAndLinkedFiles(searchQuery);
        } else {
            return searchInBibFields(searchQuery);
        }
    }

    private SearchResults searchInBibFieldsAndLinkedFiles(Query searchQuery) throws IOException {
        IndexSearcher bibFieldsIndexSearcher = acquireIndexSearcher(bibFieldsSearcherManager);
        IndexSearcher linkedFilesIndexSearcher = acquireIndexSearcher(linkedFilesSearcherManager);
        try {
            MultiReader multiReader = new MultiReader(bibFieldsIndexSearcher.getIndexReader(), linkedFilesIndexSearcher.getIndexReader());
            IndexSearcher indexSearcher = new IndexSearcher(multiReader);
            return search(indexSearcher, searchQuery, true);
        } finally {
            releaseIndexSearcher(bibFieldsSearcherManager, bibFieldsIndexSearcher);
            releaseIndexSearcher(linkedFilesSearcherManager, linkedFilesIndexSearcher);
        }
    }

    private SearchResults searchInBibFields(Query searchQuery) throws IOException {
        IndexSearcher indexSearcher = acquireIndexSearcher(bibFieldsSearcherManager);
        try {
            return search(indexSearcher, searchQuery, false);
        } finally {
            releaseIndexSearcher(bibFieldsSearcherManager, indexSearcher);
        }
    }

    private SearchResults search(IndexSearcher indexSearcher, Query searchQuery, boolean shouldSearchInLinkedFiles) throws IOException {
        TopDocs topDocs = indexSearcher.search(searchQuery, Integer.MAX_VALUE);
        StoredFields storedFields = indexSearcher.storedFields();
        LOGGER.debug("Found {} matching documents", topDocs.totalHits.value);
        return getSearchResults(topDocs, storedFields, searchQuery, shouldSearchInLinkedFiles);
    }

    private SearchResults getSearchResults(TopDocs topDocs, StoredFields storedFields, Query searchQuery, boolean shouldSearchInLinkedFiles) throws IOException {
        SearchResults searchResults = new SearchResults();
        long startTime = System.currentTimeMillis();

        if (shouldSearchInLinkedFiles) {
            getBibFieldsAndLinkedFilesResults(topDocs, storedFields, searchQuery, searchResults);
        } else {
            getBibFieldsResults(topDocs, storedFields, searchResults);
        }

        LOGGER.debug("Getting search results took {} ms", System.currentTimeMillis() - startTime);
        return searchResults;
    }

    private void getBibFieldsAndLinkedFilesResults(TopDocs topDocs, StoredFields storedFields, Query searchQuery, SearchResults searchResults) throws IOException {
        Map<String, List<String>> linkedFilesMap = getLinkedFilesMap();
        Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter("<b>", "</b>"), new QueryScorer(searchQuery));

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document document = storedFields.document(scoreDoc.doc);
            String fileLink = getFieldContents(document, SearchFieldConstants.PATH);

            if (!fileLink.isEmpty()) {
                addLinkedFileToResults(document, fileLink, linkedFilesMap, highlighter, searchResults, scoreDoc.score);
            } else {
                addBibEntryToResults(document, searchResults, scoreDoc.score);
            }
        }
    }

    private void getBibFieldsResults(TopDocs topDocs, StoredFields storedFields, SearchResults searchResults) throws IOException {
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document document = storedFields.document(scoreDoc.doc);
            addBibEntryToResults(document, searchResults, scoreDoc.score);
        }
    }

    private void addLinkedFileToResults(Document document, String fileLink, Map<String, List<String>> linkedFilesMap, Highlighter highlighter, SearchResults searchResults, float score) {
        List<String> entriesWithFile = linkedFilesMap.get(fileLink);
        if (entriesWithFile != null && !entriesWithFile.isEmpty()) {
            SearchResult searchResult = new SearchResult(score, fileLink,
                    getFieldContents(document, SearchFieldConstants.CONTENT),
                    getFieldContents(document, SearchFieldConstants.ANNOTATIONS),
                    Integer.parseInt(getFieldContents(document, SearchFieldConstants.PAGE_NUMBER)),
                    highlighter);
            searchResults.addSearchResult(entriesWithFile, searchResult);
        }
    }

    private void addBibEntryToResults(Document document, SearchResults searchResults, float score) {
        String entryId = getFieldContents(document, SearchFieldConstants.ENTRY_ID);
        searchResults.addSearchResult(entryId, new SearchResult(score));
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
