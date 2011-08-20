package net.sf.jabref.oo;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Util;

/**
 * Subclass of BibtexEntry for representing entries referenced in a document that can't
 * be found in JabRef's current database.
 */
public class UndefinedBibtexEntry extends BibtexEntry {
    private String key;

    public UndefinedBibtexEntry(String key) {
        super(Util.createNeutralId());
        this.key = key;
        setField("author", OOBibStyle.UNDEFINED_CITATION_MARKER);
    }

    public String getKey() {
        return key;
    }
}
