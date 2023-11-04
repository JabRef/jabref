package org.jabref.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class BibEntryPreferences {
    private final ObjectProperty<Character> keywordSeparator;

    public BibEntryPreferences(Character keywordSeparator) {
        this.keywordSeparator = new SimpleObjectProperty<>(keywordSeparator);
    }

    public Character getKeywordSeparator() {
        return keywordSeparator.get();
    }

    public ObjectProperty<Character> keywordSeparatorProperty() {
        return keywordSeparator;
    }

    public void setKeywordSeparator(Character keywordSeparator) {
        this.keywordSeparator.set(keywordSeparator);
    }
}
