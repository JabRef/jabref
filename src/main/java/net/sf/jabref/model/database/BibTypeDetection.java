package net.sf.jabref.model.database;

import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibEntry;

import java.util.Collection;

public class BibTypeDetection {
    public static BibType inferType(Collection<BibEntry> entries) {
        // standard mode
        BibType type = BibType.BIBTEX;

        // check for biblatex entries
        // TODO must be based on name not type!!!
        // TODO rename BibtexEntry and BibtexDatabase to generic names
        if(entries.stream().allMatch(e -> BibLatexEntryTypes.ALL.contains(e.getType()))) {
            return BibType.BIBLATEX;
        }

        return type;
    }

    private void typeBasedCheck() {

    }

    private void fieldBasedCheck() {

    }
}
