package org.jabref.logic.citation.repository;

import java.util.List;

import org.jabref.model.entry.BibEntry;

public interface BibEntryCitationsAndReferencesRepository {

    /// Insert "cited by"
    void insertCitations(BibEntry entry, List<BibEntry> citations);

    List<BibEntry> readCitations(BibEntry entry);

    boolean containsCitations(BibEntry entry);

    boolean isCitationsUpdatable(BibEntry entry);

    /// Insert "citing"
    void insertReferences(BibEntry entry, List<BibEntry> citations);

    List<BibEntry> readReferences(BibEntry entry);

    boolean containsReferences(BibEntry entry);

    boolean isReferencesUpdatable(BibEntry entry);

    void close();
}
