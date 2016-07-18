package net.sf.jabref.logic.bibtex;

import java.util.List;

import net.sf.jabref.JabRefPreferences;

public class LatexFieldFormatterPreferences {

    private final boolean resolveStringsAllFields;
    private final char valueDelimiterStartOfValue;
    private final char valueDelimiterEndOfValue;
    private final List<String> doNotResolveStringsFor;
    private final int lineLength;
    private final FieldContentParserPreferences fieldContentParserPreferences;


    public LatexFieldFormatterPreferences(boolean resolveStringsAllFields, char valueDelimiterStartOfValue,
            char valueDelimiterEndOfValue, List<String> doNotResolveStringsFor, int lineLength,
            FieldContentParserPreferences fieldContentParserPreferences) {
        this.resolveStringsAllFields = resolveStringsAllFields;
        this.valueDelimiterStartOfValue = valueDelimiterStartOfValue;
        this.valueDelimiterEndOfValue = valueDelimiterEndOfValue;
        this.doNotResolveStringsFor = doNotResolveStringsFor;
        this.lineLength = lineLength;
        this.fieldContentParserPreferences = fieldContentParserPreferences;
    }

    public static LatexFieldFormatterPreferences fromPreferences(JabRefPreferences prefs) {
        return new LatexFieldFormatterPreferences(prefs.getBoolean(JabRefPreferences.RESOLVE_STRINGS_ALL_FIELDS),
                prefs.getValueDelimiters(0), prefs.getValueDelimiters(1),
                prefs.getStringList(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR),
                prefs.getInt(JabRefPreferences.LINE_LENGTH), FieldContentParserPreferences.fromPreferences(prefs));
    }

    public boolean isResolveStringsAllFields() {
        return resolveStringsAllFields;
    }

    public char getValueDelimiterStartOfValue() {
        return valueDelimiterStartOfValue;
    }

    public char getValueDelimiterEndOfValue() {
        return valueDelimiterEndOfValue;
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
