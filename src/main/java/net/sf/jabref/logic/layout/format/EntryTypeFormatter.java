package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.TypedBibEntry;
import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;


public class EntryTypeFormatter implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        // Fancy typesetting
        BibEntry entry = new BibEntry();
        entry.setType(fieldText);
        TypedBibEntry typedEntry = new TypedBibEntry(entry, BibDatabaseMode.BIBLATEX);
        return typedEntry.getTypeForDisplay();

    }


}
