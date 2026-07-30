package org.jabref.gui.theme;

import org.jabref.logic.l10n.Localization;

public enum ThemeColorScheme {
    FOLLOW_SYSTEM(Localization.lang("Follow System")),
    LIGHT(Localization.lang("Light")),
    DARK(Localization.lang("Dark"));

    private final String colorSchemeName;

    ThemeColorScheme(String colorSchemeName) {
        this.colorSchemeName = colorSchemeName;
    }

    public static ThemeColorScheme of(String colorScheme) {
        if (colorScheme == null) {
            return FOLLOW_SYSTEM;
        }

        try {
            return valueOf(colorScheme);
        } catch (IllegalArgumentException e) {
            return FOLLOW_SYSTEM;
        }
    }

    public String getLocalizedName() {
        return colorSchemeName;
    }

    public String getPreferenceName() {
        return name();
    }
}
