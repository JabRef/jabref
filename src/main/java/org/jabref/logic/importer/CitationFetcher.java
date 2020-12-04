package org.jabref.logic.importer;

import java.util.List;

import org.jabref.model.entry.BibEntry;

/**
 * Searches web resources for citing related articles based on a {@link BibEntry}.
 */
public interface CitationFetcher {

    /**
     * Looks for hits which are citing the given {@link BibEntry}.
     *
     * @param entry entry to search articles for
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    List<BibEntry> searchCitedBy(BibEntry entry) throws FetcherException;

    /**
     * Looks for hits which are cited by the given {@link BibEntry}.
     *
     * @param entry entry to search articles for
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    List<BibEntry> searchCiting(BibEntry entry) throws FetcherException;
}
