package org.jabref.model.metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ContentSelector {

    private final String fieldName;
    private final List<String> values;

    public ContentSelector(String fieldName, String... values) {
        this(fieldName, Arrays.asList(values));
    }

    public ContentSelector(String fieldName, List<String> values) {
        this.fieldName = fieldName;
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
        return Objects.equals(fieldName, that.fieldName) &&
                Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, values);
    }

    public String getFieldName() {
        return fieldName;
    }

    public List<String> getValues() {
        return Collections.unmodifiableList(values);
    }
}
