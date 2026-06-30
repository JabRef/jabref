package org.jabref.model.entry;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class BibEntryPreferences {

    private final ObjectProperty<Character> keywordSeparator;

    private BibEntryPreferences() {
        this(
                ','  // Keyword separator
        );
    }

    public BibEntryPreferences(Character keywordSeparator) {
        this.keywordSeparator = new SimpleObjectProperty<>(keywordSeparator);
    }

    public static BibEntryPreferences getDefault() {
        return new BibEntryPreferences();
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
