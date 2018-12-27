package org.jabref.logic.xmp;

import java.util.List;

public class XmpPreferences {

    private final boolean useXMPPrivacyFilter;
    private final List<String> xmpPrivacyFilter;
    private final Character keywordSeparator;


    public XmpPreferences(boolean useXMPPrivacyFilter, List<String> xmpPrivacyFilter, Character keywordSeparator) {
        this.useXMPPrivacyFilter = useXMPPrivacyFilter;
        this.xmpPrivacyFilter = xmpPrivacyFilter;
        this.keywordSeparator = keywordSeparator;
    }

    public boolean isUseXMPPrivacyFilter() {
        return useXMPPrivacyFilter;
    }

    public List<String> getXmpPrivacyFilter() {
        return xmpPrivacyFilter;
    }

    public Character getKeywordSeparator() {
        return keywordSeparator;
    }
}
