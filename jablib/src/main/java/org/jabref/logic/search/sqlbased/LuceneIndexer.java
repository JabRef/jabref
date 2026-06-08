package org.jabref.logic.search.sqlbased;

import java.util.Collection;

import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.entry.BibEntry;

import org.apache.lucene.search.SearcherManager;
import org.jspecify.annotations.Nullable;

public interface LuceneIndexer {
    void updateOnStart(BackgroundTask<?> task);

    void addToIndex(Collection<BibEntry> entries, BackgroundTask<?> task);

    void removeFromIndex(Collection<BibEntry> entries, BackgroundTask<?> task);

    void updateEntry(BibEntry entry, @Nullable String oldValue, @Nullable String newValue, BackgroundTask<?> task);

    void removeAllFromIndex();

    void rebuildIndex(BackgroundTask<?> task);

    SearcherManager getSearcherManager();

    void close();

    void closeAndWait();
}
