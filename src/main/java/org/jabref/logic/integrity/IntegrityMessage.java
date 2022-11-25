package org.jabref.logic.integrity;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public final class IntegrityMessage implements Cloneable {
    private final BibEntry entry;
    private final Field field;
    private final String message;

    public IntegrityMessage(String message, BibEntry entry, Field field) {
        this.message = message;
        this.entry = entry;
        this.field = field;
    }

    @Override
    public String toString() {
        return "[" + getEntry().getCitationKey().orElse("") + "] in " + field.getDisplayName() + ": " + getMessage();
    }

    public String getMessage() {
        return message;
    }

    public BibEntry getEntry() {
        return entry;
    }

    public Field getField() {
        return field;
    }

    @Override
    public Object clone() {
        return new IntegrityMessage(message, entry, field);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IntegrityMessage that = (IntegrityMessage) o;
        return Objects.equals(entry, that.entry) &&
                Objects.equals(field, that.field) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entry, field, message);
    }
}
