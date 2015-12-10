package net.sf.jabref.model.database;

import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibEntry;

import java.util.Collection;

public class BibDatabaseTypeDetection {
    public static BibDatabaseType inferType(Collection<BibEntry> entries) {
        // TODO: work on intersection of types for faster decision?
        // standard mode
        BibDatabaseType type = BibDatabaseType.BIBTEX;

        // check for biblatex entries
        if(entries.stream().allMatch(e -> typeBasedBiblatexMatch(e))) {
            return BibDatabaseType.BIBLATEX;
        }

        return type;
    }

    private static boolean typeBasedBiblatexMatch(BibEntry entry) {
        return BibLatexEntryTypes.ALL.stream().anyMatch(e -> e.getName().equalsIgnoreCase(entry.getType().getName()));
    }

    private void fieldBasedMatch() {

    }
}
