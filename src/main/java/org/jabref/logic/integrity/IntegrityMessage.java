package org.jabref.logic.integrity;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;

import org.apache.commons.lang3.builder.EqualsBuilder;

public final class IntegrityMessage implements Cloneable {

    private final BibEntry entry;
    private final String fieldName;
    private final String message;

    public IntegrityMessage(String message, BibEntry entry, String fieldName) {
        this.message = message;
        this.entry = entry;
        this.fieldName = fieldName;
    }

    @Override
    public String toString() {
        return "[" + getEntry().getCiteKeyOptional().orElse("") + "] in " + getFieldName() + ": " + getMessage();
    }

    public String getMessage() {
        return message;
    }

    public BibEntry getEntry() {
        return entry;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Object clone() {
        return new IntegrityMessage(message, entry, fieldName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        IntegrityMessage that = (IntegrityMessage) obj;
        return new EqualsBuilder()
                .append(entry, that.entry)
                .append(fieldName, that.fieldName)
                .append(message, that.message)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return Objects.hash(entry, fieldName, message);
    }

}
