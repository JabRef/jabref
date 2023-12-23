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

    public List<BibEntry> lookupRelatedEntries(BibEntry entry) {
        if (isRelatedEntriesNotCached(entry)) {
            refreshCache(entry);
        }

        return cache.lookupRelatedEntries(entry);
    }

    public boolean isRelatedEntriesNotCached(BibEntry entry) {
        return !cache.isRelatedEntriesCached(entry);
    }

    /**
     * Fetches entries related to the given {@code entry} and cache the result for faster access.
     * */
    public void refreshCache(BibEntry entry) {
        List<BibEntry> relatedEntries = fetcher.fetch(entry);
        cache.updateCache(entry, relatedEntries);
    }
}
