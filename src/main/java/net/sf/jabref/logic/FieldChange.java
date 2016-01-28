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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((entry == null) ? 0 : entry.hashCode());
        result = (prime * result) + ((field == null) ? 0 : field.hashCode());
        result = (prime * result) + ((newValue == null) ? 0 : newValue.hashCode());
        result = (prime * result) + ((oldValue == null) ? 0 : oldValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
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

    @Override
    public String toString() {
        return "FieldChange [entry=" + entry.getCiteKey() + ", field=" + field + ", oldValue=" + oldValue
                + ", newValue=" + newValue
                + "]";
    }
}
