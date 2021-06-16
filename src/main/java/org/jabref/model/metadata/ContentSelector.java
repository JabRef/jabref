package org.jabref.model.metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jabref.model.entry.field.Field;

public class ContentSelector {

    private final Field field;
    private final List<String> values;

    public ContentSelector(Field field, String... values) {
        this(field, Arrays.asList(values));
    }

    public ContentSelector(Field field, List<String> values) {
        this.field = field;
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ContentSelector that = (ContentSelector) o;
        return Objects.equals(field, that.field) &&
                Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, values);
    }

    public Field getField() {
        return field;
    }

    public List<String> getValues() {
        return Collections.unmodifiableList(values);
    }
}
