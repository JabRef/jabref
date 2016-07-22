package net.sf.jabref.logic.layout.format;

import java.util.List;

import net.sf.jabref.preferences.JabRefPreferences;

public class NameFormatterPreferences {

    private final List<String> nameFormatterKey;
    private final List<String> nameFormatterValue;


    public NameFormatterPreferences(List<String> nameFormatterKey, List<String> nameFormatterValue) {
        this.nameFormatterKey = nameFormatterKey;
        this.nameFormatterValue = nameFormatterValue;
    }

    public static NameFormatterPreferences fromPreferences(JabRefPreferences jabRefPreferences) {
        return new NameFormatterPreferences(jabRefPreferences.getStringList(NameFormatter.NAME_FORMATER_KEY),
                jabRefPreferences.getStringList(NameFormatter.NAME_FORMATTER_VALUE));
    }

    public List<String> getNameFormatterKey() {
        return nameFormatterKey;
    }


    public List<String> getNameFormatterValue() {
        return nameFormatterValue;
    }
}
