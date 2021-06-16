package org.jabref.logic.preferences;

public class OwnerPreferences {
    private final boolean useOwner;
    private final String defaultOwner;
    private final boolean overwriteOwner;

    public OwnerPreferences(boolean useOwner,
                            String defaultOwner,
                            boolean overwriteOwner) {
        this.useOwner = useOwner;
        this.defaultOwner = defaultOwner;
        this.overwriteOwner = overwriteOwner;
    }

    public boolean isUseOwner() {
        return useOwner;
    }

    public String getDefaultOwner() {
        return defaultOwner;
    }

    public boolean isOverwriteOwner() {
        return overwriteOwner;
    }
}
