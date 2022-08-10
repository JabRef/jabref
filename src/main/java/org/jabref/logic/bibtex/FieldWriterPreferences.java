package org.jabref.logic.bibtex;

import java.util.List;

import org.jabref.model.entry.field.Field;

public class FieldWriterPreferences {

    private final boolean resolveStrings;
    private final List<Field> resolveStringsForFields;
    private final FieldContentFormatterPreferences fieldContentFormatterPreferences;

    /**
     * @param resolveStrings true - The character {@link FieldWriter#BIBTEX_STRING_START_END_SYMBOL} should be interpreted as indicator of BibTeX strings
     */
    public FieldWriterPreferences(boolean resolveStrings, List<Field> resolveStringsForFields,
                                  FieldContentFormatterPreferences fieldContentFormatterPreferences) {
        this.resolveStrings = resolveStrings;
        this.resolveStringsForFields = resolveStringsForFields;
        this.fieldContentFormatterPreferences = fieldContentFormatterPreferences;
    }

    public boolean isResolveStrings() {
        return resolveStrings;
    }

    public List<Field> getResolveStringsForFields() {
        return resolveStringsForFields;
    }

    public FieldContentFormatterPreferences getFieldContentFormatterPreferences() {
        return fieldContentFormatterPreferences;
    }
}
