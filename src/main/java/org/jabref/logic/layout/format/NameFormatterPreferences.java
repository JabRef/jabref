package org.jabref.logic.layout.format;

import java.util.List;

public class NameFormatterPreferences {

    private final List<String> nameFormatterKey;
    private final List<String> nameFormatterValue;


    public NameFormatterPreferences(List<String> nameFormatterKey, List<String> nameFormatterValue) {
        this.nameFormatterKey = nameFormatterKey;
        this.nameFormatterValue = nameFormatterValue;
    }

    public List<String> getNameFormatterKey() {
        return nameFormatterKey;
    }

    public List<String> getNameFormatterValue() {
        return nameFormatterValue;
    }
}
