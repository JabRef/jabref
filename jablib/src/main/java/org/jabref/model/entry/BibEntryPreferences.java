package org.jabref.model.entry;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class BibEntryPreferences {
    private final ObjectProperty<Character> keywordSeparator;
    private final ObjectProperty<String> multipleKeywordSeparator;

    public BibEntryPreferences(Character keywordSeparator) {
        this.keywordSeparator = new SimpleObjectProperty<>(keywordSeparator);
        this.multipleKeywordSeparator = new SimpleObjectProperty<String>(",");
    }

    public BibEntryPreferences(String multipleKeywordSeparator) {
        this.multipleKeywordSeparator = new SimpleObjectProperty<String>(multipleKeywordSeparator);
        this.keywordSeparator = new SimpleObjectProperty<>(',');
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

    public String getMultipleKeywordSeparator() {
        return multipleKeywordSeparator.get();
    }

    public ObjectProperty<String> multipleKeywordSeparatorProperty() {
        return multipleKeywordSeparator;
    }

    public void setMultipleKeywordSeparator(String multipleKeywordSeparator) {
        this.multipleKeywordSeparator.set(multipleKeywordSeparator);
    }
}
