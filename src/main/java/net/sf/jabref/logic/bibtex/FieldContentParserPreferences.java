package net.sf.jabref.logic.bibtex;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.preferences.JabRefPreferences;

public class FieldContentParserPreferences {

    private final List<String> nonWrappableFields;


    public FieldContentParserPreferences() {
        // This constructor is only to allow an empty constructor in SavePreferences
        this.nonWrappableFields = Collections.emptyList();
    }

    public FieldContentParserPreferences(List<String> nonWrappableFields) {
        this.nonWrappableFields = nonWrappableFields;
    }

    public List<String> getNonWrappableFields() {
        return nonWrappableFields;
    }

    public static FieldContentParserPreferences fromPreferences(JabRefPreferences jabRefPreferences) {
        return new FieldContentParserPreferences(
                jabRefPreferences.getStringList(JabRefPreferences.NON_WRAPPABLE_FIELDS));
    }
}
