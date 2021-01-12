package org.jabref.preferences;

import org.jabref.gui.theme.ThemePreference;

public class AppearancePreferences {
    private final boolean shouldOverrideDefaultFontSize;
    private final int mainFontSize;
    private final ThemePreference themePreference;

    public AppearancePreferences(boolean shouldOverrideDefaultFontSize, int mainFontSize, ThemePreference themePreference) {
        this.shouldOverrideDefaultFontSize = shouldOverrideDefaultFontSize;
        this.mainFontSize = mainFontSize;
        this.themePreference = themePreference;
    }

    public boolean shouldOverrideDefaultFontSize() {
        return shouldOverrideDefaultFontSize;
    }

    public int getMainFontSize() {
        return mainFontSize;
    }

    public ThemePreference getThemePreference() {
        return themePreference;
    }
}
