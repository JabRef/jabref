package org.jabref.logic.importer;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

/**
 * Searches web resources for bibliographic information based on an identifier.
 */
public interface IdBasedFetcher extends BibEntryFetcher {

    /**
     * Looks for bibliographic information associated to the given identifier.
     *
     * @param identifier a string which uniquely identifies the item
     * @param targetBibEntryFormat the format the entries should be returned in
     * @return a {@link BibEntry} containing the bibliographic information (or an empty optional if no data was found) in the requested format
     */
    Optional<BibEntry> performSearchById(String identifier, BibDatabaseMode targetBibEntryFormat) throws FetcherException;
}
