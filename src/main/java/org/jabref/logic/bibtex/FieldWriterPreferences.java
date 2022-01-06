package org.jabref.logic.bibtex;

import java.util.List;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class FieldWriterPreferences {

    private final boolean doNotResolveStrings;
    private final List<Field> resolveStringsForFields;
    private final int lineLength = 65; // Constant
    private final FieldContentFormatterPreferences fieldContentFormatterPreferences;

    public FieldWriterPreferences(boolean doNotResolveFields, List<Field> resolveStringsForFields,
                                  FieldContentFormatterPreferences fieldContentFormatterPreferences) {
        this.doNotResolveStrings = doNotResolveFields;
        this.resolveStringsForFields = resolveStringsForFields;
        this.fieldContentFormatterPreferences = fieldContentFormatterPreferences;
    }

    /**
     * ONLY USE IN TEST: ONLY resolves BibTex String for the Field Month
     * Creates an instance with default values (not obeying any user preferences). This constructor should be used with
     * caution. The other constructor has to be preferred.
     * @deprecated
     */
    @Deprecated
    public FieldWriterPreferences() {
        this(false, List.of(StandardField.MONTH), new FieldContentFormatterPreferences());
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
