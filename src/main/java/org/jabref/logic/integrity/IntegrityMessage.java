package org.jabref.logic.integrity;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntegrityMessage that = (IntegrityMessage) o;
        return Objects.equals(entry, that.entry) &&
                Objects.equals(fieldName, that.fieldName) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entry, fieldName, message);
    }

}
