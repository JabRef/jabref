package org.jabref.logic.importer;

import org.jabref.logic.formatter.Formatter;
import org.jabref.model.entry.BibEntry;

public interface ParserFetcher {

    /**
     * Performs a cleanup of the fetched entry.
     * <p>
     * Only systematic errors of the fetcher should be corrected here
     * (i.e. if information is consistently contained in the wrong field or the wrong format)
     * but not cosmetic issues which may depend on the user's taste (for example, LateX code vs HTML in the abstract).
     * <p>
     * Try to reuse existing {@link Formatter} for the cleanup. For example,
     * {@code new FieldFormatterCleanup(StandardField.TITLE, new RemoveBracesFormatter()).cleanup(entry);}
     * <p>
     * By default, no cleanup is done.
     *
     * @param entry the entry to be cleaned-up
     */
    default void doPostCleanup(BibEntry entry) {
        // Do nothing by default
    }
}
