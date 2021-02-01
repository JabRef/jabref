package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.StringJoiner;

public class OrFields extends LinkedHashSet<Field> implements Comparable<OrFields> {

    public OrFields(Field field) {
        add(field);
    }

    public OrFields(Field... fields) {
        addAll(Arrays.asList(fields));
    }

    public OrFields(Collection<Field> fields) {
        addAll(fields);
    }

    public String getDisplayName() {
        StringJoiner joiner = new StringJoiner("/");
        for (Field field : this) {
            joiner.add(field.getDisplayName());
        }
        return joiner.toString();
    }

    public Field getPrimary() {
        return this.iterator().next();
    }

    @Override
    public int compareTo(OrFields o) {
        return FieldFactory.serializeOrFields(this).compareTo(FieldFactory.serializeOrFields(o));
    }
}
