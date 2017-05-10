package org.jabref.logic.pdf.search;

import java.io.IOException;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.pdf.search.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Indexer {
    private static final Log LOGGER = LogFactory.getLog(Indexer.class);

    Directory directoryToIndex;
    StandardAnalyzer analyzer;

    /**
     * Initialize an index to search for pdfs in a specific directory
     *
     * @param pathToIndex path to the directory where the index is created
     */
    public void initializeIndex(String pathToIndex) throws IOException {

        directoryToIndex = FSDirectory.getDirectory(pathToIndex);
        analyzer = new StandardAnalyzer();
    }

    /**
     * Adds all pdf files linked to an entry in the database to the lucene search index
     *
     * @param database a bibtex database to link the pdf files to
     */
    public void addDocuments(BibDatabase database) throws IOException {

        IndexWriter indexWriter = new IndexWriter(directoryToIndex, analyzer, true,
                IndexWriter.MaxFieldLength.UNLIMITED);

        database.getEntries().stream().
                filter(entry -> entry.hasField(FieldName.FILE)).
                filter(entry -> entry.getCiteKeyOptional().isPresent()).
                forEach(entry -> addToIndex(entry, indexWriter));
    }

    private void addToIndex(BibEntry entry, IndexWriter indexWriter) {
        try {
            indexWriter.addDocument(new DocumentReader(entry).readPDFContents());
        } catch (IOException e) {
            LOGGER.debug("Document could not be added to the index.", e);
        }
    }

    /**
     * search for results matching a query in a specified directory
     *
     * @param pathToDirectory path in which the index must be stored
     * @param searchQuery     a pattern to search for matching entries in the index
     * @param fields          the fields of lucene documents so match the query with
     * @return a result set of all documents that have matches in any fields
     */
    public ResultSet searchWithIndex(String pathToDirectory, String searchQuery, String[] fields) throws ParseException, IOException {

        Query q = new MultiFieldQueryParser(fields,
                analyzer).parse(searchQuery);

        int hitsPerPage = 10;
        IndexSearcher searcher = new IndexSearcher(pathToDirectory);
        TopDocCollector collector = new TopDocCollector(hitsPerPage);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        Document[] searchResults = new Document[hits.length];

        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            searchResults[i] = searcher.doc(docId);
        }
        return new ResultSet(searchResults, collector);
    }
}
