package net.sf.jabref.logic;

import net.sf.jabref.model.entry.BibEntry;

/**
 *
 */
public class FieldChange {

    private final BibEntry entry;
    private final String field;
    private final String oldValue;
    private final String newValue;


    public FieldChange(BibEntry entry, String field, String oldValue, String newValue) {
        this.entry = entry;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public BibEntry getEntry() {
        return this.entry;
    }

    public String getField() {
        return this.field;
    }

    public String getOldValue() {
        return this.oldValue;
    }

    public String getNewValue() {
        return this.newValue;
    }
}
