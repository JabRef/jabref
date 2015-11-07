package net.sf.jabref.logic.fetcher;

import net.sf.jabref.model.entry.BibtexEntry;

import java.net.URL;
import java.io.IOException;
import java.util.Optional;

/**
 * This interface is used for classes that try to resolve a full-text PDF url for a BibTex entry.
 * Implementing classes should specialize on specific article sites.
 * See e.g. @link{http://libguides.mit.edu/apis}.
 */
public interface FullTextFinder {
    /**
     * Tries to find a fulltext URL for a given BibTex entry.
     *
     * @param entry The Bibtex entry
     * @return The fulltext PDF URL Optional, if found, or an empty Optional if not found.
     * @throws NullPointerException if no BibTex entry is given
     * @throws java.io.IOException
     */
    Optional<URL> findFullText(BibtexEntry entry) throws IOException;
}
