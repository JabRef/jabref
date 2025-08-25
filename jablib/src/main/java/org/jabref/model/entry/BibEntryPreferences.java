package org.jabref.model.entry;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class BibEntryPreferences {
    private final ObjectProperty<String> keywordSeparator;

    public BibEntryPreferences(String keywordSeparator) {
        this.keywordSeparator = new SimpleObjectProperty<>(keywordSeparator);
    }

    public String getKeywordSeparator() {
        return keywordSeparator.get();
    }

    public ObjectProperty<String> keywordSeparatorProperty() {
        return keywordSeparator;
    }

    public void setKeywordSeparator(String keywordSeparator) {
        this.keywordSeparator.set(keywordSeparator);
    }
}
