package org.jabref.logic.pdf.search;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.pdf.search.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;

public class Indexer {
    private static final Log LOGGER = LogFactory.getLog(Indexer.class);

    private final Directory directoryToIndex;
    private StandardAnalyzer analyzer;

    public Indexer(Path directory) throws IOException {
        this.directoryToIndex = new SimpleFSDirectory(directory);
    }

    /**
     * Adds all pdf files linked to an entry in the database to the lucene search index
     *
     * @param database a bibtex database to link the pdf files to
     */
    public void addDocuments(BibDatabase database) {

        try (IndexWriter indexWriter = new IndexWriter(directoryToIndex, new IndexWriterConfig(new StandardAnalyzer()))) {

            database.getEntries().stream().
                    filter(entry -> entry.hasField(FieldName.FILE)).
                    filter(entry -> entry.getCiteKeyOptional().isPresent()).
                    forEach(entry -> addToIndex(entry, indexWriter));
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        }
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
    public ResultSet searchWithIndex(Path pathToDirectory, String searchQuery, String[] fields) throws IOException, ParseException {

        Query q = new MultiFieldQueryParser(fields, analyzer).parse(searchQuery);

        int hitsPerPage = 10;
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(pathToDirectory));
        IndexSearcher searcher = new IndexSearcher(reader);

        TopDocs hits = searcher.search(q, hitsPerPage);

        Document[] searchResults = new Document[hits.totalHits];

        for (int i = 0; i < hits.totalHits; ++i) {
            int docId = hits.scoreDocs[i].doc;
            searchResults[i] = searcher.doc(docId);
        }
        return new ResultSet(searchResults);
    }
}
