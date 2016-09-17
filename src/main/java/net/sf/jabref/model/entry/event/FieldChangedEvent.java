package net.sf.jabref.model.entry.event;

import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;

/**
 * <code>FieldChangedEvent</code> is fired when a field of <code>BibEntry</code> has been modified, removed or added.
 */
public class FieldChangedEvent extends EntryChangedEvent {

    private final String fieldName;
    private final String newValue;
    private final String oldValue;


    /**
     * @param bibEntry Affected BibEntry object
     * @param fieldName Name of field which has been changed
     * @param newValue new field value
     * @param newValue old field value
     * @param location location Location affected by this event
     */
    public FieldChangedEvent(BibEntry bibEntry, String fieldName, String newValue, String oldValue,
            EntryEventSource location) {
        super(bibEntry, location);
        this.fieldName = fieldName;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    /**
     * @param bibEntry Affected BibEntry object
     * @param fieldName Name of field which has been changed
     * @param newValue new field value
     */
    public FieldChangedEvent(BibEntry bibEntry, String fieldName, String newValue, String oldValue) {
        super(bibEntry);
        this.fieldName = fieldName;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    /**
     * @param bibEntry Affected BibEntry object
     * @param fieldName Name of field which has been changed
     * @param newValue new field value
     * @param location location Location affected by this event
     */
    public FieldChangedEvent(FieldChange fieldChange, EntryEventSource location) {
        super(fieldChange.getEntry(), location);
        this.fieldName = fieldChange.getField();
        this.newValue = fieldChange.getNewValue();
        this.oldValue = fieldChange.getOldValue();
    }

    public FieldChangedEvent(FieldChange fieldChange) {
        this(fieldChange, EntryEventSource.LOCAL);
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getOldValue() {
        return oldValue;
    }

}
