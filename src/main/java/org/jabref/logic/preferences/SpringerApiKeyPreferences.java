package org.jabref.logic.preferences;

public class SpringerApiKeyPreferences {
    private final boolean useCustom;
    private final String defaultApiKey;

    public SpringerApiKeyPreferences(boolean useCustom, String defaultApiKey) {
        this.useCustom = useCustom;
        this.defaultApiKey = defaultApiKey;
    }

    public boolean isUseCustom() {
        return useCustom;
    }

    public String getDefaultApiKey() {
        return defaultApiKey;
    }

}
