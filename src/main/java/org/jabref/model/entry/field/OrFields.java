package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
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

    public OrFields(Collection<Field> fields) {
        fields.addAll(fields);
    }

    public String getDisplayName() {
        StringJoiner joiner = new StringJoiner("/");
        for (Field field : fields) {
            joiner.add(field.getDisplayName());
        }
        return joiner.toString();
    }

    public Field getPrimary() {
        return fields.iterator().next();
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
}
