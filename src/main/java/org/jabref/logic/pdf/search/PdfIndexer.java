package org.jabref.logic.pdf.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
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

import com.google.common.annotations.VisibleForTesting;
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

    @VisibleForTesting
    IndexWriter indexWriter;

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final Directory indexDirectory;
    private IndexReader reader;

    private PdfIndexer(BibDatabaseContext databaseContext, Directory indexDirectory, FilePreferences filePreferences) {
        this.databaseContext = databaseContext;
        this.indexDirectory = indexDirectory;
        this.filePreferences = filePreferences;
    }

    /**
     * Method is public, because DatabaseSearcherWithBibFilesTest resides in another package
     */
    @VisibleForTesting
    public static PdfIndexer of(BibDatabaseContext databaseContext, Path indexDirectory, FilePreferences filePreferences) throws IOException {
        return new PdfIndexer(databaseContext, new NIOFSDirectory(indexDirectory), filePreferences);
    }

    /**
     * Method is public, because DatabaseSearcherWithBibFilesTest resides in another package
     */
    @VisibleForTesting
    public static PdfIndexer of(BibDatabaseContext databaseContext, FilePreferences filePreferences) throws IOException {
        return new PdfIndexer(databaseContext, new NIOFSDirectory(databaseContext.getFulltextIndexPath()), filePreferences);
    }

    /**
     * Creates (and thus resets) the PDF index. No re-indexing will be done.
     * Any previous state of the Lucene search is deleted.
     */
    public void createIndex() {
        LOGGER.debug("Creating new index for directory {}.", indexDirectory);
        initializeIndexWriterAndReader(IndexWriterConfig.OpenMode.CREATE);
    }

    /**
     * Needs to be accessed by {@link PdfSearcher}
     */
    IndexWriter getIndexWriter() {
        LOGGER.trace("Getting the index writer");
        if (indexWriter == null) {
            LOGGER.trace("Initializing the index writer");
            initializeIndexWriterAndReader(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        } else {
            LOGGER.trace("Using existing index writer");
        }
        return indexWriter;
    }

    private void initializeIndexWriterAndReader(IndexWriterConfig.OpenMode mode) {
        try {
            indexWriter = new IndexWriter(
                    indexDirectory,
                    new IndexWriterConfig(
                            new EnglishStemAnalyzer()).setOpenMode(mode));
        } catch (IOException e) {
            LOGGER.error("Could not initialize the IndexWriter", e);
        }
        try {
            reader = DirectoryReader.open(indexWriter);
        } catch (IOException e) {
            LOGGER.error("Could not initialize the IndexReader", e);
        }
    }

    /**
     * Rebuilds the PDF index. All PDF files linked to entries in the database will be re-indexed.
     */
    public void rebuildIndex() {
        LOGGER.debug("Rebuilding index.");
        createIndex();
        addToIndex(databaseContext.getEntries());
    }

    public void addToIndex(List<BibEntry> entries) {
        int count = 0;
        for (BibEntry entry : entries) {
            addToIndex(entry, false);
            count++;
            if (count % 100 == 0) {
                doCommit();
            }
        }
        doCommit();
        LOGGER.debug("Added {} documents to the index.", count);
    }

    /**
     * Adds all PDF files linked to one entry in the database to an existing (or new) Lucene search index
     *
     * @param entry a bibtex entry to link the pdf files to
     */
    public void addToIndex(BibEntry entry) {
        addToIndex(entry, entry.getFiles(), true);
    }

    private void addToIndex(BibEntry entry, boolean shouldCommit) {
        addToIndex(entry, entry.getFiles(), false);
        if (shouldCommit) {
            doCommit();
        }
    }

    /**
     * Adds a list of pdf files linked to one entry in the database to an existing (or new) Lucene search index
     *
     * @param entry a bibtex entry to link the pdf files to
     */
    public void addToIndex(BibEntry entry, Collection<LinkedFile> linkedFiles) {
        addToIndex(entry, linkedFiles, true);
    }

    public void addToIndex(BibEntry entry, Collection<LinkedFile> linkedFiles, boolean shouldCommit) {
        for (LinkedFile linkedFile : linkedFiles) {
            addToIndex(entry, linkedFile, false);
        }
        if (shouldCommit) {
            doCommit();
        }
    }

    private void doCommit() {
        try {
            getIndexWriter().commit();
        } catch (IOException e) {
            LOGGER.warn("Could not commit changes to the index.", e);
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
        removeFromIndex(entry.getFiles());
    }

    /**
     * Removes a list of files linked to a bib-entry from the index
     */
    public void removeFromIndex(Collection<LinkedFile> linkedFiles) {
        for (LinkedFile linkedFile : linkedFiles) {
            removeFromIndex(linkedFile.getLink());
        }
    }

    public void removePathsFromIndex(Collection<String> linkedFiles) {
        for (String linkedFile : linkedFiles) {
            removeFromIndex(linkedFile);
        }
    }

    /**
     * Writes the file to the index if the file is not yet in the index or the file on the fs is newer than the one in
     * the index.
     *
     * @param entry the entry associated with the file
     * @param linkedFile the file to write to the index
     */
    public void addToIndex(BibEntry entry, LinkedFile linkedFile) {
        this.addToIndex(entry, linkedFile, true);
    }

    private void addToIndex(BibEntry entry, LinkedFile linkedFile, boolean shouldCommit) {
        if (linkedFile.isOnlineLink() ||
                (!StandardFileType.PDF.getName().equals(linkedFile.getFileType()) &&
                        // We do not require the file type to be set
                        (!linkedFile.getLink().endsWith(".pdf") && !linkedFile.getLink().endsWith(".PDF")))) {
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
            try {
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
                if (shouldCommit) {
                    getIndexWriter().commit();
                }
            } else {
                LOGGER.debug("No content found in file {}", linkedFile.getLink());
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
        try (IndexReader reader = DirectoryReader.open(getIndexWriter())) {
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

    public void close() throws IOException {
        indexWriter.close();
    }
}
