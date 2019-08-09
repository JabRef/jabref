package org.jabref.logic.bibtex;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.field.Field;

public class FieldContentParserPreferences {

    private final List<Field> nonWrappableFields;

    public FieldContentParserPreferences() {
        // This constructor is only to allow an empty constructor in SavePreferences
        this.nonWrappableFields = Collections.emptyList();
    }

    public FieldContentParserPreferences(List<Field> nonWrappableFields) {
        this.nonWrappableFields = nonWrappableFields;
    }

    public List<Field> getNonWrappableFields() {
        return nonWrappableFields;
    }

}
