package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.swing.text.html.Option;

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
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import static org.jabref.model.pdf.search.SearchFieldConstants.MODIFIED;

/**
 * Indexes the text of PDF files and adds it into the lucene search index.
 */
public class PdfIndexer {
    private static final Log LOGGER = LogFactory.getLog(PdfIndexer.class);

    private final Directory directoryToIndex;
    private BibDatabaseContext databaseContext;
    private BibDatabase database;

    private final FilePreferences filePreferences;

    public PdfIndexer(Directory indexDirectory, FilePreferences filePreferences) {
        this.directoryToIndex = indexDirectory;
        this.filePreferences = filePreferences;
    }

    public static PdfIndexer of(BibDatabaseContext databaseContext, FilePreferences filePreferences) throws IOException {
        return new PdfIndexer(new SimpleFSDirectory(databaseContext.getFulltextIndexPath()), filePreferences);
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
        final ObservableList<BibEntry> entries = this.database.getEntries();

        entries.stream().filter(entry -> !entry.getFiles().isEmpty()).forEach(entry -> writeToIndex(entry));
    }

    public void addToIndex(BibDatabaseContext databaseContext) {
        for (BibEntry entry : databaseContext.getEntries()) {
            addToIndex(entry, databaseContext);
        }
    }

    /**
     * Adds all the pdf files linked to one entry in the database to an existing (or new) Lucene search index
     *
     * @param entry a bibtex entry to link the pdf files to
     */
    public void addToIndex(BibEntry entry, BibDatabaseContext databaseContext) {
        for (LinkedFile linkedFile : entry.getFiles()) {
            addToIndex(entry, linkedFile, databaseContext);
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
        if (!entry.getFiles().isEmpty()) {
            writeToIndex(entry, linkedFile);
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
     * Writes all files linked to an entry to the index if the files are not yet in the index or the files on the fs are
     * newer than the one in the index.
     * @param entry the entry associated with the file
     */
    private void writeToIndex(BibEntry entry) {
        for (LinkedFile linkedFile : entry.getFiles()) {
            writeToIndex(entry, linkedFile);
        }
    }

    /**
     * Writes the file to the index if the file is not yet in the index or the file on the fs is newer than the one in
     * the index.
     * @param entry the entry associated with the file
     * @param linkedFile the file to write to the index
     */
    private void writeToIndex(BibEntry entry, LinkedFile linkedFile) {
        Optional<Path> resolvedPath = linkedFile.findIn(databaseContext, filePreferences);
        if (resolvedPath.isEmpty()) {
            LOGGER.warn("Could not find " + linkedFile.getLink());
            return;
        }
        try {
            // Check if a document with this path is already in the index
            try {
                IndexReader reader = DirectoryReader.open(directoryToIndex);
                IndexSearcher searcher = new IndexSearcher(reader);
                TermQuery query = new TermQuery(new Term(SearchFieldConstants.PATH, linkedFile.getLink()));
                TopDocs topDocs = searcher.search(query, 1);
                // If a document was found, check if is less current than the one in the FS
                if (topDocs.scoreDocs.length > 0) {
                    Document doc = reader.document(topDocs.scoreDocs[0].doc);
                    long indexModificationTime = Long.valueOf(doc.getField(SearchFieldConstants.MODIFIED).stringValue());

                    BasicFileAttributes attributes = Files.readAttributes(resolvedPath.get(), BasicFileAttributes.class);

                    if (indexModificationTime >= attributes.lastModifiedTime().to(TimeUnit.SECONDS)) {
                        return;
                    }
                }
                reader.close();
            } catch (IndexNotFoundException e) {
                // if there is no index yet, don't need to check anything!
            }
            // If no document was found, add the new one
            Optional<Document> document = new DocumentReader(entry, filePreferences).readLinkedPdf(this.databaseContext, linkedFile);
            if (document.isPresent()) {
                IndexWriter indexWriter = new IndexWriter(directoryToIndex,
                        new IndexWriterConfig(
                                new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND));
                indexWriter.addDocument(document.get());
                indexWriter.commit();
                indexWriter.close();
            }
        } catch (IOException e) {
            LOGGER.warn("Could not add the document to the index!", e);
        }
    }
}
