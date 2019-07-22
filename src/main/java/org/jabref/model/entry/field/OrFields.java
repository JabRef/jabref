package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.StringJoiner;
import java.util.TreeSet;

public class OrFields extends TreeSet<Field> implements Comparable<OrFields> {

    public OrFields(Field field) {
        super(Comparator.comparing(Field::getName));
        add(field);
    }

    public OrFields(Field... fields) {
        super(Comparator.comparing(Field::getName));
        addAll(Arrays.asList(fields));
    }

    public OrFields(Collection<Field> fields) {
        super(Comparator.comparing(Field::getName));
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
