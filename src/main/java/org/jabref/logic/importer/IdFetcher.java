package org.jabref.logic.importer;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;

/**
 * Looks for article identifier based on already present bibliographic information.
 */
public interface IdFetcher<T> extends WebFetcher {

    /**
     * Looks for an identifier based on the information stored in the given {@link BibEntry}.
     *
     * @param entry the {@link BibEntry} for which an identifier should be found
     * @return the identifier (if an ID was found, otherwise an empty {@link Optional})
     */
    Optional<T> findIdentifier(BibEntry entry) throws FetcherException;
}
