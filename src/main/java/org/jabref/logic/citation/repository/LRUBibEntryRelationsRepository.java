package org.jabref.logic.citation.repository;

import java.util.List;

import org.jabref.logic.importer.fetcher.CitationFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LRUBibEntryRelationsRepository implements BibEntryRelationsRepository {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(LRUBibEntryRelationsRepository.class);

    private final CitationFetcher fetcher;
    private final LRUBibEntryRelationsCache cache;

    public LRUBibEntryRelationsRepository(CitationFetcher fetcher, LRUBibEntryRelationsCache cache) {
        this.fetcher = fetcher;
        this.cache = cache;
    }

    @Override
    public List<BibEntry> readCitations(BibEntry entry) {
        if (needToRefreshCitations(entry)) {
            forceRefreshCitations(entry);
        }

        return cache.getCitations(entry);
    }

    @Override
    public List<BibEntry> readReferences(BibEntry entry) {
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

    @Override
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

    @Override
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
