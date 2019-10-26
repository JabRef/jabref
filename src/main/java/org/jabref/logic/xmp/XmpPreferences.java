package org.jabref.logic.xmp;

import java.util.Set;

import org.jabref.model.entry.field.Field;

public class XmpPreferences {

    private final boolean useXMPPrivacyFilter;
    private final Set<Field> xmpPrivacyFilter;
    private final Character keywordSeparator;

    public XmpPreferences(boolean useXMPPrivacyFilter, Set<Field> xmpPrivacyFilter, Character keywordSeparator) {
        this.useXMPPrivacyFilter = useXMPPrivacyFilter;
        this.xmpPrivacyFilter = xmpPrivacyFilter;
        this.keywordSeparator = keywordSeparator;
    }

    public boolean isUseXMPPrivacyFilter() {
        return useXMPPrivacyFilter;
    }

    public Set<Field> getXmpPrivacyFilter() {
        return xmpPrivacyFilter;
    }

    public Character getKeywordSeparator() {
        return keywordSeparator;
    }
}
