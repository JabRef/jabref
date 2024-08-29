package org.jabref.logic.search.retrieval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.search.LuceneIndexer;
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

    private final BibDatabaseContext databaseContext;
    private final SearcherManager bibFieldsSearcherManager;
    private final SearcherManager linkedFilesSearcherManager;

    public LuceneSearcher(BibDatabaseContext databaseContext, LuceneIndexer bibFieldsIndexer, LuceneIndexer linkedFilesIndexer) {
        this.bibFieldsSearcherManager = bibFieldsIndexer.getSearcherManager();
        this.linkedFilesSearcherManager = linkedFilesIndexer.getSearcherManager();
        this.databaseContext = databaseContext;
    }

    public boolean isMatched(BibEntry entry, SearchQuery searchQuery) {
        BooleanQuery booleanQuery = createBooleanQuery(entry, searchQuery);
        SearchResults results = search(booleanQuery, searchQuery.getSearchFlags());
        return results.getSearchScoreForEntry(entry) > 0;
    }

    private BooleanQuery createBooleanQuery(BibEntry entry, SearchQuery searchQuery) {
        Query query = searchQuery.getParsedQuery();
        TermQuery termQuery = new TermQuery(new Term(SearchFieldConstants.ENTRY_ID.toString(), entry.getId()));
        return new BooleanQuery.Builder()
                .add(query, BooleanClause.Occur.MUST)
                .add(termQuery, BooleanClause.Occur.MUST)
                .build();
    }

    public SearchResults search(Query searchQuery, EnumSet<SearchFlags> searchFlags) {
        LOGGER.debug("Searching for entries matching query: {}", searchQuery);
        try {
            if (searchFlags.contains(SearchFlags.FULLTEXT)) {
                IndexSearcher bibFieldsIndexSearcher = getIndexedSearcher(bibFieldsSearcherManager);
                IndexSearcher linkedFilesIndexSearcher = getIndexedSearcher(linkedFilesSearcherManager);
                MultiReader multiReader = new MultiReader(bibFieldsIndexSearcher.getIndexReader(), linkedFilesIndexSearcher.getIndexReader());
                IndexSearcher indexSearcher = new IndexSearcher(multiReader);

                SearchResults searchResults = search(indexSearcher, searchQuery);
                releaseIndexSearcher(bibFieldsSearcherManager, bibFieldsIndexSearcher);
                releaseIndexSearcher(linkedFilesSearcherManager, linkedFilesIndexSearcher);
                return searchResults;
            } else {
                IndexSearcher indexSearcher = getIndexedSearcher(bibFieldsSearcherManager);
                SearchResults searchResults = search(indexSearcher, searchQuery);
                releaseIndexSearcher(bibFieldsSearcherManager, indexSearcher);
                return searchResults;
            }
        } catch (IOException | IndexSearcher.TooManyClauses e) {
            LOGGER.error("Error while searching", e);
        }
        return new SearchResults();
    }

    private SearchResults search(IndexSearcher indexSearcher, Query searchQuery) throws IOException {
        TopDocs topDocs = indexSearcher.search(searchQuery, Integer.MAX_VALUE);
        StoredFields storedFields = indexSearcher.storedFields();
        LOGGER.debug("Found {} matches", topDocs.totalHits.value);
        return getSearchResults(topDocs, storedFields, searchQuery);
    }

    private SearchResults getSearchResults(TopDocs topDocs, StoredFields storedFields, Query searchQuery) throws IOException {
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
        Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter("<b>", "</b>"), new QueryScorer(searchQuery));
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
                searchResults.addSearchResult(entriesMap.get(entryId), new SearchResult(score));
            }
        }
        LOGGER.debug("Mapping search results took {} ms", System.currentTimeMillis() - startTime);
        return searchResults;
    }

    private static String getFieldContents(Document document, SearchFieldConstants
        field) {
        return Optional.ofNullable(document.get(field.toString())).orElse("");
    }

    private static IndexSearcher getIndexedSearcher(SearcherManager searcherManager) throws IOException {
        searcherManager.maybeRefreshBlocking();
        return searcherManager.acquire();
    }

    private static void releaseIndexSearcher(SearcherManager searcherManager, IndexSearcher indexSearcher) throws IOException {
        searcherManager.release(indexSearcher);
    }
}
