package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public class OrFields implements Comparable<OrFields> {

    private LinkedHashSet<Field> fields = new LinkedHashSet<>();

    public OrFields(Field field) {
        fields.add(field);
    }

    public OrFields(Field... fieldsToAdd) {
        Arrays.stream(fieldsToAdd).forEach(fields::add);
    }

    public OrFields(Collection<Field> fieldsToAdd) {
        fields.addAll(fieldsToAdd);
    }

    public String getDisplayName() {
        StringJoiner joiner = new StringJoiner("/");
        for (Field field : fields) {
            joiner.add(field.getDisplayName());
        }
        return joiner.toString();
    }

    public Field getPrimary() {
        return fields.getFirst();
    }

    public Set<Field> getFields() {
        return this.fields;
    }

    public boolean contains(Field field) {
        return fields.contains(field);
    }

    @Override
    public int compareTo(OrFields o) {
        return FieldFactory.serializeOrFields(this).compareTo(FieldFactory.serializeOrFields(o));
    }

    public boolean hasExactlyOne() {
        return this.fields.size() == 1;
    }

    public boolean isEmpty() {
        return this.fields.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrFields orFields = (OrFields) o;
        return Objects.equals(fields, orFields.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fields);
    }

    @Override
    public String toString() {
        return "OrFields{" +
                "fields=" + fields +
                '}';
    }
}
