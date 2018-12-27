package org.jabref.model.entry.event;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

/**
 * <code>FieldChangedEvent</code> is fired when a field of <code>BibEntry</code> has been modified, removed or added.
 */
public class FieldChangedEvent extends EntryChangedEvent {

    private final String fieldName;
    private final String newValue;
    private final String oldValue;
    private int delta = 0;


    /**
     * @param bibEntry  Affected BibEntry object
     * @param fieldName Name of field which has been changed
     * @param newValue  new field value
     * @param newValue  old field value
     * @param location  location Location affected by this event
     */
    public FieldChangedEvent(BibEntry bibEntry, String fieldName, String newValue, String oldValue,
                             EntryEventSource location) {
        super(bibEntry, location);
        this.fieldName = fieldName;
        this.newValue = newValue;
        this.oldValue = oldValue;
        delta = computeDelta(oldValue, newValue);
    }

    /**
     * @param bibEntry  Affected BibEntry object
     * @param fieldName Name of field which has been changed
     * @param newValue  new field value
     */
    public FieldChangedEvent(BibEntry bibEntry, String fieldName, String newValue, String oldValue) {
        super(bibEntry);
        this.fieldName = fieldName;
        this.newValue = newValue;
        this.oldValue = oldValue;
        delta = computeDelta(oldValue, newValue);
    }

    /**
     * @param bibEntry  Affected BibEntry object
     * @param fieldName Name of field which has been changed
     * @param newValue  new field value
     * @param location  location Location affected by this event
     */
    public FieldChangedEvent(FieldChange fieldChange, EntryEventSource location) {
        super(fieldChange.getEntry(), location);
        this.fieldName = fieldChange.getField();
        this.newValue = fieldChange.getNewValue();
        this.oldValue = fieldChange.getOldValue();
        delta = computeDelta(oldValue, newValue);
    }

    public FieldChangedEvent(FieldChange fieldChange) {
        this(fieldChange, EntryEventSource.LOCAL);
    }

    private int computeDelta(String oldValue, String newValue) {
        if (oldValue == newValue) {
            return 0;
        } else if (oldValue == null && newValue != null) {
            return newValue.length();
        } else if (newValue == null && oldValue != null) {
            return oldValue.length();
        } else {
            return Math.abs(newValue.length() - oldValue.length());
        }
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

    public int getDelta() {
        return delta;
    }

}
