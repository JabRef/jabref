package org.jabref.logic.pdf.search.retrieval;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.pdf.search.indexing.EnglishStemAnalyzer;
import org.jabref.model.pdf.search.ResultSet;
import org.jabref.model.pdf.search.SearchResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import static org.jabref.model.pdf.search.SearchFieldConstants.PDF_FIELDS;

public final class PdfSearcher {
    private static final Log LOGGER = LogFactory.getLog(PdfSearcher.class);

    private final Directory indexDirectory;

    public PdfSearcher() throws IOException {
        this.indexDirectory = new SimpleFSDirectory(Paths.get("src/main/resources/luceneIndex"));
    }

    /**
     * Search for results matching a query in a specified directory
     *
     * @param searchString a pattern to search for matching entries in the index, must not be null
     * @param maxHits      number of maximum search results, must be positive
     * @return a result set of all documents that have matches in any fields
     */
    public ResultSet search(String searchString, int maxHits) throws IOException {
        if (Objects.requireNonNull(searchString, "The search string was null!").isEmpty()) {
            return new ResultSet();
        }
        if (maxHits <= 0) {
            throw new IllegalArgumentException("Must be called with at least 1 maxHits, was" + maxHits);
        }

        try {
            List<SearchResult> resultDocs = new LinkedList<>();

            IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(indexDirectory));
            Query query = new MultiFieldQueryParser(PDF_FIELDS, new EnglishStemAnalyzer()).parse(searchString);
            for (ScoreDoc scoreDoc : searcher.search(query, maxHits).scoreDocs) {
                resultDocs.add(new SearchResult(searcher, scoreDoc));
            }
            return new ResultSet(resultDocs);
        } catch (ParseException e) {
            LOGGER.warn("Could not parse query: '" + searchString + "'! \n" + e.getMessage());
            return new ResultSet();
        }
    }
}
