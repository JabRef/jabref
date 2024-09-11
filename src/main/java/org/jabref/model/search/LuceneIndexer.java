package org.jabref.model.search;

import java.util.Collection;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.model.entry.BibEntry;

import org.apache.lucene.search.SearcherManager;

public interface LuceneIndexer {
    void updateOnStart(BackgroundTask<?> task);

    void addToIndex(Collection<BibEntry> entries, BackgroundTask<?> task);

    void removeFromIndex(Collection<BibEntry> entries, BackgroundTask<?> task);

    void updateEntry(BibEntry entry, String oldValue, String newValue, BackgroundTask<?> task);

    void removeAllFromIndex();

    void rebuildIndex(BackgroundTask<?> task);

    SearcherManager getSearcherManager();

    void close();
}
