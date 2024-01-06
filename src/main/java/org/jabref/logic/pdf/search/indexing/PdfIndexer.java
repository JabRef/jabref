package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.pdf.search.EnglishStemAnalyzer;
import org.jabref.model.pdf.search.SearchFieldConstants;
import org.jabref.preferences.FilePreferences;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indexes the text of PDF files and adds it into the lucene search index.
 */
public class PdfIndexer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfIndexer.class);

    private final Directory directoryToIndex;
    private BibDatabaseContext databaseContext;

    private IndexWriter indexWriter;

    private final FilePreferences filePreferences;

    public PdfIndexer(Directory indexDirectory, FilePreferences filePreferences) {
        this.directoryToIndex = indexDirectory;
        this.filePreferences = filePreferences;
    }

    public static PdfIndexer of(BibDatabaseContext databaseContext, FilePreferences filePreferences) throws IOException {
        return new PdfIndexer(new NIOFSDirectory(databaseContext.getFulltextIndexPath()), filePreferences);
    }

    /**
     * Creates (and thus resets) the PDF index. No re-indexing will be done.
     * Any previous state of the Lucene search is deleted.
     */
    public void createIndex() {
        try {
            indexWriter = new IndexWriter(
                    directoryToIndex,
                    new IndexWriterConfig(
                            new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE));
            LOGGER.debug("Created new index for directory {}.", directoryToIndex);
        } catch (IOException e) {
            LOGGER.warn("Could not create new index", e);
        }
    }

    private IndexWriter getIndexWriter() {
        if (indexWriter == null) {
            initializeIndexWriter();
        }
        return indexWriter;
    }

    private void initializeIndexWriter() {
        try {
            indexWriter = new IndexWriter(
                    directoryToIndex,
                    new IndexWriterConfig(
                            new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND));
        } catch (IOException e) {
            LOGGER.error("Could not initialize the IndexWriter", e);
        }
    }

    public void addToIndex(BibDatabaseContext databaseContext) {
        for (BibEntry entry : databaseContext.getEntries()) {
            addToIndex(entry, databaseContext);
        }
    }

    /**
     * Adds all PDF files linked to one entry in the database to an existing (or new) Lucene search index
     *
     * @param entry a bibtex entry to link the pdf files to
     * @param databaseContext the associated BibDatabaseContext
     */
    public void addToIndex(BibEntry entry, BibDatabaseContext databaseContext) {
        addToIndex(entry, entry.getFiles(), databaseContext);
    }

    /**
     * Adds a list of pdf files linked to one entry in the database to an existing (or new) Lucene search index
     *
     * @param entry a bibtex entry to link the pdf files to
     * @param databaseContext the associated BibDatabaseContext
     */
    public void addToIndex(BibEntry entry, List<LinkedFile> linkedFiles, BibDatabaseContext databaseContext) {
        for (LinkedFile linkedFile : linkedFiles) {
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
            // TODO: This needs to be commented. The databaseContext should exist?! Maybe when an unsaved database is saved?
            this.databaseContext = databaseContext;
        }
        if (!entry.getFiles().isEmpty()) {
            addToIndex(entry, linkedFile);
        } else {
            // TODO: Can this happen?
            LOGGER.debug("Tried to index {}, which is not attached to BibEntry {}", linkedFile, entry);
        }
    }

    /**
     * Removes a pdf file identified by its path from the index
     *
     * @param linkedFilePath the path to the file to be removed
     */
    public void removeFromIndex(String linkedFilePath) {
        try {
            getIndexWriter().deleteDocuments(new Term(SearchFieldConstants.PATH, linkedFilePath));
            getIndexWriter().commit();
        } catch (IOException e) {
            LOGGER.debug("Could not remove document {} from the index.", linkedFilePath, e);
        }
    }

    /**
     * Removes  all files linked to a bib-entry from the index
     *
     * @param entry the entry documents are linked to
     */
    public void removeFromIndex(BibEntry entry) {
        removeFromIndex(entry, entry.getFiles());
    }

    /**
     * Removes a list of files linked to a bib-entry from the index
     *
     * @param entry the entry documents are linked to
     */
    public void removeFromIndex(BibEntry entry, List<LinkedFile> linkedFiles) {
        for (LinkedFile linkedFile : linkedFiles) {
            removeFromIndex(linkedFile.getLink());
        }
    }

    /**
     * Writes the file to the index if the file is not yet in the index or the file on the fs is newer than the one in
     * the index.
     *
     * @param entry the entry associated with the file
     * @param linkedFile the file to write to the index
     */
    private void addToIndex(BibEntry entry, LinkedFile linkedFile) {
        if (linkedFile.isOnlineLink() || !StandardFileType.PDF.getName().equals(linkedFile.getFileType())) {
            return;
        }
        Optional<Path> resolvedPath = linkedFile.findIn(databaseContext, filePreferences);
        if (resolvedPath.isEmpty()) {
            LOGGER.debug("Could not find {}", linkedFile.getLink());
            return;
        }
        LOGGER.debug("Adding {} to index", linkedFile.getLink());
        try {
            // Check if a document with this path is already in the index
            try (IndexReader reader = DirectoryReader.open(directoryToIndex)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TermQuery query = new TermQuery(new Term(SearchFieldConstants.PATH, linkedFile.getLink()));
                TopDocs topDocs = searcher.search(query, 1);
                // If a document was found, check if is less current than the one in the FS
                if (topDocs.scoreDocs.length > 0) {
                    Document doc = reader.document(topDocs.scoreDocs[0].doc);
                    long indexModificationTime = Long.parseLong(doc.getField(SearchFieldConstants.MODIFIED).stringValue());
                    BasicFileAttributes attributes = Files.readAttributes(resolvedPath.get(), BasicFileAttributes.class);
                    if (indexModificationTime >= attributes.lastModifiedTime().to(TimeUnit.SECONDS)) {
                        LOGGER.debug("File {} is already indexed", linkedFile.getLink());
                        return;
                    }
                }
            } catch (IndexNotFoundException e) {
                LOGGER.debug("Index not found. Continuing.", e);
            }
            // If no document was found, add the new one
            Optional<List<Document>> pages = new DocumentReader(entry, filePreferences).readLinkedPdf(this.databaseContext, linkedFile);
            if (pages.isPresent()) {
                getIndexWriter().addDocuments(pages.get());
                getIndexWriter().commit();
            }
        } catch (IOException e) {
            LOGGER.warn("Could not add document {} to the index.", linkedFile.getLink(), e);
        }
    }

    /**
     * Lists the paths of all the files that are stored in the index
     *
     * @return all file paths
     */
    public Set<String> getListOfFilePaths() {
        Set<String> paths = new HashSet<>();
        try (IndexReader reader = DirectoryReader.open(directoryToIndex)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            MatchAllDocsQuery query = new MatchAllDocsQuery();
            TopDocs allDocs = searcher.search(query, Integer.MAX_VALUE);
            for (ScoreDoc scoreDoc : allDocs.scoreDocs) {
                Document doc = reader.document(scoreDoc.doc);
                paths.add(doc.getField(SearchFieldConstants.PATH).stringValue());
            }
        } catch (IOException e) {
            LOGGER.debug("Could not read from index. Returning intermediate result.", e);
            return paths;
        }
        return paths;
    }
}
