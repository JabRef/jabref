package org.jabref.logic.importer;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.importer.fetcher.TrustLevel;
import org.jabref.model.entry.BibEntry;
import org.jspecify.annotations.NonNull;

/**
 * Interface for classes that attempt to resolve a full-text PDF URL
 * for a given {@link BibEntry}.
 */
public interface FulltextFetcher {

    /**
     * Tries to find a full-text URL for the provided BibTeX entry.
     *
     * @param entry the {@link BibEntry} for which to search the full-text PDF
     * @return an {@link Optional} containing the full-text PDF {@link URL} if found,
     *         or an empty Optional if no PDF was found.
     *
     * @throws NullPointerException if {@code entry} is null
     * @throws IOException          if an I/O operation fails during fetching
     * @throws FetcherException     if a fetcher-specific error occurs
     */
    Optional<URL> findFullText(@NonNull BibEntry entry) throws IOException, FetcherException;

    /**
     * Returns the trust level of this fetcher.
     *
     * @return the {@link TrustLevel} of this fetcher
     */
    TrustLevel getTrustLevel();
}
