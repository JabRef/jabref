package org.jabref.model.entry.event;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/**
 * <code>FieldChangedEvent</code> is fired when a field of <code>BibEntry</code> has been modified, removed or added.
 */
public class FieldChangedEvent extends EntryChangedEvent {

    private final Field field;
    private final String newValue;
    private final String oldValue;
    private int charactersChangedCount = 0;

    /**
     * @param source source of this event
     * @param bibEntry Affected BibEntry object
     * @param field    Name of field which has been changed
     * @param oldValue old field value
     * @param newValue new field value
     */
    public FieldChangedEvent(EntriesEventSource source, BibEntry bibEntry, Field field, String oldValue, String newValue) {
        super(bibEntry, source);
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.charactersChangedCount = computeMajorCharacterChange(oldValue, newValue);
    }

    /**
     * @param bibEntry Affected BibEntry object
     * @param field    Name of field which has been changed
     * @param newValue new field value
     */
    public FieldChangedEvent(BibEntry bibEntry, Field field, String oldValue, String newValue) {
        super(bibEntry);
        this.field = field;
        this.newValue = newValue;
        this.oldValue = oldValue;
        this.charactersChangedCount = computeMajorCharacterChange(oldValue, newValue);
    }

    public FieldChangedEvent(EntriesEventSource source, FieldChange fieldChange) {
        super(fieldChange.getEntry(), source);
        this.field = fieldChange.getField();
        this.newValue = fieldChange.getNewValue();
        this.oldValue = fieldChange.getOldValue();
        this.charactersChangedCount = computeMajorCharacterChange(oldValue, newValue);
    }

    public FieldChangedEvent(FieldChange fieldChange) {
        this(EntriesEventSource.LOCAL, fieldChange);
    }

    private static int computeMajorCharacterChange(String oldValue, String newValue) {
        // We do == because of performance reasons
        if (oldValue == newValue) {
            return 0;
        } else if ((oldValue == null) && (newValue != null)) {
            return newValue.length();
        } else if ((newValue == null) && (oldValue != null)) {
            return oldValue.length();
        } else if ((oldValue.length() == newValue.length()) && !oldValue.equals(newValue)) {
            return newValue.length();
        } else {
            return Math.abs(newValue.length() - oldValue.length());
        }
    }

    public Field getField() {
        return field;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public int charactersChangedCount() {
        return charactersChangedCount;
    }
}
