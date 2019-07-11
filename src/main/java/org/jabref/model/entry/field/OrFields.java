package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.StringJoiner;

public class OrFields extends HashSet<Field> {

    public OrFields(Field field) {
        super(Collections.singleton(field));
    }

    public OrFields(Field... fields) {
        super(Arrays.asList(fields));
    }

    public OrFields(Collection<Field> fields) {
        super(fields);
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
}
