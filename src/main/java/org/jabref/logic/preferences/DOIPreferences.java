package org.jabref.logic.preferences;

public class DOIPreferences {
    private final boolean useCustom;
    private final String defaultBaseURI;

    public DOIPreferences(boolean useCustom, String defaultBaseURI) {
        this.useCustom = useCustom;
        this.defaultBaseURI = defaultBaseURI;
    }

    public boolean isUseCustom() {
        return useCustom;
    }

    public String getDefaultBaseURI() {
        return defaultBaseURI;
    }
}
