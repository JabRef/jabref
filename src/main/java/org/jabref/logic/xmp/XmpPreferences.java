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
    private final BooleanProperty enableEnclosingBracketsFilter;

    private final ObservableSet<Field> xmpPrivacyFilter;
    private final ObjectProperty<Character> keywordSeparator;

    public XmpPreferences(boolean useXmpPrivacyFilter, Set<Field> xmpPrivacyFilter, ObjectProperty<Character> keywordSeparator, boolean enableEnclosingBracketsFilter) {
        this.useXmpPrivacyFilter = new SimpleBooleanProperty(useXmpPrivacyFilter);
        this.xmpPrivacyFilter = FXCollections.observableSet(xmpPrivacyFilter);
        this.enableEnclosingBracketsFilter = new SimpleBooleanProperty(enableEnclosingBracketsFilter);
        this.keywordSeparator = keywordSeparator;
    }

    public boolean shouldUseXmpPrivacyFilter() {
        return useXmpPrivacyFilter.getValue();
    }
    public boolean shouldEnableEnclosingBracketsFilter(){
        return enableEnclosingBracketsFilter.getValue();
    }


    public BooleanProperty useXmpPrivacyFilterProperty() {
        return useXmpPrivacyFilter;
    }
    public BooleanProperty enableEnclosingBracketsFilterProperty() {
        return enableEnclosingBracketsFilter;
    }

    public void setUseXmpPrivacyFilter(boolean useXmpPrivacyFilter) {
        this.useXmpPrivacyFilter.set(useXmpPrivacyFilter);
    }
    public void setEnableEnclosingBracketsFilter(boolean enableEnclosingBracketsFilter) {
        this.enableEnclosingBracketsFilter.set(enableEnclosingBracketsFilter);
    }

    public ObservableSet<Field> getXmpPrivacyFilter() {
        return xmpPrivacyFilter;
    }

    public Character getKeywordSeparator() {
        return keywordSeparator.getValue();
    }
}
