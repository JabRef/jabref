package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.util.Optional;

import javafx.collections.ObservableList;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.pdf.search.SearchFieldConstants;
import org.jabref.preferences.FilePreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
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

    private final FilePreferences filePreferences;

    private PdfIndexer(Directory indexDirectory, FilePreferences filePreferences) {
        this.directoryToIndex = indexDirectory;
        this.filePreferences = filePreferences;
    }

    public static PdfIndexer of(BibDatabaseContext bibDatabaseContext, FilePreferences filePreferences) throws IOException {
        return new PdfIndexer(new SimpleFSDirectory(bibDatabaseContext.getFulltextIndexPath()), filePreferences);
    }

    public Directory getIndexDirectory() {
        return this.directoryToIndex;
    }

    /**
     * Adds all PDF files linked to an entry in the database to new Lucene search index. Any previous state of the
     * Lucene search index will be deleted!
     *
     * @param parserResult a bibtex database to link the pdf files to
     */
    public void createIndex(ParserResult parserResult) {
        createIndex(parserResult.getDatabase(), parserResult.getDatabaseContext());
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
            indexWriter.commit();
        } catch (IOException e) {
            LOGGER.warn("Could not initialize the IndexWriter!", e);
        }
    }

    /**
     * Adds all the pdf files linked to one entry in the database to an existing (or new) Lucene search index
     *
     * @param entry a bibtex entry to link the pdf files to
     */
    public void addToIndex(BibEntry entry, BibDatabaseContext databaseContext) {
        if (databaseContext != null) {
            this.databaseContext = databaseContext;
        }
        try (IndexWriter indexWriter = new IndexWriter(
                directoryToIndex,
                new IndexWriterConfig(
                        new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))
        ) {
            if (!entry.getFiles().isEmpty()) {
                writeToIndex(entry, indexWriter);
            }
            indexWriter.commit();
        } catch (IOException e) {
            LOGGER.warn("Could not initialize the IndexWriter!", e);
        }
    }

    /**
     * Adds a pdf file linked to one entry in the database to an existing (or new) Lucene search index
     *
     * @param entry a bibtex entry
     * @param linkedFile the link to the pdf files
     */
    public void addToIndex(BibEntry entry, LinkedFile linkedFile, BibDatabaseContext databaseContext) {
        if (databaseContext != null) {
            this.databaseContext = databaseContext;
        }
        try (IndexWriter indexWriter = new IndexWriter(
                directoryToIndex,
                new IndexWriterConfig(
                        new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))
        ) {
            if (!entry.getFiles().isEmpty()) {
                writeToIndex(entry, linkedFile, indexWriter);
            }
            indexWriter.commit();
        } catch (IOException e) {
            LOGGER.warn("Could not initialize the IndexWriter!", e);
        }
    }

    /**
     * Removes a pdf file linked to one entry in the database from the index
     * @param entry the entry the file is linked to
     * @param linkedFile the link to the file to be removed
     */
    public void removeFromIndex(BibEntry entry, LinkedFile linkedFile) {
        try (IndexWriter indexWriter = new IndexWriter(
                directoryToIndex,
                new IndexWriterConfig(
                        new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))
        ) {
            if (!entry.getFiles().isEmpty()) {
                indexWriter.deleteDocuments(new Term(SearchFieldConstants.PATH, linkedFile.getLink()));
            }
            indexWriter.commit();
        } catch (IOException e) {
            LOGGER.warn("Could not initialize the IndexWriter!", e);
        }
    }

    /**
     * Removes  all files linked to a bib-entry from the index
     * @param entry the entry documents are linked to
     */
    public void removeFromIndex(BibEntry entry) {
        for (LinkedFile linkedFile : entry.getFiles()) {
            removeFromIndex(entry, linkedFile);
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

    /**
     * Updates the index for the file document
     * @param entry the entry the document is linked to
     * @param linkedFile the document
     */
    public void updateIndex(BibEntry entry, LinkedFile linkedFile, BibDatabaseContext databaseContext) {
        if (databaseContext != null) {
            this.databaseContext = databaseContext;
        }
        try (IndexWriter indexWriter = new IndexWriter(
                directoryToIndex,
                new IndexWriterConfig(
                        new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))
        ) {
            if ("PDF".equals(linkedFile.getFileType())) {
                Optional<Document> document = new DocumentReader(entry, filePreferences).readLinkedPdf(databaseContext, linkedFile);
                if (document.isPresent()) {
                    indexWriter.updateDocument(new Term(SearchFieldConstants.PATH, linkedFile.getLink()), document.get());
                }
            }
            indexWriter.commit();
        } catch (IOException e) {
            LOGGER.warn("Could not initialize the IndexWriter!", e);
        }
    }

    /**
     * Updates the index for all files linked to a bib-entry
     * @param entry the entry documents are linked to
     */
    public void updateIndex(BibEntry entry, BibDatabaseContext databaseContext) {
        if (databaseContext != null) {
            this.databaseContext = databaseContext;
        }
        for (LinkedFile linkedFile : entry.getFiles()) {
            updateIndex(entry, linkedFile, databaseContext);
        }
    }

    private void writeToIndex(BibEntry entry, IndexWriter indexWriter) {
        try {
            indexWriter.addDocuments(new DocumentReader(entry, filePreferences).readLinkedPdfs(this.databaseContext));
        } catch (IOException e) {
            LOGGER.warn("Could not add the documents to the index!", e);
        }
    }

    private void writeToIndex(BibEntry entry, LinkedFile linkedFile, IndexWriter indexWriter) {
        try {
            Optional<Document> document = new DocumentReader(entry, filePreferences).readLinkedPdf(this.databaseContext, linkedFile);
            if (document.isPresent()) {
                indexWriter.addDocument(document.get());
            }
        } catch (IOException e) {
            LOGGER.warn("Could not add the document to the index!", e);
        }
    }
}
