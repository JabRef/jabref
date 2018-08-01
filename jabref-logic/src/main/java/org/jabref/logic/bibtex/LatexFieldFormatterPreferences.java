package org.jabref.logic.bibtex;

import java.util.Collections;
import java.util.List;

public class LatexFieldFormatterPreferences {

    private final boolean resolveStringsAllFields;
    private final List<String> doNotResolveStringsFor;
    private final int lineLength = 65; // Constant
    private final FieldContentParserPreferences fieldContentParserPreferences;


    public LatexFieldFormatterPreferences(boolean resolveStringsAllFields, List<String> doNotResolveStringsFor,
            FieldContentParserPreferences fieldContentParserPreferences) {
        this.resolveStringsAllFields = resolveStringsAllFields;
        this.doNotResolveStringsFor = doNotResolveStringsFor;
        this.fieldContentParserPreferences = fieldContentParserPreferences;
    }

    public LatexFieldFormatterPreferences() {
        // This constructor is only to allow an empty constructor in SavePreferences
        this(true, Collections.emptyList(), new FieldContentParserPreferences());
    }

    public boolean isResolveStringsAllFields() {
        return resolveStringsAllFields;
    }

    public List<String> getDoNotResolveStringsFor() {
        return doNotResolveStringsFor;
    }

    public int getLineLength() {
        return lineLength;
    }

    public FieldContentParserPreferences getFieldContentParserPreferences() {
        return fieldContentParserPreferences;
    }
}
