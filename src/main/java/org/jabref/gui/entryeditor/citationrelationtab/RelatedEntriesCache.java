package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;

public class RelatedEntriesCache {
    private final Map<String, List<BibEntry>> RELATED_ENTRIES_MAP = new HashMap<>();

    public List<BibEntry> lookupRelatedEntries(BibEntry entry) {
        return RELATED_ENTRIES_MAP.getOrDefault(entry.getDOI().map(DOI::getDOI).orElse(""), Collections.emptyList());
    }

    public void cacheOrMerge(BibEntry entry, List<BibEntry> relatedEntries) {
        entry.getDOI().ifPresent(doi -> RELATED_ENTRIES_MAP.putIfAbsent(doi.getDOI(), relatedEntries));
    }

    public boolean isRelatedEntriesCached(BibEntry entry) {
        return RELATED_ENTRIES_MAP.containsKey(entry.getDOI().map(DOI::getDOI).orElse(""));
    }
}
