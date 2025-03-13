package org.jabref.logic.preferences;

public enum AutoPushMode {
    MANUALLY("Manually"),
    ON_SAVE("On Save");

    private final String displayName;

    AutoPushMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static AutoPushMode fromString(String text) {
        for (AutoPushMode mode : AutoPushMode.values()) {
            if (mode.displayName.equalsIgnoreCase(text)) {
                return mode;
            }
        }
        return MANUALLY;
    }
}
