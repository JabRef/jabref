package org.jabref.logic.search.indexing;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.search.LuceneIndexer;
import org.jabref.model.search.SearchFieldConstants;
import org.jabref.preferences.PreferencesService;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.entry.field.StandardField.FILE;
import static org.jabref.model.entry.field.StandardField.GROUPS;
import static org.jabref.model.entry.field.StandardField.KEYWORDS;

public class BibFieldsIndexer implements LuceneIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibFieldsIndexer.class);
    private final BibDatabaseContext databaseContext;
    private final TaskExecutor taskExecutor;
    private final PreferencesService preferences;
    private final String libraryName;
    private final Directory indexDirectory;
    private final IndexWriter indexWriter;
    private final SearcherManager searcherManager;
    private IndexSearcher indexSearcher;

    public BibFieldsIndexer(BibDatabaseContext databaseContext, TaskExecutor executor, PreferencesService preferences) throws IOException {
        this.databaseContext = databaseContext;
        this.taskExecutor = executor;
        this.preferences = preferences;
        this.libraryName = databaseContext.getDatabasePath().map(path -> path.getFileName().toString()).orElseGet(() -> "unsaved");

        IndexWriterConfig config = new IndexWriterConfig(SearchFieldConstants.ANALYZER);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        this.indexDirectory = new ByteBuffersDirectory();
        this.indexWriter = new IndexWriter(indexDirectory, config);
        this.searcherManager = new SearcherManager(indexWriter, null);
    }

    @Override
    public void updateOnStart() {
        addToIndex(databaseContext.getDatabase().getEntries());
    }

    @Override
    public void addToIndex(Collection<BibEntry> entries) {
        UiTaskExecutor.runInJavaFXThread(() -> {
            new BackgroundTask<>() {
                @Override
                protected Void call() {
                    int i = 1;
                    LOGGER.debug("Adding {} entries to index", entries.size());
                    for (BibEntry entry : entries) {
                        if (isCanceled()) {
                            updateMessage(Localization.lang("Indexing canceled: %0 of %1 entries added to the index.", i, entries.size()));
                            break;
                        }
                        addToIndex(entry);
                        updateProgress(i, entries.size());
                        updateMessage(Localization.lang("%0 of %1 entries added to the index.", i, entries.size()));
                        i++;
                    }
                    updateMessage(Localization.lang("Indexing completed: %0 entries added to the index.", entries.size()));
                    return null;
                }
            }.showToUser(entries.size() > 1)
             .setTitle(Localization.lang("Indexing bib fields for %0", libraryName))
             .executeWith(taskExecutor);
        });
    }

    private void addToIndex(BibEntry bibEntry) {
        try {
            Document document = new Document();
            org.apache.lucene.document.Field.Store store = org.apache.lucene.document.Field.Store.YES;

            document.add(new StringField(SearchFieldConstants.BIB_ENTRY_ID, bibEntry.getId(), store));
            document.add(new StringField(SearchFieldConstants.BIB_ENTRY_TYPE, bibEntry.getType().getName(), store));
            document.add(new TextField(SearchFieldConstants.DEFAULT_FIELD, bibEntry.getParsedSerialization(), store));

            for (Map.Entry<Field, String> field : bibEntry.getFieldMap().entrySet()) {
                String fieldValue = field.getValue();
                String fieldName = field.getKey().getName();
                SearchFieldConstants.SEARCHABLE_BIB_FIELDS.add(fieldName);

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

    @Override
    public void removeFromIndex(Collection<BibEntry> entries) {
        entries.forEach(this::removeFromIndex);
    }

    private void removeFromIndex(BibEntry entry) {
        try {
            LOGGER.debug("Removing entry {} from index", entry.getId());
            indexWriter.deleteDocuments((new Term(SearchFieldConstants.BIB_ENTRY_ID, entry.getId())));
            LOGGER.debug("Entry {} removed from index", entry.getId());
        } catch (IOException e) {
            LOGGER.error("Error deleting entry from index", e);
        }
    }

    @Override
    public void updateEntry(BibEntry entry, String oldValue, String newValue) {
        LOGGER.debug("Updating entry {} in index", entry.getId());
        removeFromIndex(List.of(entry));
        addToIndex(List.of(entry));
    }

    @Override
    public void removeAllFromIndex() {
        try {
            LOGGER.debug("Removing all bib fields from index");
            indexWriter.deleteAll();
            LOGGER.debug("All bib fields removed from index");
        } catch (IOException e) {
            LOGGER.error("Error deleting all linked files from index", e);
        }
    }

    @Override
    public void rebuildIndex() {
        removeAllFromIndex();
        addToIndex(databaseContext.getDatabase().getEntries());
    }

    @Override
    public IndexSearcher getIndexSearcher() {
        LOGGER.debug("Getting index searcher");
        try {
            if (indexSearcher != null) {
                LOGGER.debug("Releasing index searcher");
                searcherManager.release(indexSearcher);
            }
            LOGGER.debug("Refreshing searcher");
            searcherManager.maybeRefresh();
            LOGGER.debug("Acquiring index searcher");
            indexSearcher = searcherManager.acquire();
        } catch (IOException e) {
            LOGGER.error("Error refreshing searcher", e);
        }
        return indexSearcher;
    }

    @Override
    public void close() {
        LOGGER.debug("Closing index");
        HeadlessExecutorService.INSTANCE.execute(() -> {
            try {
                searcherManager.close();
                indexWriter.close();
                indexDirectory.close();
                LOGGER.debug("Index closed");
            } catch (IOException e) {
                LOGGER.error("Error closing index", e);
            }
        });
    }
}
