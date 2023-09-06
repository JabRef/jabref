package org.jabref.gui.entryeditor.citationrelationtab;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BibEntryRelationsCache {
    private static final Map<String, List<BibEntry>> citationsMap = new HashMap<>();
    private static final Map<String, List<BibEntry>> referencesMap = new HashMap<>();

    public List<BibEntry> getCitations(BibEntry entry) {
        return citationsMap.getOrDefault(entry.getDOI().map(DOI::getDOI).orElse(""), Collections.emptyList());
    }

    public List<BibEntry> getReferences(BibEntry entry) {
        return referencesMap.getOrDefault(entry.getDOI().map(DOI::getDOI).orElse(""), Collections.emptyList());
    }

    public void cacheOrMergeCitations(BibEntry entry, List<BibEntry> citations) {
        entry.getDOI().ifPresent(doi -> citationsMap.put(doi.getDOI(), citations));
    }

    public void cacheOrMergeReferences(BibEntry entry, List<BibEntry> references) {
        entry.getDOI().ifPresent(doi -> referencesMap.putIfAbsent(doi.getDOI(), references));
    }

    public boolean citationsCached(BibEntry entry) {
        return citationsMap.containsKey(entry.getDOI().map(DOI::getDOI).orElse(""));
    }

    public boolean referencesCached(BibEntry entry) {
        return referencesMap.containsKey(entry.getDOI().map(DOI::getDOI).orElse(""));
    }
}
