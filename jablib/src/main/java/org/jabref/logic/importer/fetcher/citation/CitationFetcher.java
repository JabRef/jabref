package org.jabref.logic.importer.fetcher.citation;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/**
 * Searches web resources for citing related articles based on a {@link BibEntry}.
 */
@NullMarked
public interface CitationFetcher {

    /**
     * Possible search methods
     */
    enum SearchType {
        CITES("reference"),
        CITED_BY("citation");

        public final String label;

        SearchType(String label) {
            this.label = label;
        }
    }

    /**
     * Returns the localized name of this fetcher.
     * The title can be used to display the fetcher in the menu and in the side pane.
     *
     * @return the localized name
     */
    String getName();

    /**
     * Looks for hits which are cited by the given {@link BibEntry}. This typically is the "References" (or "Bibliography" or "Literature") section of a paper.
     *
     * @param entry entry to search articles for
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    List<BibEntry> getReferences(BibEntry entry) throws FetcherException;

    /**
     * Looks for hits which are citing the given {@link BibEntry}.
     *
     * @param entry entry to search articles for
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    List<BibEntry> getCitations(BibEntry entry) throws FetcherException;

    /**
     * Get the paper details that includes citation count field for a given {@link BibEntry}.
     *
     * @param entry entry to search citation count field
     * @return returns a {@link Integer} for citation count field (may be empty)
     */
    Optional<Integer> getCitationCount(BibEntry entry) throws FetcherException;
}
