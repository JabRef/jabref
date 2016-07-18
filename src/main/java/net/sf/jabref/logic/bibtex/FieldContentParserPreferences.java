package net.sf.jabref.logic.bibtex;

import java.util.List;

import net.sf.jabref.JabRefPreferences;

public class FieldContentParserPreferences {

    private final List<String> nonWrappableFields;


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
