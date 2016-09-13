package net.sf.jabref.logic.xmp;

import java.util.List;

public class XMPPreferences {

    private final boolean useXMPPrivacyFilter;
    private final List<String> xmpPrivacyFilter;
    private final String keywordSeparator;


    public XMPPreferences(boolean useXMPPrivacyFilter, List<String> xmpPrivacyFilter, String keywordSeparator) {
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

    public String getKeywordSeparator() {
        return keywordSeparator;
    }
}
