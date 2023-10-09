package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.List;

import org.jabref.gui.entryeditor.citationrelationtab.semanticscholar.RelatedEntriesFetcher;
import org.jabref.model.entry.BibEntry;

public class RelatedEntriesRepository {
    private final RelatedEntriesFetcher fetcher;
    private final RelatedEntriesCache cache;

    public RelatedEntriesRepository(RelatedEntriesFetcher fetcher, RelatedEntriesCache cache) {
        this.fetcher = fetcher;
        this.cache = cache;
    }

    public RelatedEntriesRepository(RelatedEntriesFetcher fetcher) {
        this(fetcher, new RelatedEntriesCache());
    }

    public List<BibEntry> lookupRelatedEntries(BibEntry entry) {
        if (needToRefreshCache(entry)) {
            refreshCache(entry);
        }

        return cache.lookupRelatedEntries(entry);
    }

    public boolean needToRefreshCache(BibEntry entry) {
        return !cache.isRelatedEntriesCached(entry);
    }

    public void refreshCache(BibEntry entry) {
        List<BibEntry> relatedEntries = fetcher.fetch(entry);
        cache.cacheOrMerge(entry, relatedEntries);
    }
}
