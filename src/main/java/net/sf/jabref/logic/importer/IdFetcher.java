package net.sf.jabref.logic.importer;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Looks for article identifier based on already present bibliographic information.
 */
public interface IdFetcher {

    /**
     * Looks for an identifier based on the information stored in the given {@link BibEntry} and
     * then updates the {@link BibEntry} with the found id.
     *
     * @param entry the {@link BibEntry} for which an identifier should be found
     * @return an updated {@link BibEntry} containing the identifier (if an ID was found, otherwise the {@link BibEntry}
     *         is left unchanged)
     */
    BibEntry updateIdentfier(BibEntry entry);
}
