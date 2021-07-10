package org.jabref.logic.pdf.search.retrieval;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.jabref.gui.LibraryTab;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.pdf.search.EnglishStemAnalyzer;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.pdf.search.SearchResult;
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

import static org.jabref.model.pdf.search.SearchFieldConstants.PDF_FIELDS;

public final class PdfSearcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);

    private final Directory indexDirectory;

    private PdfSearcher(Directory indexDirectory) {
        this.indexDirectory = indexDirectory;
    }

    public static PdfSearcher of(BibDatabaseContext databaseContext) throws IOException {
        return new PdfSearcher(new NIOFSDirectory(databaseContext.getFulltextIndexPath()));
    }

    /**
     * Search for results matching a query in the Lucene search index
     *
     * @param searchString a pattern to search for matching entries in the index, must not be null
     * @param maxHits      number of maximum search results, must be positive
     * @return a result set of all documents that have matches in any fields
     */
    public PdfSearchResults search(final String searchString, final int maxHits)
            throws IOException {
        if (StringUtil.isBlank(Objects.requireNonNull(searchString, "The search string was null!"))) {
            return new PdfSearchResults();
        }
        if (maxHits <= 0) {
            throw new IllegalArgumentException("Must be called with at least 1 maxHits, was" + maxHits);
        }

        try {
            List<SearchResult> resultDocs = new LinkedList<>();

            IndexReader reader = DirectoryReader.open(indexDirectory);
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = new MultiFieldQueryParser(PDF_FIELDS, new EnglishStemAnalyzer()).parse(searchString);
            TopDocs results = searcher.search(query, maxHits);
            for (ScoreDoc scoreDoc : results.scoreDocs) {
                resultDocs.add(new SearchResult(searcher, query, scoreDoc));
            }
            return new PdfSearchResults(resultDocs);
        } catch (ParseException e) {
            LOGGER.warn("Could not parse query: '" + searchString + "'! \n" + e.getMessage());
            return new PdfSearchResults();
        }
    }
}
