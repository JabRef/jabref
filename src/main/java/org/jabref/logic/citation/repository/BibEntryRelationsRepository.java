package org.jabref.logic.citation.repository;

import java.util.List;
import org.jabref.model.entry.BibEntry;

public interface BibEntryRelationsRepository {
    List<BibEntry> readCitations(BibEntry entry);

    List<BibEntry> readReferences(BibEntry entry);

    /**
     * Fetch citations for a bib entry and update local database.
     * @param entry should not be null
     * @deprecated fetching citations should be done by the service layer (calling code)
     */
    @Deprecated
    void forceRefreshCitations(BibEntry entry);

    /**
     * Fetch references made by a bib entry and update local database.
     * @param entry should not be null
     * @deprecated fetching references should be done by the service layer (calling code)
     */
    @Deprecated
    void forceRefreshReferences(BibEntry entry);
}
