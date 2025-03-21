package org.jabref.logic.preferences;

import org.jabref.logic.l10n.Localization;

public enum AutoPushMode {
    ON_SAVE(Localization.lang("On save"));

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
        return ON_SAVE;
    }
}
