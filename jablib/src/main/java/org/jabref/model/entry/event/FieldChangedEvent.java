package org.jabref.model.entry.event;

import java.util.Objects;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// `FieldChangedEvent` is fired when a field of `BibEntry` has been modified, removed or added.
///
/// A value is `null` when the field had none: before the field was added, or after it was removed.
@NullMarked
public class FieldChangedEvent extends EntryChangedEvent {

    private final Field field;
    private final @Nullable String oldValue;
    private final @Nullable String newValue;
    private int charactersChangedCount = 0;

    /// @param source   source of this event
    /// @param bibEntry Affected BibEntry object
    /// @param field    Name of field which has been changed
    /// @param oldValue old field value
    /// @param newValue new field value
    public FieldChangedEvent(EntriesEventSource source, BibEntry bibEntry, Field field, @Nullable String oldValue, @Nullable String newValue) {
        super(bibEntry, source);
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.charactersChangedCount = computeMajorCharacterChange(oldValue, newValue);
    }

    /// @param bibEntry Affected BibEntry object
    /// @param field    Name of field which has been changed
    /// @param newValue new field value
    public FieldChangedEvent(BibEntry bibEntry, Field field, @Nullable String oldValue, @Nullable String newValue) {
        super(bibEntry);
        this.field = field;
        this.newValue = newValue;
        this.oldValue = oldValue;
        this.charactersChangedCount = computeMajorCharacterChange(oldValue, newValue);
    }

    public FieldChangedEvent(EntriesEventSource source, FieldChange fieldChange) {
        super(fieldChange.entry(), source);
        this.field = fieldChange.field();
        this.newValue = fieldChange.newValue();
        this.oldValue = fieldChange.oldValue();
        this.charactersChangedCount = computeMajorCharacterChange(fieldChange.oldValue(), fieldChange.newValue());
    }

    public FieldChangedEvent(FieldChange fieldChange) {
        this(EntriesEventSource.LOCAL, fieldChange);
    }

    /// An absent value counts as empty: adding a value changes as many characters as it is long.
    private static int computeMajorCharacterChange(@Nullable String oldValue, @Nullable String newValue) {
        String before = Objects.toString(oldValue, "");
        String after = Objects.toString(newValue, "");
        if (before.equals(after)) {
            return 0;
        }
        if (before.length() == after.length()) {
            return after.length();
        }
        return Math.abs(after.length() - before.length());
    }

    public Field getField() {
        return field;
    }

    public @Nullable String getOldValue() {
        return oldValue;
    }

    public @Nullable String getNewValue() {
        return newValue;
    }

    public int charactersChangedCount() {
        return charactersChangedCount;
    }
}
