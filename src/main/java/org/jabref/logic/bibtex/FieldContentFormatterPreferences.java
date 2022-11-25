package org.jabref.logic.bibtex;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.field.Field;

public class FieldContentFormatterPreferences {

    private final List<Field> nonWrappableFields;

    /**
     * Constructor defining that there are not any non-wrappable fields
     */
    public FieldContentFormatterPreferences() {
        this.nonWrappableFields = Collections.emptyList();
    }

    public FieldContentFormatterPreferences(List<Field> nonWrappableFields) {
        this.nonWrappableFields = nonWrappableFields;
    }

    public List<Field> getNonWrappableFields() {
        return nonWrappableFields;
    }
}
