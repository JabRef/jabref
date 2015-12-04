package net.sf.jabref.logic;

import net.sf.jabref.model.entry.BibtexEntry;

/**
 *
 */
public class FieldChange {

    private final BibtexEntry entry;
    private final String field;
    private final String oldValue;
    private final String newValue;


    public FieldChange(BibtexEntry entry, String field, String oldValue, String newValue) {
        this.entry = entry;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public BibtexEntry getEntry() {
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
