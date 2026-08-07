package org.jabref.logic.citation.repository;

import java.util.List;

import org.jabref.model.entry.BibEntry;

public interface BibEntryCitationsAndReferencesRepository {

    /// Insert "cited by"
    void addCitations(BibEntry entry, List<BibEntry> citations);

    List<BibEntry> getCitations(BibEntry entry);

    boolean containsCitations(BibEntry entry);

    boolean isCitationsUpdatable(BibEntry entry);

    /// Insert "citing"
    void addReferences(BibEntry entry, List<BibEntry> citations);

    List<BibEntry> getReferences(BibEntry entry);

    boolean containsReferences(BibEntry entry);

    boolean isReferencesUpdatable(BibEntry entry);

    void close();
}
