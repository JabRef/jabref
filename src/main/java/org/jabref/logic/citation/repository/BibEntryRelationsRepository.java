package org.jabref.logic.citation.repository;

import java.util.List;

import org.jabref.logic.importer.fetcher.CitationFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibEntryRelationsRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibEntryRelationsRepository.class);

    private final CitationFetcher fetcher;
    private final BibEntryRelationsCache cache;

    public BibEntryRelationsRepository(CitationFetcher fetcher, BibEntryRelationsCache cache) {
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
            List<BibEntry> references;
            try {
                references = fetcher.searchCiting(entry);
            } catch (FetcherException e) {
                LOGGER.error("Error while fetching references", e);
                references = List.of();
            }
            cache.cacheOrMergeReferences(entry, references);
        }

        return cache.getReferences(entry);
    }

    public void forceRefreshCitations(BibEntry entry) {
        try {
            List<BibEntry> citations = fetcher.searchCitedBy(entry);
            cache.cacheOrMergeCitations(entry, citations);
        } catch (FetcherException e) {
            LOGGER.error("Error while fetching citations", e);
        }
    }

    private boolean needToRefreshCitations(BibEntry entry) {
        return !cache.citationsCached(entry);
    }

    private boolean needToRefreshReferences(BibEntry entry) {
        return !cache.referencesCached(entry);
    }

    public void forceRefreshReferences(BibEntry entry) {
        List<BibEntry> references;
        try {
            references = fetcher.searchCiting(entry);
        } catch (FetcherException e) {
            LOGGER.error("Error while fetching references", e);
            references = List.of();
        }
        cache.cacheOrMergeReferences(entry, references);
    }
}
