package org.jabref.logic.xmp;

import java.util.HashSet;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class XmpPreferences {

    private static final Set<StandardField> DEFAULT_XMP_PRIVACY_FILTER = Set.of(
            StandardField.PDF,
            StandardField.TIMESTAMP,
            StandardField.KEYWORDS,
            StandardField.OWNER,
            StandardField.NOTE,
            StandardField.REVIEW);

    private final BooleanProperty useXmpPrivacyFilter;
    private final ObservableSet<Field> xmpPrivacyFilter;
    private final SimpleObjectProperty<Character> keywordSeparator;

    private XmpPreferences() {
        this(
                false,                                     // Don't use XMP privacy filter
                new HashSet<>(DEFAULT_XMP_PRIVACY_FILTER), // Default XMP privacy filter fields (mutable copy)
                new SimpleObjectProperty<>(',')            // Default keyword separator
        );
    }

    public XmpPreferences(boolean useXmpPrivacyFilter, Set<Field> xmpPrivacyFilter, ReadOnlyObjectProperty<Character> keywordSeparatorProperty) {
        this.useXmpPrivacyFilter = new SimpleBooleanProperty(useXmpPrivacyFilter);
        this.xmpPrivacyFilter = FXCollections.observableSet(new HashSet<>(xmpPrivacyFilter));
        this.keywordSeparator = new SimpleObjectProperty<>();
        this.keywordSeparator.bind(keywordSeparatorProperty);
    }

    public static XmpPreferences getDefault() {
        return new XmpPreferences();
    }

    public void setAll(XmpPreferences other) {
        this.useXmpPrivacyFilter.set(other.shouldUseXmpPrivacyFilter());
        this.xmpPrivacyFilter.clear();
        this.xmpPrivacyFilter.addAll(other.getXmpPrivacyFilter());
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
