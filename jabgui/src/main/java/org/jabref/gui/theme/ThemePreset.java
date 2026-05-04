package org.jabref.gui.theme;

import org.jabref.logic.l10n.Localization;

public enum ThemePreset {
    JABREF(Localization.lang("JabRef Theme"), "jabref-theme.css"),
    ATLANTA_PRIMER(Localization.lang("Atlanta Primer"), "atlanta-primer-javafx.css");

    private final String themeName;
    private final String css;

    private StyleSheet styleSheet;

    ThemePreset(String themeName, String css) {
        this.themeName = themeName;
        this.css = css;
    }

    public static ThemePreset of(String themePreset) {
        if (themePreset == null) {
            return JABREF;
        }

        try {
            return valueOf(themePreset);
        } catch (IllegalArgumentException e) {
            return JABREF;
        }
    }

    public String getPreferenceName() {
        return name();
    }

    public String getLocalizedName() {
        return themeName;
    }

    public StyleSheet getStyleSheet() {
        if (styleSheet == null) {
            styleSheet = StyleSheet.create(css).orElseThrow();
        }
        return styleSheet;
    }
}
