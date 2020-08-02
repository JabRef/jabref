package org.jabref.logic.importer;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;

/**
 * Searches web resources for bibliographic information based on an identifier.
 */
public interface IdBasedFetcher extends WebFetcher {

    /**
     * Looks for bibliographic information associated to the given identifier.
     *
     * @param identifier a string which uniquely identifies the item
     * @return a {@link BibEntry} containing the bibliographic information (or an empty optional if no data was found)
     */
    Optional<BibEntry> performSearchById(String identifier) throws FetcherException;
}
