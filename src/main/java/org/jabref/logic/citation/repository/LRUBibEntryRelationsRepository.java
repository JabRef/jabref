package org.jabref.logic.citation.repository;

import java.util.List;
import java.util.Objects;

import org.jabref.model.entry.BibEntry;

public class LRUBibEntryRelationsRepository implements BibEntryRelationsRepository {

    private final LRUBibEntryRelationsCache cache;

    public LRUBibEntryRelationsRepository(LRUBibEntryRelationsCache cache) {
        this.cache = cache;
    }

    @Override
    public void insertCitations(BibEntry entry, List<BibEntry> citations) {
        cache.cacheOrMergeCitations(
            entry, Objects.requireNonNullElseGet(citations, List::of)
        );
    }

    @Override
    public List<BibEntry> readCitations(BibEntry entry) {
        return cache.getCitations(entry);
    }

    @Override
    public boolean containsCitations(BibEntry entry) {
        return cache.citationsCached(entry);
    }

    @Override
    public void insertReferences(BibEntry entry, List<BibEntry> references) {
        cache.cacheOrMergeReferences(
            entry, Objects.requireNonNullElseGet(references, List::of)
        );
    }

    @Override
    public List<BibEntry> readReferences(BibEntry entry) {
        return cache.getReferences(entry);
    }

    @Override
    public boolean containsReferences(BibEntry entry) {
        return cache.referencesCached(entry);
    }
}
