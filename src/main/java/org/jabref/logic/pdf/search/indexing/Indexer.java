package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.nio.file.Paths;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;


/**
 * Indexes the text of pdf files and adds it into the lucene index.
 */
public class Indexer {
    private static final Log LOGGER = LogFactory.getLog(Indexer.class);

    private final Directory directoryToIndex;

    public Indexer() throws IOException {
        this.directoryToIndex = new SimpleFSDirectory(Paths.get("src/main/resources/luceneIndex"));
    }

    public Directory getIndexDirectory() {
        return this.directoryToIndex;
    }

    /**
     * Adds all pdf files linked to an entry in the database to the lucene search index
     *
     * @param database a bibtex database to link the pdf files to
     */
    public void createIndex(BibDatabase database) {
        try (IndexWriter indexWriter = new IndexWriter(directoryToIndex,
                new IndexWriterConfig(new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE))) {
            database.getEntries().stream().
                    filter(entry -> entry.hasField(FieldName.FILE)).
                    filter(entry -> entry.getCiteKeyOptional().isPresent()).
                    forEach(entry -> writeToIndex(entry, indexWriter));
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    /**
     * Deletes all entries from an index.
     */
    public void flushIndex() {

        IndexWriterConfig config = new IndexWriterConfig();
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try (IndexWriter deleter = new IndexWriter(directoryToIndex, config)) {
            // Do nothing. Index is deleted.
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    /**
     * Adds all the pdf files linked to an entry in the database to the lucene search index
     *
     * @param entry a bibtex entry to link the pdf files to
     */
    public void appendToIndex(BibEntry entry) {
        try (IndexWriter indexWriter = new IndexWriter(directoryToIndex,
                new IndexWriterConfig(new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {

            if (entry.hasField(FieldName.FILE) && entry.getCiteKeyOptional().isPresent()) {
                writeToIndex(entry, indexWriter);
            }
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    private void writeToIndex(BibEntry entry, IndexWriter indexWriter) {
        try {
            indexWriter.addDocument(new DocumentReader(entry).readPDFContents());
        } catch (IOException e) {
            LOGGER.debug("Document could not be added to the index.", e);
        }
    }
}
