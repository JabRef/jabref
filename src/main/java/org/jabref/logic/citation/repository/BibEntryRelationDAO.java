package org.jabref.logic.citation.repository;

import java.util.List;

import org.jabref.model.entry.BibEntry;

public interface BibEntryRelationDAO {

    List<BibEntry> getRelations(BibEntry entry);

    void cacheOrMergeRelations(BibEntry entry, List<BibEntry> relations);

    boolean containsKey(BibEntry entry);
}
