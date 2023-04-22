package org.jabref.logic.bibtex;

import java.util.List;

import org.jabref.model.entry.field.Field;

public class FieldPreferences {

    private final boolean resolveStrings;
    private final List<Field> resolveStringsForFields;
    private final List<Field> nonWrappableFields;

    /**
     * @param resolveStrings true - The character {@link FieldWriter#BIBTEX_STRING_START_END_SYMBOL} should be interpreted as indicator of BibTeX strings
     */
    public FieldPreferences(boolean resolveStrings,
                            List<Field> resolveStringsForFields,
                            List<Field> nonWrappableFields) {
        this.resolveStrings = resolveStrings;
        this.resolveStringsForFields = resolveStringsForFields;
        this.nonWrappableFields = nonWrappableFields;
    }

    public boolean shouldResolveStrings() {
        return resolveStrings;
    }

    public List<Field> getResolveStringsForFields() {
        return resolveStringsForFields;
    }

    public List<Field> getNonWrappableFields() {
        return nonWrappableFields;
    }
}
