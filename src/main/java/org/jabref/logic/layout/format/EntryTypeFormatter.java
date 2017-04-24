package org.jabref.logic.layout.format;

import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

/*
 * Camel casing of entry type string, unknown entry types gets a leading capital
 *
 * Example (known): inbook -> InBook
 * Example (unknown): banana -> Banana
 */
public class EntryTypeFormatter implements LayoutFormatter {

    /**
     * Input: entry type as a string
     */
    @Override
    public String format(String entryType) {
        BibEntry entry = new BibEntry();
        entry.setType(entryType);
        TypedBibEntry typedEntry = new TypedBibEntry(entry, BibDatabaseMode.BIBLATEX);
        return typedEntry.getTypeForDisplay();
    }
}
