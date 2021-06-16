package org.jabref.logic.bibtex;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.field.Field;

public class FieldWriterPreferences {

    private final boolean resolveStringsAllFields;
    private final List<Field> doNotResolveStringsFor;
    private final int lineLength = 65; // Constant
    private final FieldContentFormatterPreferences fieldContentFormatterPreferences;

    public FieldWriterPreferences(boolean resolveStringsAllFields, List<Field> doNotResolveStringsFor,
                                  FieldContentFormatterPreferences fieldContentFormatterPreferences) {
        this.resolveStringsAllFields = resolveStringsAllFields;
        this.doNotResolveStringsFor = doNotResolveStringsFor;
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

    public boolean isResolveStringsAllFields() {
        return resolveStringsAllFields;
    }

    public List<Field> getDoNotResolveStringsFor() {
        return doNotResolveStringsFor;
    }

    public int getLineLength() {
        return lineLength;
    }

    public FieldContentFormatterPreferences getFieldContentFormatterPreferences() {
        return fieldContentFormatterPreferences;
    }
}
