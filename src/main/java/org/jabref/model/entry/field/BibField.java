package org.jabref.model.entry.field;

import java.util.Objects;

public class BibField implements Comparable<BibField> {

    private final FieldPriority priority;
    private final Field field;

    public BibField(Field field, FieldPriority priority) {
        this.priority = priority;
        this.field = field;
    }

    public Field getField() {
        return field;
    }

    public FieldPriority getPriority() {
        return priority;
    }

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
                "field=" + field.getName() +
                ", priority=" + priority +
                '}';
    }

    @Override
    public int compareTo(BibField o) {
        return field.getName().compareTo(o.field.getName());
    }
}
