package org.jabref.logic.citation.repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;

import org.eclipse.jgit.util.LRUMap;

import static org.jabref.logic.citation.repository.LRUCacheBibEntryRelationsDAO.Configuration.MAX_CACHED_ENTRIES;

public enum LRUCacheBibEntryRelationsDAO implements BibEntryRelationDAO {

    CITATIONS(new LRUMap<>(MAX_CACHED_ENTRIES, MAX_CACHED_ENTRIES)),
    REFERENCES(new LRUMap<>(MAX_CACHED_ENTRIES, MAX_CACHED_ENTRIES));

    public static class Configuration {
        public static final int MAX_CACHED_ENTRIES = 100;
    }

    private final Map<DOI, Set<BibEntry>> relationsMap;

    LRUCacheBibEntryRelationsDAO(Map<DOI, Set<BibEntry>> relationsMap) {
        this.relationsMap = relationsMap;
    }

    @Override
    public List<BibEntry> getRelations(BibEntry entry) {
        return entry
            .getDOI()
            .stream()
            .flatMap(doi -> this.relationsMap.getOrDefault(doi, Set.of()).stream())
            .toList();
    }

    @Override
    public synchronized void cacheOrMergeRelations(BibEntry entry, List<BibEntry> relations) {
        entry.getDOI().ifPresent(doi -> {
            var cachedRelations = this.relationsMap.getOrDefault(doi, new LinkedHashSet<>());
            cachedRelations.addAll(relations);
            relationsMap.put(doi, cachedRelations);
        });
    }

    @Override
    public boolean containsKey(BibEntry entry) {
        return entry.getDOI().map(this.relationsMap::containsKey).orElse(false);
    }

    public void clearEntries() {
        this.relationsMap.clear();
    }
}
