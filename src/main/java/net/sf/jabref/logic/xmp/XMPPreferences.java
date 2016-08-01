package net.sf.jabref.logic.xmp;

import java.util.List;

import net.sf.jabref.preferences.JabRefPreferences;

public class XMPPreferences {

    private final boolean useXMPPrivacyFilter;
    private final List<String> xmpPrivacyFilter;
    private final String keywordSeparator;


    public XMPPreferences(boolean useXMPPrivacyFilter, List<String> xmpPrivacyFilter, String keywordSeparator) {
        this.useXMPPrivacyFilter = useXMPPrivacyFilter;
        this.xmpPrivacyFilter = xmpPrivacyFilter;
        this.keywordSeparator = keywordSeparator;
    }

    public static XMPPreferences fromPreferences(JabRefPreferences jabrefPreferences) {
        return new XMPPreferences(jabrefPreferences.getBoolean(JabRefPreferences.USE_XMP_PRIVACY_FILTER),
                jabrefPreferences.getStringList(JabRefPreferences.XMP_PRIVACY_FILTERS),
                jabrefPreferences.get(JabRefPreferences.KEYWORD_SEPARATOR));
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
