package org.jabref.logic.citation.repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;

import org.eclipse.jgit.util.LRUMap;

public class LRUBibEntryRelationsCache {
    private static final Integer MAX_CACHED_ENTRIES = 100;
    private static final Map<DOI, Set<BibEntry>> CITATIONS_MAP = new LRUMap<>(MAX_CACHED_ENTRIES, MAX_CACHED_ENTRIES);
    private static final Map<DOI, Set<BibEntry>> REFERENCES_MAP = new LRUMap<>(MAX_CACHED_ENTRIES, MAX_CACHED_ENTRIES);

    public List<BibEntry> getCitations(BibEntry entry) {
        return entry
            .getDOI()
            .stream()
            .flatMap(doi -> CITATIONS_MAP.getOrDefault(doi, Set.of()).stream())
            .toList();
    }

    public List<BibEntry> getReferences(BibEntry entry) {
        return entry
            .getDOI()
            .stream()
            .flatMap(doi -> REFERENCES_MAP.getOrDefault(doi, Set.of()).stream())
            .toList();
    }

    public void cacheOrMergeCitations(BibEntry entry, List<BibEntry> citations) {
        entry.getDOI().ifPresent(doi -> {
            var cachedRelations = CITATIONS_MAP.getOrDefault(doi, new LinkedHashSet<>());
            cachedRelations.addAll(citations);
            CITATIONS_MAP.put(doi, cachedRelations);
        });
    }

    public void cacheOrMergeReferences(BibEntry entry, List<BibEntry> references) {
        entry.getDOI().ifPresent(doi -> {
            var cachedRelations = REFERENCES_MAP.getOrDefault(doi, new LinkedHashSet<>());
            cachedRelations.addAll(references);
            REFERENCES_MAP.put(doi, cachedRelations);
        });
    }

    public boolean citationsCached(BibEntry entry) {
        return entry.getDOI().map(CITATIONS_MAP::containsKey).orElse(false);
    }

    public boolean referencesCached(BibEntry entry) {
        return entry.getDOI().map(REFERENCES_MAP::containsKey).orElse(false);
    }
}
