package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.List;

import org.jabref.gui.entryeditor.citationrelationtab.semanticscholar.SemanticScholarFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;

public class BibEntryRelationsRepository {
    private final SemanticScholarFetcher fetcher;
    private final BibEntryRelationsCache cache;

    public BibEntryRelationsRepository(SemanticScholarFetcher fetcher, BibEntryRelationsCache cache) {
        this.fetcher = fetcher;
        this.cache = cache;
    }

    public List<BibEntry> getCitations(BibEntry entry) {
        if (needToRefreshCitations(entry)) {
            forceRefreshCitations(entry);
        }

        return cache.getCitations(entry);
    }

    public List<BibEntry> getReferences(BibEntry entry) {
        if (needToRefreshReferences(entry)) {
            List<BibEntry> references = fetcher.searchCiting(entry);
            cache.cacheOrMergeReferences(entry, references);
        }

        return cache.getReferences(entry);
    }

    public void forceRefreshCitations(BibEntry entry) {
        try {
            List<BibEntry> citations = fetcher.searchCitedBy(entry);
            cache.cacheOrMergeCitations(entry, citations);
        } catch (FetcherException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean needToRefreshCitations(BibEntry entry) {
        return !cache.citationsCached(entry);
    }

    public boolean needToRefreshReferences(BibEntry entry) {
        return !cache.referencesCached(entry);
    }

    public void forceRefreshReferences(BibEntry entry) {
        List<BibEntry> references = fetcher.searchCiting(entry);
        cache.cacheOrMergeReferences(entry, references);
    }
}
