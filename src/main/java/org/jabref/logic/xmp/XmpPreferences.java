package org.jabref.logic.xmp;

import java.util.Set;

import org.jabref.model.entry.field.Field;

public class XmpPreferences {

    private final boolean useXmpPrivacyFilter;
    private final Set<Field> xmpPrivacyFilter;
    private final Character keywordSeparator;

    public XmpPreferences(boolean useXmpPrivacyFilter, Set<Field> xmpPrivacyFilter, Character keywordSeparator) {
        this.useXmpPrivacyFilter = useXmpPrivacyFilter;
        this.xmpPrivacyFilter = xmpPrivacyFilter;
        this.keywordSeparator = keywordSeparator;
    }

    public boolean shouldUseXmpPrivacyFilter() {
        return useXmpPrivacyFilter;
    }

    public Set<Field> getXmpPrivacyFilter() {
        return xmpPrivacyFilter;
    }

    public Character getKeywordSeparator() {
        return keywordSeparator;
    }
}
