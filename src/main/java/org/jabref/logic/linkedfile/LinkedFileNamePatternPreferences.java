package org.jabref.logic.linkedfile;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class LinkedFileNamePatternPreferences {

    private final ObjectProperty<GlobalLinkedFileNamePatterns> namePatterns = new SimpleObjectProperty<>();
    private final String defaultPattern;

    public LinkedFileNamePatternPreferences(String defaultPattern) {
        this.defaultPattern = defaultPattern;
    }

    public GlobalLinkedFileNamePatterns getNamePatterns() {
        return namePatterns.get();
    }

    public ObjectProperty<GlobalLinkedFileNamePatterns> namePatternsProperty() {
        return namePatterns;
    }

    public void setNamePatterns(GlobalLinkedFileNamePatterns namePatterns) {
        this.namePatterns.set(namePatterns);
    }

    public String getDefaultPattern() {
        return defaultPattern;
    }
}
