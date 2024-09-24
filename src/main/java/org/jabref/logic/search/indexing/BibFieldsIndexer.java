package org.jabref.logic.search.indexing;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.LuceneIndexer;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.SearchFieldConstants;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibFieldsIndexer implements LuceneIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibFieldsIndexer.class);
    private final BibDatabaseContext databaseContext;
    private final String libraryName;
    private final Directory indexDirectory;
    private IndexWriter indexWriter;
    private SearcherManager searcherManager;

    public BibFieldsIndexer(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
        this.libraryName = databaseContext.getDatabasePath().map(path -> path.getFileName().toString()).orElse("unsaved");

        IndexWriterConfig config = new IndexWriterConfig(SearchFieldConstants.LATEX_AWARE_NGRAM_ANALYZER);

        this.indexDirectory = new ByteBuffersDirectory();
        try {
            this.indexWriter = new IndexWriter(indexDirectory, config);
            this.searcherManager = new SearcherManager(indexWriter, null);
        } catch (IOException e) {
            LOGGER.error("Error initializing bib fields index", e);
        }
    }

    @Override
    public void updateOnStart(BackgroundTask<?> task) {
        addToIndex(databaseContext.getDatabase().getEntries(), task);
    }

    @Override
    public void addToIndex(Collection<BibEntry> entries, BackgroundTask<?> task) {
        if (entries.size() > 1) {
            task.showToUser(true);
            task.setTitle(Localization.lang("Indexing bib fields for %0", libraryName));
        }
        int i = 1;
        long startTime = System.currentTimeMillis();
        LOGGER.debug("Adding {} entries to index", entries.size());
        for (BibEntry entry : entries) {
            if (task.isCancelled()) {
                LOGGER.debug("Indexing canceled");
                return;
            }
            addToIndex(entry);
            task.updateProgress(i, entries.size());
            task.updateMessage(Localization.lang("%0 of %1 entries added to the index.", i, entries.size()));
            i++;
        }
        LOGGER.debug("Added {} entries to index in {} ms", entries.size(), System.currentTimeMillis() - startTime);
    }

    private void addToIndex(BibEntry bibEntry) {
        try {
            Document document = new Document();
            org.apache.lucene.document.Field.Store storeEnabled = org.apache.lucene.document.Field.Store.YES;
            org.apache.lucene.document.Field.Store storeDisabled = org.apache.lucene.document.Field.Store.NO;
            document.add(new StringField(SearchFieldConstants.ENTRY_ID.toString(), bibEntry.getId(), storeEnabled));
            document.add(new TextField(SearchFieldConstants.ENTRY_TYPE.toString(), bibEntry.getType().getName(), storeDisabled));

            StringBuilder allFields = new StringBuilder(bibEntry.getType().getName());
            for (Map.Entry<Field, String> mapEntry : bibEntry.getFieldMap().entrySet()) {
                document.add(new TextField(mapEntry.getKey().getName(), mapEntry.getValue(), storeDisabled));
                if (mapEntry.getKey().equals(StandardField.GROUPS)) {
                    // Do not add groups to the allFields field: https://github.com/JabRef/jabref/issues/7996
                    continue;
                }
                allFields.append('\n').append(mapEntry.getValue());
            }
            document.add(new TextField(SearchFieldConstants.DEFAULT_FIELD.toString(), allFields.toString(), storeDisabled));
            indexWriter.addDocument(document);
        } catch (IOException e) {
            LOGGER.warn("Could not add an entry to the index.", e);
        }
    }

    @Override
    public void removeFromIndex(Collection<BibEntry> entries, BackgroundTask<?> task) {
        if (entries.size() > 1) {
            task.showToUser(true);
            task.setTitle(Localization.lang("Removing entries from index for %0", libraryName));
        }
        int i = 1;
        for (BibEntry entry : entries) {
            if (task.isCancelled()) {
                LOGGER.debug("Removing entries canceled");
                return;
            }
            removeFromIndex(entry);
            task.updateProgress(i, entries.size());
            task.updateMessage(Localization.lang("%0 of %1 entries removed from the index.", i, entries.size()));
            i++;
        }
    }

    private void removeFromIndex(BibEntry entry) {
        try {
            indexWriter.deleteDocuments((new Term(SearchFieldConstants.ENTRY_ID.toString(), entry.getId())));
            LOGGER.debug("Entry {} removed from index", entry.getId());
        } catch (IOException e) {
            LOGGER.error("Error deleting entry from index", e);
        }
    }

    @Override
    public void updateEntry(BibEntry entry, String oldValue, String newValue, BackgroundTask<?> task) {
        LOGGER.debug("Updating entry {} in index", entry.getId());
        removeFromIndex(entry);
        addToIndex(entry);
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
    public void rebuildIndex(BackgroundTask<?> task) {
        removeAllFromIndex();
        addToIndex(databaseContext.getDatabase().getEntries(), task);
    }

    @Override
    public SearcherManager getSearcherManager() {
        return searcherManager;
    }

    @Override
    public void close() {
        HeadlessExecutorService.INSTANCE.execute(this::closeIndex);
    }

    @Override
    public void closeAndWait() {
        HeadlessExecutorService.INSTANCE.executeAndWait(this::closeIndex);
    }

    private void closeIndex() {
        try {
            LOGGER.debug("Closing bib fields index");
            searcherManager.close();
            indexWriter.close();
            indexDirectory.close();
            LOGGER.debug("Bib fields index closed");
        } catch (IOException e) {
            LOGGER.error("Error while closing bib fields index", e);
        }
    }
}
