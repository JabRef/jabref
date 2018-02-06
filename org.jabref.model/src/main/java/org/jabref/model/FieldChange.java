package org.jabref.model;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;

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
                if (other.oldValue != null) {
                    return false;
                }
            } else if (!oldValue.equals(other.oldValue)) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "FieldChange [entry=" + entry.getCiteKeyOptional().orElse("") + ", field=" + field + ", oldValue="
                + oldValue + ", newValue=" + newValue + "]";
    }
}
