package org.jabref.logic.citation.repository;

import java.util.List;

import org.jabref.model.entry.BibEntry;

public class MVStoreBibEntryRelationsRepository implements BibEntryRelationsRepository {

    @Override
    public void insertCitations(BibEntry entry, List<BibEntry> citations) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<BibEntry> readCitations(BibEntry entry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsCitations(BibEntry entry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insertReferences(BibEntry entry, List<BibEntry> citations) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<BibEntry> readReferences(BibEntry entry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsReferences(BibEntry entry) {
        throw new UnsupportedOperationException();
    }
}
