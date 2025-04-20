package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;

import org.eclipse.jgit.util.LRUMap;

public class BibEntryRelationsCache {
    private static final Integer MAX_CACHED_ENTRIES = 100;
    private static final Map<String, List<BibEntry>> CITATIONS_MAP = new LRUMap<>(MAX_CACHED_ENTRIES, MAX_CACHED_ENTRIES);
    private static final Map<String, List<BibEntry>> REFERENCES_MAP = new LRUMap<>(MAX_CACHED_ENTRIES, MAX_CACHED_ENTRIES);

    public List<BibEntry> getCitations(BibEntry entry) {
        return CITATIONS_MAP.getOrDefault(entry.getDOI().map(DOI::asString).orElse(""), Collections.emptyList());
    }

    public List<BibEntry> getReferences(BibEntry entry) {
        return REFERENCES_MAP.getOrDefault(entry.getDOI().map(DOI::asString).orElse(""), Collections.emptyList());
    }

    public void cacheOrMergeCitations(BibEntry entry, List<BibEntry> citations) {
        entry.getDOI().ifPresent(doi -> CITATIONS_MAP.put(doi.asString(), citations));
    }

    public void cacheOrMergeReferences(BibEntry entry, List<BibEntry> references) {
        entry.getDOI().ifPresent(doi -> REFERENCES_MAP.putIfAbsent(doi.asString(), references));
    }

    public boolean citationsCached(BibEntry entry) {
        return CITATIONS_MAP.containsKey(entry.getDOI().map(DOI::asString).orElse(""));
    }

    public boolean referencesCached(BibEntry entry) {
        return REFERENCES_MAP.containsKey(entry.getDOI().map(DOI::asString).orElse(""));
    }
}
