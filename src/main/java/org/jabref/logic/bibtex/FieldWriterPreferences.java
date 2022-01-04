package org.jabref.logic.bibtex;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.field.Field;

public class FieldWriterPreferences {

    private final boolean doNotResolveStrings;
    private final List<Field> resolveStringsForFields;
    private final int lineLength = 65; // Constant
    private final FieldContentFormatterPreferences fieldContentFormatterPreferences;

    public FieldWriterPreferences(boolean resolveStringsAllFields, List<Field> doNotResolveStringsFor,
                                  FieldContentFormatterPreferences fieldContentFormatterPreferences) {
        this.doNotResolveStrings = resolveStringsAllFields;
        this.resolveStringsForFields = doNotResolveStringsFor;
        this.fieldContentFormatterPreferences = fieldContentFormatterPreferences;
    }

    /**
     * Creates an instance with default values (not obeying any user preferences). This constructor should be used with
     * caution. The other constructor has to be preferred.
     */
    public FieldWriterPreferences() {
        // This constructor is only to allow an empty constructor in SavePreferences
        this(true, Collections.emptyList(), new FieldContentFormatterPreferences());
    }

    public boolean isDoNotResolveStrings() {
        return doNotResolveStrings;
    }

    public List<Field> getResolveStringsForFields() {
        return resolveStringsForFields;
    }

    public int getLineLength() {
        return lineLength;
    }

    public FieldContentFormatterPreferences getFieldContentFormatterPreferences() {
        return fieldContentFormatterPreferences;
    }
}
