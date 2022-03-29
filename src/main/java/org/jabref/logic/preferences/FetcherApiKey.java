package org.jabref.logic.preferences;

public class FetcherApiKey {
    private final String name;
    private boolean useCustom;
    private String customApiKey;

    public FetcherApiKey(String name, boolean useCustom, String customApiKey) {
        this.name = name;
        this.useCustom = useCustom;
        this.customApiKey = customApiKey;
    }

    public String getName() {
        return name;
    }

    public boolean shouldUseCustom() {
        return useCustom;
    }

    public String getCustomApiKey() {
        return customApiKey;
    }

    public void shouldUseCustomKey(boolean useCustom) {
        this.useCustom = useCustom;
    }

    public void setCustomApiKey(String customApiKey) {
        this.customApiKey = customApiKey;
    }
}
