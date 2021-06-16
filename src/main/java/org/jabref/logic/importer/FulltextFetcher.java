package org.jabref.logic.importer;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.importer.fetcher.TrustLevel;
import org.jabref.model.entry.BibEntry;

/**
 * This interface is used for classes that try to resolve a full-text PDF url for a BibTex entry.
 * Implementing classes should specialize on specific article sites.
 * See e.g. @link{http://libguides.mit.edu/apis}.
 */
public interface FulltextFetcher {
    /**
     * Tries to find a fulltext URL for a given BibTex entry.
     *
     * @param entry The Bibtex entry
     * @return The fulltext PDF URL Optional, if found, or an empty Optional if not found.
     * @throws NullPointerException if no BibTex entry is given
     * @throws java.io.IOException  if an IO operation has failed
     * @throws FetcherException     if a fetcher specific error occurred
     */
    Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException;

    /**
     * Returns the level of trust for this fetcher.
     * We distinguish between publishers and meta search engines for example.
     *
     * @return The trust level of the fetcher, the higher the better
     */
    default TrustLevel getTrustLevel() {
        return TrustLevel.UNKNOWN;
    }
}
