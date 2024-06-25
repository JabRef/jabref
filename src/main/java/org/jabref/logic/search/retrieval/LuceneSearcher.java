package org.jabref.logic.search.retrieval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
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
            LOGGER.debug("Searching for entries matching query: {}", searchQuery.getQuery());
            topDocs = indexSearcher.search(searchQuery.getQuery(), Integer.MAX_VALUE);
            storedFields = indexSearcher.storedFields();
            LOGGER.debug("Found {} matches", topDocs.totalHits.value);
            return getSearchResults();
        } catch (IOException | IndexSearcher.TooManyClauses e) {
            LOGGER.error("Error while searching", e);
        }
        return new SearchResults();
    }

    private SearchResults getSearchResults() {
        SearchResults searchResults = new SearchResults();
        Map<String, BibEntry> entriesMap = new HashMap<>();
        Map<String, List<BibEntry>> linkedFielsMap = new HashMap<>();

        for (BibEntry bibEntry : databaseContext.getEntries()) {
            entriesMap.put(bibEntry.getId(), bibEntry);
            for (LinkedFile linkedFile : bibEntry.getFiles()) {
                linkedFielsMap.computeIfAbsent(linkedFile.getLink(), k -> new ArrayList<>()).add(bibEntry);
            }
        }

        Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter("<b>", "</b>"), new QueryScorer(searchQuery.getQuery()));
        Analyzer analyzer = SearchFieldConstants.ANALYZER;

        long startTime = System.currentTimeMillis();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            float luceneScore = scoreDoc.score;
            String fileLink = getFieldContents(storedFields, scoreDoc, SearchFieldConstants.PATH);
            if (!fileLink.isEmpty()) {
                List<BibEntry> entriesWithFile = linkedFielsMap.get(fileLink);
                if (entriesWithFile.isEmpty()) {
                    continue;
                }
                int pageNumber = Integer.parseInt(getFieldContents(storedFields, scoreDoc, SearchFieldConstants.PAGE_NUMBER));
                long modified = Long.parseLong(getFieldContents(storedFields, scoreDoc, SearchFieldConstants.MODIFIED));
                String content = getFieldContents(storedFields, scoreDoc, SearchFieldConstants.CONTENT);
                String annotations = getFieldContents(storedFields, scoreDoc, SearchFieldConstants.ANNOTATIONS);

                searchResults.addSearchResult(entriesWithFile,
                        new SearchResult(luceneScore, fileLink, pageNumber, modified,
                                getHighlighterFragments(highlighter, analyzer, SearchFieldConstants.CONTENT.toString(), content),
                                getHighlighterFragments(highlighter, analyzer, SearchFieldConstants.ANNOTATIONS.toString(), annotations)));
            } else {
                String entryId = getFieldContents(storedFields, scoreDoc, SearchFieldConstants.ENTRY_ID);
                String defaultField = getFieldContents(storedFields, scoreDoc, SearchFieldConstants.DEFAULT_FIELD);
                searchResults.addSearchResult(entriesMap.get(entryId),
                        new SearchResult(luceneScore, getHighlighterFragments(highlighter, analyzer, SearchFieldConstants.DEFAULT_FIELD.toString(), defaultField)));
            }
        }
        LOGGER.debug("Mapping search results took {} ms", System.currentTimeMillis() - startTime);
        return searchResults;
    }

    private static List<String> getHighlighterFragments(Highlighter highlighter, Analyzer analyzer, String field, String content) {
        try (TokenStream contentStream = analyzer.tokenStream(field, content)) {
            TextFragment[] frags = highlighter.getBestTextFragments(contentStream, content, true, 10);
            return Arrays.stream(frags).map(TextFragment::toString).toList();
        } catch (IOException | InvalidTokenOffsetsException e) {
            return List.of();
        }
    }

    private static String getFieldContents(StoredFields storedFields, ScoreDoc scoreDoc, SearchFieldConstants field) {
        try {
            return Optional.ofNullable(storedFields.document(scoreDoc.doc).get(field.toString())).orElse("");
        } catch (IOException e) {
            LOGGER.error("Error while getting field contents for field {}", field, e);
        }
        return "";
    }
}
