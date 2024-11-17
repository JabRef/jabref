package org.jabref.logic.citation.repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;

import org.eclipse.jgit.util.LRUMap;

public abstract class LRUCacheBibEntryRelationsDAO implements BibEntryRelationDAO {

    private static final Integer MAX_CACHED_ENTRIES = 100;

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

    public static class LRUCacheBibEntryCitations extends LRUCacheBibEntryRelationsDAO {

        private final static LRUCacheBibEntryCitations CITATIONS_CACHE = new LRUCacheBibEntryCitations();

        private LRUCacheBibEntryCitations() {
            super(new LRUMap<>(MAX_CACHED_ENTRIES, MAX_CACHED_ENTRIES));
        }

        public static LRUCacheBibEntryCitations getInstance() {
            return CITATIONS_CACHE;
        }
    }

    public static class LRUCacheBibEntryReferences extends LRUCacheBibEntryRelationsDAO {

        private final static LRUCacheBibEntryReferences REFERENCES_CACHE = new LRUCacheBibEntryReferences();

        private LRUCacheBibEntryReferences() {
            super(new LRUMap<>(MAX_CACHED_ENTRIES, MAX_CACHED_ENTRIES));
        }

        public static LRUCacheBibEntryReferences getInstance() {
            return REFERENCES_CACHE;
        }
    }
}
