package org.jabref.model.metadata;

import java.util.Collections;
import java.util.List;

public class ContentSelector {

    private final String fieldName;

    private final List<String> values;

    public ContentSelector(String fieldName, List<String> values) {
        this.fieldName = fieldName;
        this.values = values;
    }

    public String getFieldName() {
        return fieldName;
    }

    public List<String> getValues() {
        return Collections.unmodifiableList(values);
    }
}
