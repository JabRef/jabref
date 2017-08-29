package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.nio.file.Paths;

import javafx.collections.ObservableList;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;


/**
 * Indexes the text of PDF files and adds it into the lucene search index.
 */
public class PdfIndexer {
    private static final Log LOGGER = LogFactory.getLog(PdfIndexer.class);

    private final Directory directoryToIndex;
    private BibDatabaseContext databaseContext;
    private BibDatabase database;

    public PdfIndexer() throws IOException {
        this.directoryToIndex = new SimpleFSDirectory(Paths.get("src/main/resources/luceneIndex"));
    }

    public Directory getIndexDirectory() {
        return this.directoryToIndex;
    }

    /**
     * Adds all PDF files linked to an entry in the database to new Lucene search index. Any previous state of the
     * Lucene search index will be deleted!
     *
     * @param perserResult a bibtex database to link the pdf files to
     */
    public void createIndex(ParserResult perserResult) {
        this.databaseContext = perserResult.getDatabaseContext();
        this.database = perserResult.getDatabase();
        try (IndexWriter indexWriter = new IndexWriter(directoryToIndex,
                new IndexWriterConfig(new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE))) {
            final ObservableList<BibEntry> entries = this.database.getEntries();

            entries.stream().filter(entry -> !entry.getFiles().isEmpty()).forEach(entry -> writeToIndex(entry, indexWriter));
        } catch (IOException e) {
            LOGGER.warn("Could not initialize the IndexWriter!", e);
        }
    }

    /**
     * Adds all PDF files linked to an entry in the database to new Lucene search index. Any previous state of the
     * Lucene search index will be deleted!
     *
     * @param database a bibtex database to link the pdf files to
     */
    public void createIndex(BibDatabase database, BibDatabaseContext context) {
        this.databaseContext = context;
        this.database = database;
        try (IndexWriter indexWriter = new IndexWriter(directoryToIndex,
                new IndexWriterConfig(new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE))) {
            final ObservableList<BibEntry> entries = this.database.getEntries();

            entries.stream().filter(entry -> !entry.getFiles().isEmpty()).forEach(entry -> writeToIndex(entry, indexWriter));
        } catch (IOException e) {
            LOGGER.warn("Could not initialize the IndexWriter!", e);
        }
    }

    /**
     * Adds all the pdf files linked to one entry in the database to an existing (or new) Lucene search index
     *
     * @param entry a bibtex entry to link the pdf files to
     */
    public void addToIndex(BibEntry entry) {
        try (IndexWriter indexWriter = new IndexWriter(
                directoryToIndex,
                new IndexWriterConfig(
                        new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))
        ) {
            if (!entry.getFiles().isEmpty()) {
                writeToIndex(entry, indexWriter);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not initialize the IndexWriter!", e);
        }
    }

    /**
     * Deletes all entries from the Lucene search index.
     */
    public void flushIndex() {
        IndexWriterConfig config = new IndexWriterConfig();
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try (IndexWriter deleter = new IndexWriter(directoryToIndex, config)) {
            // Do nothing. Index is deleted.
            return;
        } catch (IOException e) {
            LOGGER.warn("The IndexWriter could not be initialized", e);
        }
    }

    private void writeToIndex(BibEntry entry, IndexWriter indexWriter) {
        try {
            indexWriter.addDocuments(new DocumentReader(entry).readLinkedPdfs(this.databaseContext));
        } catch (IOException e) {
            LOGGER.warn("Could not add the documents to the index!", e);
        }
    }
}
