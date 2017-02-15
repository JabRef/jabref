package net.sf.jabref.logic.openoffice;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

/**
 * Subclass of BibEntry for representing entries referenced in a document that can't
 * be found in JabRef's current database.
 */
public class UndefinedBibtexEntry extends BibEntry {

    private final String key;


    public UndefinedBibtexEntry(String key) {
        this.key = key;
        setField(FieldName.AUTHOR, OOBibStyle.UNDEFINED_CITATION_MARKER);
    }

    public String getKey() {
        return key;
    }
}
