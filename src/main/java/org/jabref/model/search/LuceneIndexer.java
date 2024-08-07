package org.jabref.model.search;

import java.util.Collection;

import org.jabref.model.entry.BibEntry;

import org.apache.lucene.search.IndexSearcher;

public interface LuceneIndexer {
    void updateOnStart();

    void addToIndex(Collection<BibEntry> entries);

    void removeFromIndex(Collection<BibEntry> entries);

    void updateEntry(BibEntry entry, String oldValue, String newValue);

    void removeAllFromIndex();

    void rebuildIndex();

    IndexSearcher getIndexSearcher();

    void close();
}
