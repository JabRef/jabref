package org.jabref.logic.pdf.search.retrieval;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.search.EnglishStemAnalyzer;
import org.jabref.model.pdf.search.LuceneSearchResults;
import org.jabref.model.pdf.search.SearchFieldConstants;
import org.jabref.model.pdf.search.SearchResult;
import org.jabref.model.search.rules.SearchRules;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
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
        HashMap<String, Float> boosts = new HashMap<>();
        SearchFieldConstants.searchableBibFields.forEach(field -> boosts.put(field, Float.valueOf(4)));

        if (query.getSearchFlags().contains(SearchRules.SearchFlags.FULLTEXT)) {
            Arrays.stream(SearchFieldConstants.PDF_FIELDS).forEach(field -> boosts.put(field, Float.valueOf(1)));
        }
        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            String[] fieldsToSearchArray = new String[boosts.size()];
            boosts.keySet().toArray(fieldsToSearchArray);
            String queryString = query.getQuery();
            if (query.getSearchFlags().contains(SearchRules.SearchFlags.REGULAR_EXPRESSION)) {
                if (queryString.length() > 0 && !(queryString.startsWith("/") && queryString.endsWith("/"))) {
                    queryString = "/" + queryString + "/";
                }
            }
            Query luceneQuery = new MultiFieldQueryParser(fieldsToSearchArray, new EnglishStemAnalyzer(), boosts).parse(queryString);
            TopDocs docs = searcher.search(luceneQuery, Integer.MAX_VALUE);
            for (ScoreDoc scoreDoc : docs.scoreDocs) {
                SearchResult searchResult = new SearchResult(searcher, luceneQuery, scoreDoc);
                for (BibEntry match : searchResult.getMatchingEntries(databaseContext)) {
                    if (searchResult.getLuceneScore() > 0) {
                        if (!results.containsKey(match)) {
                            results.put(match, new LuceneSearchResults());
                        }
                        results.get(match).addResult(searchResult);
                    }
                }
            }
        } catch (ParseException e) {
            LOGGER.warn("Could not parse query: '{}'!\n{}", query.getQuery(), e.getMessage());
        } catch (IOException e) {
            LOGGER.warn("Could not open Index at: '{}'!\n{}", indexDirectory, e.getMessage());
        }
        return results;
    }
}
