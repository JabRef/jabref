package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;

public class BibEntryRelationsCache {
    private static final Map<String, List<BibEntry>> CITATIONS_MAP = new HashMap<>();
    private static final Map<String, List<BibEntry>> REFERENCES_MAP = new HashMap<>();

    public List<BibEntry> getCitations(BibEntry entry) {
        return CITATIONS_MAP.getOrDefault(entry.getDOI().map(DOI::getDOI).orElse(""), Collections.emptyList());
    }

    public List<BibEntry> getReferences(BibEntry entry) {
        return REFERENCES_MAP.getOrDefault(entry.getDOI().map(DOI::getDOI).orElse(""), Collections.emptyList());
    }

    public void cacheOrMergeCitations(BibEntry entry, List<BibEntry> citations) {
        entry.getDOI().ifPresent(doi -> CITATIONS_MAP.put(doi.getDOI(), citations));
    }

    public void cacheOrMergeReferences(BibEntry entry, List<BibEntry> references) {
        entry.getDOI().ifPresent(doi -> REFERENCES_MAP.putIfAbsent(doi.getDOI(), references));
    }

    public boolean citationsCached(BibEntry entry) {
        return CITATIONS_MAP.containsKey(entry.getDOI().map(DOI::getDOI).orElse(""));
    }

    public boolean referencesCached(BibEntry entry) {
        return REFERENCES_MAP.containsKey(entry.getDOI().map(DOI::getDOI).orElse(""));
    }
}
