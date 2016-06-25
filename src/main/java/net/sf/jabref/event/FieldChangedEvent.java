package net.sf.jabref.event;

import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;

/**
 * <code>FieldChangedEvent</code> is fired when a field of <code>BibEntry</code> has been modified, removed or added.
 */
public class FieldChangedEvent extends EntryChangedEvent {

    private final String fieldName;
    private final String newValue;

    /**
     * @param bibEntry Affected BibEntry object
     * @param fieldName Name of field which has been changed
     * @param newValue new field value
     */
    public FieldChangedEvent(BibEntry bibEntry, String fieldName, String newValue) {
        super(bibEntry);
        this.fieldName = fieldName;
        this.newValue = newValue;
    }

    public FieldChangedEvent(FieldChange fieldChange) {
        this(fieldChange.getEntry(), fieldChange.getField(), fieldChange.getNewValue());
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getNewValue() {
        return newValue;
    }

}
