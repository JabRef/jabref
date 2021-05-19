package org.jabref.logic.preferences;

public class CustomApiKeyPreferences {
    private final String name;
    private boolean useCustom;
    private String customApiKey;

    public CustomApiKeyPreferences(String name, boolean useCustom, String customApiKey) {
        this.name = name;
        this.useCustom = useCustom;
        this.customApiKey = customApiKey;
    }

    public String getName() {
        return name;
    }

    public boolean isUseCustom() {
        return useCustom;
    }

    public String getCustomApiKey() {
        return customApiKey;
    }

    public void useCustom(boolean useCustom) {
        this.useCustom = useCustom;
    }

    public void setCustomApiKey(String customApiKey) {
        this.customApiKey = customApiKey;
    }

}
