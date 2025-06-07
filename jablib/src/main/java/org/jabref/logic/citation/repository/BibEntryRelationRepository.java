package org.jabref.logic.citation.repository;

import java.util.List;

import org.jabref.model.entry.BibEntry;

/**
 * Generic interface for a repository that stores relations between BibEntries.
 */
public interface BibEntryRelationRepository {

    List<BibEntry> getRelations(BibEntry entry);

    /**
     * Adds the given relations to the entry. Appends to existing relations.
     */
    void addRelations(BibEntry entry, List<BibEntry> relations);

    boolean containsKey(BibEntry entry);

    default boolean shouldUpdate(BibEntry entry) {
        return true;
    }

    void close();
}
