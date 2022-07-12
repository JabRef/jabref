package org.jabref.logic.pdf.search.retrieval;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.search.EnglishStemAnalyzer;
import org.jabref.model.pdf.search.LuceneSearchResults;
import org.jabref.model.pdf.search.SearchFieldConstants;
import org.jabref.model.pdf.search.SearchResult;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.strings.StringUtil;

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
     * @param searchString a pattern to search for matching entries in the index, must not be null
     * @param maxHits      number of maximum search results, must be positive
     * @return a result set of all documents that have matches in any fields
     */
    public LuceneSearchResults search(final String searchString, final int maxHits)
        throws IOException {
        Thread.dumpStack();
        if (StringUtil.isBlank(Objects.requireNonNull(searchString, "The search string was null!"))) {
            return new LuceneSearchResults();
        }
        if (maxHits <= 0) {
            throw new IllegalArgumentException("Must be called with at least 1 maxHits, was" + maxHits);
        }

        List<SearchResult> resultDocs = new LinkedList<>();

        if (!DirectoryReader.indexExists(indexDirectory)) {
            LOGGER.debug("Index directory {} does not yet exist", indexDirectory);
            return new LuceneSearchResults();
        }

        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            String[] searchable_fields = new String[SearchFieldConstants.searchableBibFields.size()];
            SearchFieldConstants.searchableBibFields.toArray(searchable_fields);
            Query query = new MultiFieldQueryParser(searchable_fields, new EnglishStemAnalyzer()).parse(searchString);
            TopDocs results = searcher.search(query, maxHits);
            for (ScoreDoc scoreDoc : results.scoreDocs) {
                resultDocs.add(new SearchResult(searcher, query, scoreDoc));
            }
            return new LuceneSearchResults(resultDocs);
        } catch (ParseException e) {
            LOGGER.warn("Could not parse query: '{}'!\n{}", searchString, e.getMessage());
            return new LuceneSearchResults();
        }
    }

    public HashMap<BibEntry, List<SearchResult>> search(SearchQuery query) {
        HashMap<BibEntry, List<SearchResult>> results = new HashMap<>();
        HashMap<String, Float> boosts = new HashMap<String, Float>();
        SearchFieldConstants.searchableBibFields.stream().forEach(field -> boosts.put(field, Float.valueOf(4)));

        if (query.getSearchFlags().contains(SearchRules.SearchFlags.FULLTEXT)) {
            Arrays.stream(SearchFieldConstants.PDF_FIELDS).forEach(field -> boosts.put(field, Float.valueOf(1)));
        }
        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            String[] fieldsToSearchArray = new String[boosts.size()];
            boosts.keySet().toArray(fieldsToSearchArray);
            Query luceneQuery = new MultiFieldQueryParser(fieldsToSearchArray, new EnglishStemAnalyzer(), boosts).parse(query.getQuery());
            TopDocs docs = searcher.search(luceneQuery, Integer.MAX_VALUE);
            for (ScoreDoc scoreDoc : docs.scoreDocs) {
                SearchResult searchResult = new SearchResult(searcher, luceneQuery, scoreDoc);
                for (BibEntry match : searchResult.getMatchingEntries(databaseContext)) {
                    if (!results.containsKey(match)) {
                        results.put(match, new LinkedList<>());
                    }
                    results.get(match).add(searchResult);
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
