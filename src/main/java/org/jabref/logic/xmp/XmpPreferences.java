package org.jabref.logic.xmp;

import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.model.entry.field.Field;

public class XmpPreferences {

    private final BooleanProperty useXmpPrivacyFilter;
    private final ObservableSet<Field> xmpPrivacyFilter;
    private final ObjectProperty<Character> keywordSeparator;

    public XmpPreferences(boolean useXmpPrivacyFilter, Set<Field> xmpPrivacyFilter, ObjectProperty<Character> keywordSeparator) {
        this.useXmpPrivacyFilter = new SimpleBooleanProperty(useXmpPrivacyFilter);
        this.xmpPrivacyFilter = FXCollections.observableSet(xmpPrivacyFilter);
        this.keywordSeparator = keywordSeparator;
    }

    public boolean shouldUseXmpPrivacyFilter() {
        return useXmpPrivacyFilter.getValue();
    }

    public BooleanProperty useXmpPrivacyFilterProperty() {
        return useXmpPrivacyFilter;
    }

    public void setUseXmpPrivacyFilter(boolean useXmpPrivacyFilter) {
        this.useXmpPrivacyFilter.set(useXmpPrivacyFilter);
    }

    public ObservableSet<Field> getXmpPrivacyFilter() {
        return xmpPrivacyFilter;
    }

    public Character getKeywordSeparator() {
        return keywordSeparator.getValue();
    }
}
