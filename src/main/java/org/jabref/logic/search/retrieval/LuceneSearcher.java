package org.jabref.logic.search.retrieval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.search.SearchFieldConstants;
import org.jabref.model.search.SearchQuery;
import org.jabref.model.search.SearchResult;
import org.jabref.model.search.SearchResults;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LuceneSearcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneSearcher.class);
    private final BibDatabaseContext databaseContext;
    private SearchQuery searchQuery;
    private TopDocs topDocs;
    private StoredFields storedFields;

    public LuceneSearcher(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
    }

    public boolean isMatched(BibEntry entry, SearchQuery searchQuery, IndexSearcher indexSearcher) {
        String query = "+" + SearchFieldConstants.ENTRY_ID + " +" + searchQuery.getSearchExpression();
        SearchQuery newSearchQuery = new SearchQuery(query, searchQuery.getSearchFlags());
        SearchResults searchResults = search(searchQuery, indexSearcher);
        return searchResults.getSearchScoreForEntry(entry) > 0;
    }

    /**
     * Search for results matching a query in the Lucene search index
     *
     * @param searchQuery query to search for
     * @param indexSearcher the index searcher to use
     * @return a result map of all entries that have matches in any fields
     */
    public SearchResults search(SearchQuery searchQuery, IndexSearcher indexSearcher) {
        this.searchQuery = Objects.requireNonNull(searchQuery);
        try {
            LOGGER.debug("Searching for entries matching query: {}", searchQuery.getParsedQuery());
            topDocs = indexSearcher.search(searchQuery.getParsedQuery(), Integer.MAX_VALUE);
            storedFields = indexSearcher.storedFields();
            LOGGER.debug("Found {} matches", topDocs.totalHits.value);
            return getSearchResults();
        } catch (IOException | IndexSearcher.TooManyClauses e) {
            LOGGER.error("Error while searching", e);
        }
        return new SearchResults();
    }

    private SearchResults getSearchResults() throws IOException {
        SearchResults searchResults = new SearchResults();
        Map<String, BibEntry> entriesMap = new HashMap<>();
        Map<String, List<BibEntry>> linkedFilesMap = new HashMap<>();

        for (BibEntry bibEntry : databaseContext.getEntries()) {
            entriesMap.put(bibEntry.getId(), bibEntry);
            for (LinkedFile linkedFile : bibEntry.getFiles()) {
                linkedFilesMap.computeIfAbsent(linkedFile.getLink(), k -> new ArrayList<>()).add(bibEntry);
            }
        }

        long startTime = System.currentTimeMillis();
        Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter("<b>", "</b>"), new QueryScorer(searchQuery.getParsedQuery()));
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            float score = scoreDoc.score;
            Document document = storedFields.document(scoreDoc.doc);

            String fileLink = getFieldContents(document, SearchFieldConstants.PATH);
            if (!fileLink.isEmpty()) {
                List<BibEntry> entriesWithFile = linkedFilesMap.get(fileLink);
                if (!entriesWithFile.isEmpty()) {
                    SearchResult searchResult = new SearchResult(score, fileLink,
                            getFieldContents(document, SearchFieldConstants.CONTENT),
                            getFieldContents(document, SearchFieldConstants.ANNOTATIONS),
                            Integer.parseInt(getFieldContents(document, SearchFieldConstants.PAGE_NUMBER)),
                            highlighter);
                    searchResults.addSearchResult(entriesWithFile, searchResult);
                }
            } else {
                String entryId = getFieldContents(document, SearchFieldConstants.ENTRY_ID);
                searchResults.addSearchResult(entriesMap.get(entryId), new SearchResult(score, highlighter));
            }
        }
        LOGGER.debug("Mapping search results took {} ms", System.currentTimeMillis() - startTime);
        return searchResults;
    }

    private static String getFieldContents(Document document, SearchFieldConstants field) {
        return Optional.ofNullable(document.get(field.toString())).orElse("");
    }
}
