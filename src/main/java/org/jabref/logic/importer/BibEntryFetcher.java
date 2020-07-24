package org.jabref.logic.importer;

import org.jabref.logic.cleanup.ConvertToBiblatexCleanup;
import org.jabref.logic.cleanup.ConvertToBibtexCleanup;
import org.jabref.model.cleanup.Formatter;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

public interface BibEntryFetcher extends WebFetcher {

    /**
     * Performs a cleanup of the fetched entry.
     *
     * Only systematic errors of the fetcher should be corrected here
     * (i.e. if information is consistently contained in the wrong field or the wrong format)
     * but not cosmetic issues which may depend on the user's taste (for example, LateX code vs HTML in the abstract).
     *
     * Try to reuse existing {@link Formatter} for the cleanup. For example,
     * {@code new FieldFormatterCleanup(StandardField.TITLE, new RemoveBracesFormatter()).cleanup(entry);}
     *
     * By default converts the format of fetched BibEntries into the targeted mode
     *
     * If this method is overwritten, you either have to ensure that you convert
     * the entries into the correct format, or call this method in the overwritten
     * method using ParentInterface.super.doPostCleanup(...)
     *
     * @param entry the entry to be cleaned-up
     * @param targetBibEntryFormat the format the entry should be converted to
     */
    default void doPostCleanup(BibEntry entry, BibDatabaseMode targetBibEntryFormat) {
        if (targetBibEntryFormat == BibDatabaseMode.BIBTEX && getBibFormatOfFetchedEntries() == BibDatabaseMode.BIBLATEX) {
            new ConvertToBibtexCleanup().cleanup(entry);
        } else if (targetBibEntryFormat == BibDatabaseMode.BIBLATEX && getBibFormatOfFetchedEntries() == BibDatabaseMode.BIBTEX) {
            new ConvertToBiblatexCleanup().cleanup(entry);
        }
    }

    /**
     * Each fetcher returns his BibEntries either formated in BibTeX or BibLaTeX
     *
     * @return The format the fetcher returns
     */
    BibDatabaseMode getBibFormatOfFetchedEntries();
}
