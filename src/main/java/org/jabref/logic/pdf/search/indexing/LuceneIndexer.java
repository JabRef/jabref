package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.pdf.search.EnglishStemAnalyzer;
import org.jabref.model.pdf.search.SearchFieldConstants;
import org.jabref.preferences.FilePreferences;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
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
public class LuceneIndexer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);

    private final Directory directoryToIndex;
    private final BibDatabaseContext databaseContext;

    private final FilePreferences filePreferences;

    public LuceneIndexer(BibDatabaseContext databaseContext, FilePreferences filePreferences) throws IOException {
        this.databaseContext = databaseContext;
        this.directoryToIndex = new NIOFSDirectory(databaseContext.getFulltextIndexPath());
        this.filePreferences = filePreferences;
    }

    public static LuceneIndexer of(BibDatabaseContext databaseContext, FilePreferences filePreferences) throws IOException {
        return new LuceneIndexer(databaseContext, filePreferences);
    }

    public BibDatabaseContext getDatabaseContext() {
        return databaseContext;
    }

    /**
     * Adds all PDF files linked to an entry in the database to new Lucene search index. Any previous state of the
     * Lucene search index will be deleted!
     */
    public void createIndex() {
        // Create new index by creating IndexWriter but not writing anything.
        try (IndexWriter indexWriter = new IndexWriter(directoryToIndex, new IndexWriterConfig(new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE))) {
            // empty comment for checkstyle
        } catch (IOException e) {
            LOGGER.warn("Could not create new Index!", e);
        }
    }

    /**
     * Adds all the pdf files linked to one entry in the database to an existing (or new) Lucene search index
     *
     * @param entry a bibtex entry to link the pdf files to
     */
    public void addLinkedFilesToIndex(BibEntry entry) {
        for (LinkedFile file : entry.getFiles()) {
            writeFileToIndex(entry, file);
        }
    }

    /**
     * Removes an entry identified by its hash from the index
     *
     * @param hash the hash to be removed
     */
    public void removeFromIndex(int hash) {
        try (IndexWriter indexWriter = new IndexWriter(
                directoryToIndex,
                new IndexWriterConfig(
                        new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            indexWriter.deleteDocuments(new Term(SearchFieldConstants.BIB_ENTRY_ID_HASH, String.valueOf(hash)));
            indexWriter.commit();
        } catch (IOException e) {
            LOGGER.warn("Could not initialize the IndexWriter!", e);
        }
    }

    /**
     * Removes a pdf file identified by its path from the index
     *
     * @param linkedFilePath the path to the file to be removed
     */
    public void removeFromIndex(String linkedFilePath) {
        try (IndexWriter indexWriter = new IndexWriter(
                directoryToIndex,
                new IndexWriterConfig(
                        new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            indexWriter.deleteDocuments(new Term(SearchFieldConstants.PATH, linkedFilePath));
            indexWriter.commit();
        } catch (IOException e) {
            LOGGER.warn("Could not initialize the IndexWriter!", e);
        }
    }

    /**
     * Removes a list of files linked to a bib-entry from the index
     *
     * @param entry the entry documents are linked to
     */
    public void removeFromIndex(BibEntry entry) {
        for (LinkedFile linkedFile : entry.getFiles()) {
            removeFromIndex(linkedFile.getLink());
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
        } catch (IOException e) {
            LOGGER.warn("The IndexWriter could not be initialized", e);
        }
    }

    public void addBibFieldsToIndex(BibEntry bibEntry) {
        try {
            try (IndexWriter indexWriter = new IndexWriter(directoryToIndex,
                    new IndexWriterConfig(
                            new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
                Document document = new Document();
                document.add(new StringField(SearchFieldConstants.BIB_ENTRY_ID_HASH, String.valueOf(bibEntry.getLastIndexHash()), org.apache.lucene.document.Field.Store.YES));
                for (Map.Entry<Field, String> field : bibEntry.getFieldMap().entrySet()) {
                    document.add(new TextField(field.getKey().getName(), field.getValue(), org.apache.lucene.document.Field.Store.YES));
                    SearchFieldConstants.searchableBibFields.add(field.getKey().getName());
                }
                indexWriter.addDocument(document);
                indexWriter.commit();
            }
        } catch (IOException e) {
            LOGGER.warn("Could not add an entry to the index!", e);
        }
    }

    /**
     * Writes the file to the index if the file is not yet in the index or the file on the fs is newer than the one in
     * the index.
     *
     * @param entry the entry associated with the file
     * @param linkedFile the file to write to the index
     */
    private void writeFileToIndex(BibEntry entry, LinkedFile linkedFile) {
        if (!filePreferences.shouldFulltextIndexLinkedFiles()) {
            return;
        }
        if (linkedFile.isOnlineLink() || !StandardFileType.PDF.getName().equals(linkedFile.getFileType())) {
            return;
        }
        Optional<Path> resolvedPath = linkedFile.findIn(databaseContext, filePreferences);
        if (resolvedPath.isEmpty()) {
            LOGGER.warn("Could not find {}", linkedFile.getLink());
            return;
        }
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
                        return;
                    }
                }
            } catch (IndexNotFoundException e) {
                // if there is no index yet, don't need to check anything!
            }
            // If no document was found, add the new one
            Optional<List<Document>> pages = new DocumentReader(entry, filePreferences).readLinkedPdf(this.databaseContext, linkedFile);
            if (pages.isPresent()) {
                try (IndexWriter indexWriter = new IndexWriter(directoryToIndex,
                                                               new IndexWriterConfig(
                                                                                     new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
                    indexWriter.addDocuments(pages.get());
                    indexWriter.commit();
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not add the document {} to the index!", linkedFile.getLink(), e);
        }
    }

    public void updateBibFieldsInIndex(BibEntry entry) {
        int oldHash = entry.getLastIndexHash();
        int newHash = entry.updateAndGetIndexHash();
        if (oldHash != newHash) {
            addBibFieldsToIndex(entry);
            removeFromIndex(oldHash);
        }
    }

    public void updateLinkedFilesInIndex(BibEntry entry, List<LinkedFile> removedFiles) {
        for (LinkedFile removedFile : removedFiles) {
            removeFromIndex(removedFile.getLink());
        }
        for (LinkedFile linkedFile : entry.getFiles()) {
            writeFileToIndex(entry, linkedFile);
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
                if (doc.getField(SearchFieldConstants.PATH) != null) {
                    paths.add(doc.getField(SearchFieldConstants.PATH).stringValue());
                }
            }
        } catch (IOException e) {
            return paths;
        }
        return paths;
    }

    public void deleteLinkedFilesIndex() {
        try (IndexWriter indexWriter = new IndexWriter(
                directoryToIndex,
                new IndexWriterConfig(
                        new EnglishStemAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            QueryParser queryParser = new QueryParser(SearchFieldConstants.PATH, new EnglishStemAnalyzer());
            queryParser.setAllowLeadingWildcard(true);
            indexWriter.deleteDocuments(queryParser.parse("*"));
            indexWriter.commit();
        } catch (IOException e) {
            LOGGER.warn("Could not initialize the IndexWriter!", e);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public FilePreferences getFilePreferences() {
        return filePreferences;
    }
}
