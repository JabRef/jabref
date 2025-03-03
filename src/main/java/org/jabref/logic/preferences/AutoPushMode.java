package org.jabref.logic.preferences;

public enum AutoPushMode {
    MANUALLY("Manually"),
    AUTOMATICALLY("Automatically");

    private final String mode;

    AutoPushMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    public static AutoPushMode fromString(String text) {
        for (AutoPushMode mode : AutoPushMode.values()) {
            if (mode.mode.equalsIgnoreCase(text)) {
                return mode;
            }
        }
        return MANUALLY; // Default mode
    }
}
