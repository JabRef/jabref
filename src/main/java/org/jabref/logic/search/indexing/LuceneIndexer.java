package org.jabref.logic.search.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.util.Pair;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.search.SearchFieldConstants;
import org.jabref.preferences.PreferencesService;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.entry.field.StandardField.FILE;
import static org.jabref.model.entry.field.StandardField.GROUPS;
import static org.jabref.model.entry.field.StandardField.KEYWORDS;

public class LuceneIndexer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneIndexer.class);
    private static final Analyzer ANALYZER = new StandardAnalyzer();

    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferences;
    private final TaskExecutor taskExecutor;
    private final DocumentReader documentReader;
    private final String databaseName;
    private final BooleanProperty shouldIndexLinkedFiles;
    private final Directory directoryToIndex;
    private DirectoryReader reader;
    private IndexWriter indexWriter;

    public LuceneIndexer(BibDatabaseContext databaseContext, TaskExecutor taskExecutor, PreferencesService preferences) {
        this.databaseContext = databaseContext;
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;
        this.documentReader = new DocumentReader(preferences.getFilePreferences());
        this.databaseName = databaseContext.getDatabasePath().map(path -> path.getFileName().toString()).orElse("");
        this.shouldIndexLinkedFiles = preferences.getFilePreferences().fulltextIndexLinkedFilesProperty();

        Directory tmpDirectory = null;
        try {
            tmpDirectory = new NIOFSDirectory(databaseContext.getFulltextIndexPath());
        } catch (IOException e) {
            LOGGER.error("Could not initialize the index directory.", e);
        }
        this.directoryToIndex = tmpDirectory;

        bindToPreferences();
    }

    public void initializeIndexWriterAndReader(IndexWriterConfig.OpenMode openMode) {
        if (directoryToIndex == null) {
            LOGGER.info("Index directory must not be null.");
            return;
        }

        try {
            IndexWriterConfig config = new IndexWriterConfig(ANALYZER);
            config.setOpenMode(openMode);
            indexWriter = new IndexWriter(directoryToIndex, config);
        } catch (IOException e) {
            LOGGER.error("Could not initialize the IndexWriter.", e);
            return;
        }
        try {
            reader = DirectoryReader.open(indexWriter, true, false);
        } catch (IOException e) {
            LOGGER.error("Could not initialize the IndexReader.", e);
        }
    }

    public void close() {
        try {
            if (indexWriter != null) {
                indexWriter.close();
            }
        } catch (IOException e) {
            LOGGER.warn("Could not close the index writer.", e);
        }
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            LOGGER.warn("Could not close the index reader.", e);
        }
    }

    public void commit() {
        try {
            if (indexWriter != null) {
                indexWriter.commit();
            }
        } catch (IOException e) {
            LOGGER.warn("Could not commit changes to the index.", e);
        }
    }

    public void flushIndex() {
        close();
        initializeIndexWriterAndReader(IndexWriterConfig.OpenMode.CREATE);
    }

    private void bindToPreferences() {
        shouldIndexLinkedFiles.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                indexLinkedFiles(getLinkedFilesFromEntries(databaseContext.getEntries()));
            } else {
                deleteAllLinkedFilesFromIndex();
            }
        });
    }

    public void rebuildIndex() {
        flushIndex();
        indexEntries(databaseContext.getEntries());
    }

    public void indexEntries(Collection<BibEntry> entries) {
        indexBibFields(entries);

        if (shouldIndexLinkedFiles.get()) {
            indexLinkedFiles(getLinkedFilesFromEntries(entries));
        }
    }

    public void updateIndex() {
        Set<String> indexedHashes = getIndexedHashes();

        Set<String> currentHashes = getHashes(databaseContext.getEntries(), true);

        Pair<Set<String>, Set<String>> diffHashes = calculateDifferences(indexedHashes, currentHashes);
        Set<String> hashesToAdd = diffHashes.getKey();
        Set<String> hashesToRemove = diffHashes.getValue();

        removeBibFieldsByHash(hashesToRemove);

        indexBibFields(databaseContext.getEntries()
                                      .stream()
                                      .filter(entry -> hashesToAdd.contains(String.valueOf(entry.getLastIndexHash())))
                                      .toList());

        if (shouldIndexLinkedFiles.get()) {
            Set<String> indexedFiles = getIndexedFiles().keySet();

            Set<LinkedFile> currentFiles = getLinkedFilesFromEntries(databaseContext.getEntries());
            Set<String> currentFilesLinks = currentFiles.stream()
                                                        .map(LinkedFile::getLink)
                                                        .collect(Collectors.toSet());

            Pair<Set<String>, Set<String>> diffFiles = calculateDifferences(indexedFiles, currentFilesLinks);
            Set<String> filesToAdd = diffFiles.getKey();
            Set<String> filesToRemove = diffFiles.getValue();

            removeLinkedFilesByLink(filesToRemove);

            indexLinkedFiles(currentFiles.stream()
                                         .filter(linkedFile -> filesToAdd.contains(linkedFile.getLink()))
                                         .collect(Collectors.toSet()));
        } else {
            deleteAllLinkedFilesFromIndex();
        }
    }

    public void indexBibFields(Collection<BibEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        DefaultTaskExecutor.runInJavaFXThread(() ->
                indexBibFieldsTask(entries)
                        .showToUser(true)
                        .setTitle(Localization.lang("Indexing bib fields for %0", databaseName))
                        .executeWith(taskExecutor));
    }

    private BackgroundTask<Void> indexBibFieldsTask(Collection<BibEntry> entries) {
        return new BackgroundTask<>() {
            @Override
            protected Void call() {
                updateProgress(0, entries.size());
                updateMessage(Localization.lang("%0 of %1 entries added to the index", 0, entries.size()));

                int i = 1;
                for (BibEntry entry : entries) {
                    indexBibFields(entry);
                    updateProgress(i, entries.size());
                    updateMessage(Localization.lang("%0 of %1 entries added to the index", i, entries.size()));
                    i++;
                }
                commit();
                return null;
            }
        };
    }

    private void indexBibFields(BibEntry bibEntry) {
        try {
            Document document = new Document();
            org.apache.lucene.document.Field.Store store = org.apache.lucene.document.Field.Store.YES;

            document.add(new StringField(SearchFieldConstants.BIB_ENTRY_ID_HASH, String.valueOf(bibEntry.updateAndGetIndexHash()), store));

            for (Map.Entry<Field, String> field : bibEntry.getFieldMap().entrySet()) {
                String fieldValue = field.getValue();
                String fieldName = field.getKey().getName();
                SearchFieldConstants.searchableBibFields.add(fieldName);

                switch (field.getKey()) {
                    case KEYWORDS ->
                            KeywordList.parse(fieldValue, preferences.getBibEntryPreferences().getKeywordSeparator())
                                       .forEach(keyword -> document.add(new StringField(fieldName, keyword.toString(), store)));
                    case GROUPS ->
                            Arrays.stream(fieldValue.split(preferences.getBibEntryPreferences().getKeywordSeparator().toString()))
                                  .forEach(group -> document.add(new StringField(fieldName, group, store)));
                    case FILE ->
                            FileFieldParser.parse(fieldValue).stream()
                                           .map(LinkedFile::getLink)
                                           .forEach(link -> document.add(new StringField(fieldName, link, store)));
                    default ->
                            document.add(new TextField(fieldName, fieldValue, store));
                }
            }
            indexWriter.addDocument(document);
        } catch (IOException e) {
            LOGGER.warn("Could not add an entry to the index.", e);
        }
    }

    public void indexLinkedFiles(Collection<LinkedFile> linkedFiles) {
        if (!shouldIndexLinkedFiles.get() || linkedFiles.isEmpty()) {
            return;
        }
        DefaultTaskExecutor.runInJavaFXThread(() ->
                indexLinkedFilesTask(linkedFiles)
                        .willBeRecoveredAutomatically(true)
                        .showToUser(true)
                        .setTitle(Localization.lang("Indexing pdf files for %0", databaseName))
                        .executeWith(taskExecutor));
    }

    private BackgroundTask<Void> indexLinkedFilesTask(Collection<LinkedFile> linkedFiles) {
        return new BackgroundTask<>() {
            @Override
            protected Void call() {
                updateProgress(0, linkedFiles.size());
                updateMessage(Localization.lang("%0 of %1 files added to the index", 0, linkedFiles.size()));

                int i = 1;
                Map<String, Long> indexedFiles = getIndexedFiles();

                for (LinkedFile linkedFile : linkedFiles) {
                    Optional<Path> resolvedPath = linkedFile.findIn(databaseContext, preferences.getFilePreferences());
                    if (resolvedPath.isEmpty()) {
                        LOGGER.debug("Could not find {}", linkedFile.getLink());
                    } else {
                        if (indexedFiles.containsKey(linkedFile.getLink())) {
                            long indexModificationTime = indexedFiles.get(linkedFile.getLink());
                            try {
                                BasicFileAttributes attributes = Files.readAttributes(resolvedPath.get(), BasicFileAttributes.class);
                                if (attributes.lastModifiedTime().to(TimeUnit.SECONDS) > indexModificationTime) {
                                    removeLinkedFileByLink(linkedFile.getLink());
                                    indexLinkedFile(linkedFile, resolvedPath.get());
                                }
                            } catch (IOException e) {
                                LOGGER.warn("Could not check the modification time of the file {}.", linkedFile.getLink(), e);
                            }
                        } else {
                            indexLinkedFile(linkedFile, resolvedPath.get());
                        }
                    }
                    updateProgress(i, linkedFiles.size());
                    updateMessage(Localization.lang("%0 of %1 files added to the index", i, linkedFiles.size()));
                    i++;
                }
                commit();
                return null;
            }
        };
    }

    private void indexLinkedFile(LinkedFile linkedFile, Path resolvedPath) {
        List<Document> pages = documentReader.readPdfContents(linkedFile, resolvedPath);
        try {
            indexWriter.addDocuments(pages);
        } catch (IOException e) {
            LOGGER.warn("Could not add the document {} to the index.", linkedFile.getLink(), e);
        }
    }

    public void removeEntries(Collection<BibEntry> entries) {
        removeBibFieldsFromIndex(entries);
        removeLinkedFilesByLink(getLinkedFilesFromEntries(entries).stream()
                                                                  .map(LinkedFile::getLink)
                                                                  .collect(Collectors.toSet()));
    }

    public void removeBibFieldsFromIndex(Collection<BibEntry> entries) {
        removeBibFieldsByHash(getHashes(entries, false));
    }

    private void removeBibFieldsByHash(Collection<String> hashes) {
        BackgroundTask.wrap(() -> {
            hashes.forEach(this::removeBibFieldsByHash);
            commit();
        }).executeWith(taskExecutor);
    }

    private void removeBibFieldsByHash(String hash) {
        try {
            indexWriter.deleteDocuments(new Term(SearchFieldConstants.BIB_ENTRY_ID_HASH, hash));
        } catch (IOException e) {
            LOGGER.warn("Could not remove the entry from the index.", e);
        }
    }

    public void removeLinkedFilesByLink(Collection<String> linkedFiles) {
        BackgroundTask.wrap(() -> {
            linkedFiles.forEach(this::removeLinkedFileByLink);
            commit();
        }).executeWith(taskExecutor);
    }

    private void removeLinkedFileByLink(String linkedFile) {
        try {
            indexWriter.deleteDocuments(new Term(SearchFieldConstants.PATH, linkedFile));
        } catch (IOException e) {
            LOGGER.warn("Could not remove the linked file {} from the index.", linkedFile, e);
        }
    }

    private void deleteAllLinkedFilesFromIndex() {
        BackgroundTask.wrap(() -> {
            QueryParser queryParser = new QueryParser(SearchFieldConstants.PATH, ANALYZER);
            queryParser.setAllowLeadingWildcard(true);

            try {
                indexWriter.deleteDocuments(queryParser.parse("*"));
                commit();
            } catch (ParseException | IOException e) {
                LOGGER.warn("Could not delete linked files from the index.", e);
            }
        }).executeWith(taskExecutor);
    }

    private Set<String> getIndexedHashes() {
        Set<String> hashes = new HashSet<>();
        for (LeafReaderContext leaf : reader.leaves()) {
            try {
                Terms terms = leaf.reader().terms(SearchFieldConstants.BIB_ENTRY_ID_HASH);
                if (terms != null) {
                    TermsEnum termsEnum = terms.iterator();
                    BytesRef term;
                    while ((term = termsEnum.next()) != null) {
                        hashes.add(term.utf8ToString());
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Could not retrieve the indexed entries.", e);
            }
        }
        return hashes;
    }

    private Map<String, Long> getIndexedFiles() {
        Map<String, Long> paths = new HashMap<>();
        try {
            TermQuery query = new TermQuery(new Term(SearchFieldConstants.PAGE_NUMBER, "1"));
            TopDocs allDocs = new IndexSearcher(reader).search(query, Integer.MAX_VALUE);
            StoredFields storedFields = reader.storedFields();
            for (ScoreDoc scoreDoc : allDocs.scoreDocs) {
                Document doc = storedFields.document(scoreDoc.doc);
                var pathField = doc.getField(SearchFieldConstants.PATH);
                var modifiedField = doc.getField(SearchFieldConstants.MODIFIED);
                if (pathField != null && modifiedField != null) {
                    paths.put(pathField.stringValue(), Long.valueOf(modifiedField.stringValue()));
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not retrieve the indexed files.", e);
        }
        return paths;
    }

    private static Set<String> getHashes(Collection<BibEntry> entries, boolean update) {
        return entries.stream()
                      .map(update ? BibEntry::updateAndGetIndexHash : BibEntry::getLastIndexHash)
                      .map(String::valueOf)
                      .collect(Collectors.toSet());
    }

    private static Set<LinkedFile> getLinkedFilesFromEntries(Collection<BibEntry> entries) {
        return entries.stream()
                              .flatMap(entry -> entry.getFiles().stream())
                              .filter(linkedFile -> !linkedFile.isOnlineLink() && StandardFileType.PDF.getName().equals(linkedFile.getFileType()))
                              .collect(Collectors.toSet());
    }

    private static Pair<Set<String>, Set<String>> calculateDifferences(Set<String> indexedSet, Set<String> currentSet) {
        Set<String> toAdd = currentSet.stream()
                                      .filter(element -> !indexedSet.contains(element))
                                      .collect(Collectors.toSet());

        Set<String> toRemove = indexedSet.stream()
                                         .filter(element -> !currentSet.contains(element))
                                         .collect(Collectors.toSet());

        return new Pair<>(toAdd, toRemove);
    }
}
