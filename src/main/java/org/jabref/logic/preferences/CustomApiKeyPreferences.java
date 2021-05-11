package org.jabref.logic.preferences;

public class CustomApiKeyPreferences {
    private final String name;
    private boolean useCustom;
    private String defaultApiKey;

    public CustomApiKeyPreferences(String name, boolean useCustom, String defaultApiKey) {
        this.name = name;
        this.useCustom = useCustom;
        this.defaultApiKey = defaultApiKey;
    }

    public String getName() {
        return name;
    }

    public boolean isUseCustom() {
        return useCustom;
    }

    public String getDefaultApiKey() {
        return defaultApiKey;
    }

    public void useCustom(boolean useCustom) {
        this.useCustom = useCustom;
    }

    public void setDefaultApiKey(String defaultApiKey) {
        this.defaultApiKey = defaultApiKey;
    }

}
