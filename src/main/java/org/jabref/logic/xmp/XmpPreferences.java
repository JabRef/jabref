package org.jabref.logic.xmp;

import java.util.List;

import org.jabref.model.entry.field.Field;

public class XmpPreferences {

    private final boolean useXMPPrivacyFilter;
    private final List<Field> xmpPrivacyFilter;
    private final Character keywordSeparator;

    public XmpPreferences(boolean useXMPPrivacyFilter, List<Field> xmpPrivacyFilter, Character keywordSeparator) {
        this.useXMPPrivacyFilter = useXMPPrivacyFilter;
        this.xmpPrivacyFilter = xmpPrivacyFilter;
        this.keywordSeparator = keywordSeparator;
    }

    public boolean isUseXMPPrivacyFilter() {
        return useXMPPrivacyFilter;
    }

    public List<Field> getXmpPrivacyFilter() {
        return xmpPrivacyFilter;
    }

    public Character getKeywordSeparator() {
        return keywordSeparator;
    }
}
