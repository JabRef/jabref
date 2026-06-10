package org.jabref.model.entry.event;

import java.util.Objects;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.jspecify.annotations.Nullable;

/// `FieldChangedEvent` is fired when a field of `BibEntry` has been modified, removed or added.
public class FieldChangedEvent extends EntryChangedEvent {

    private final Field field;
    @Nullable private final String newValue;
    @Nullable private final String oldValue;
    private int majorCharacterChange = 0;

    /// @param bibEntry Affected BibEntry object
    /// @param field    Name of field which has been changed
    /// @param oldValue old field value
    /// @param newValue new field value
    /// @param location Location affected by this event
    public FieldChangedEvent(BibEntry bibEntry, Field field, @Nullable String newValue, @Nullable String oldValue,
                             EntriesEventSource location) {
        super(bibEntry, location);
        this.field = field;
        this.newValue = newValue;
        this.oldValue = oldValue;
        this.majorCharacterChange = computeMajorCharacterChange(oldValue, newValue);
    }

    /// @param bibEntry Affected BibEntry object
    /// @param field    Name of field which has been changed
    /// @param newValue new field value
    public FieldChangedEvent(BibEntry bibEntry, Field field, @Nullable String newValue, @Nullable String oldValue) {
        super(bibEntry);
        this.field = field;
        this.newValue = newValue;
        this.oldValue = oldValue;
        this.majorCharacterChange = computeMajorCharacterChange(oldValue, newValue);
    }

    /// @param location Location affected by this event
    public FieldChangedEvent(FieldChange fieldChange, EntriesEventSource location) {
        super(fieldChange.getEntry(), location);
        this.field = fieldChange.getField();
        this.newValue = fieldChange.getNewValue();
        this.oldValue = fieldChange.getOldValue();
        this.majorCharacterChange = computeMajorCharacterChange(oldValue, newValue);
    }

    public FieldChangedEvent(FieldChange fieldChange) {
        this(fieldChange, EntriesEventSource.LOCAL);
    }

    private int computeMajorCharacterChange(@Nullable String oldValue, @Nullable String newValue) {
        // Objects.equals first checks '=='
        if ((oldValue == null && newValue == null) || Objects.equals(oldValue, newValue)) {
            return 0;
        } else if (oldValue == null) {
            return newValue.length();
        } else if (newValue == null) {
            return oldValue.length();
        } else if (oldValue.length() == newValue.length()) {
            return newValue.length();
        } else {
            return Math.abs(newValue.length() - oldValue.length());
        }
    }

    public Field getField() {
        return field;
    }

    public Optional<String> getNewValue() {
        return Optional.ofNullable(newValue);
    }

    public Optional<String> getOldValue() {
        return Optional.ofNullable(oldValue);
    }

    public int getMajorCharacterChange() {
        return majorCharacterChange;
    }
}
