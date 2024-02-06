package org.jabref.logic.pdf.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.LibraryTab;
import org.jabref.model.pdf.search.EnglishStemAnalyzer;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.pdf.search.SearchResult;
import org.jabref.model.strings.StringUtil;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.pdf.search.SearchFieldConstants.PDF_FIELDS;

public final class PdfSearcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);

    private final PdfIndexer indexer;
    private EnglishStemAnalyzer englishStemAnalyzer = new EnglishStemAnalyzer();

    private PdfSearcher(PdfIndexer indexer) {
        this.indexer = indexer;
    }

    public static PdfSearcher of(PdfIndexer indexer) throws IOException {
        return new PdfSearcher(indexer);
    }

    /**
     * Search for results matching a query in the Lucene search index
     *
     * @param searchString a pattern to search for matching entries in the index, must not be null
     * @param maxHits      number of maximum search results, must be positive
     * @return a result set of all documents that have matches in any fields
     */
    public PdfSearchResults search(final String searchString, final int maxHits) throws IOException {
        if (StringUtil.isBlank(Objects.requireNonNull(searchString, "The search string was null."))) {
            return new PdfSearchResults();
        }
        if (maxHits <= 0) {
            throw new IllegalArgumentException("Must be called with at least 1 maxHits, was " + maxHits);
        }

        List<SearchResult> resultDocs = new ArrayList<>();
        // We need to point the DirectoryReader to the indexer, because we get errors otherwise
        // Hint from https://stackoverflow.com/a/63673753/873282.
        Optional<IndexWriter> optionalIndexWriter = indexer.getIndexWriter();
        if (optionalIndexWriter.isEmpty()) {
            LOGGER.info("No index writer present, returning empty result set.");
            return new PdfSearchResults();
        }
        try (IndexReader reader = DirectoryReader.open(optionalIndexWriter.get())) {
            Query query = new MultiFieldQueryParser(PDF_FIELDS, englishStemAnalyzer).parse(searchString);
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs results = searcher.search(query, maxHits);
            for (ScoreDoc scoreDoc : results.scoreDocs) {
                resultDocs.add(new SearchResult(searcher, query, scoreDoc));
            }
            return new PdfSearchResults(resultDocs);
        } catch (ParseException e) {
            LOGGER.warn("Could not parse query: '{}'", searchString, e);
            return new PdfSearchResults();
        }
    }
}
