package org.jabref.logic.openoffice;

import org.jabref.logic.openoffice.style.OOBibStyle;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

/**
 * Subclass of BibEntry for representing entries referenced in a document that can't be found in JabRef's current database.
 */
public class UndefinedBibtexEntry extends BibEntry {

    private final String key;

    public UndefinedBibtexEntry(String key) {
        this.key = key;
        setField(StandardField.AUTHOR, OOBibStyle.UNDEFINED_CITATION_MARKER);
    }

    public String getKey() {
        return key;
    }
}
