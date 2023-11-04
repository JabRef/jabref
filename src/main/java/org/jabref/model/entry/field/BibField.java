package org.jabref.model.entry.field;

import java.util.Objects;

public record BibField(Field field, FieldPriority priority) implements Comparable<BibField> {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BibField)) {
            return false;
        }
        BibField other = (BibField) o;
        return field.getName().equalsIgnoreCase(other.field.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(field.getName());
    }

    @Override
    public String toString() {
        return "BibField{" +
                "field=" + field.getDisplayName() +
                ", priority=" + priority +
                '}';
    }

    @Override
    public int compareTo(BibField o) {
        return field.getName().compareTo(o.field.getName());
    }
}
