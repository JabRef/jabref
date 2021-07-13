package org.jabref.model;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/**
 * This class is used in the instance of a field being modified, removed or added.
 */
public class FieldChange {

    private final BibEntry entry;
    private final Field field;
    private final String oldValue;
    private final String newValue;

    public FieldChange(BibEntry entry, Field field, String oldValue, String newValue) {
        this.entry = Objects.requireNonNull(entry);
        this.field = Objects.requireNonNull(field);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public BibEntry getEntry() {
        return this.entry;
    }

    public Field getField() {
        return this.field;
    }

    public String getOldValue() {
        return this.oldValue;
    }

    public String getNewValue() {
        return this.newValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entry, field, newValue, oldValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FieldChange) {
            FieldChange other = (FieldChange) obj;
            if (entry == null) {
                if (other.entry != null) {
                    return false;
                }
            } else if (!entry.equals(other.entry)) {
                return false;
            }
            if (field == null) {
                if (other.field != null) {
                    return false;
                }
            } else if (!field.equals(other.field)) {
                return false;
            }
            if (newValue == null) {
                if (other.newValue != null) {
                    return false;
                }
            } else if (!newValue.equals(other.newValue)) {
                return false;
            }
            if (oldValue == null) {
                return other.oldValue == null;
            } else {
                return oldValue.equals(other.oldValue);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "FieldChange [entry=" + entry.getCitationKey().orElse("") + ", field=" + field + ", oldValue="
                + oldValue + ", newValue=" + newValue + "]";
    }
}
