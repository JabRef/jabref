package org.jabref.preferences;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AppearancePreferences that = (AppearancePreferences) o;
        return shouldOverrideDefaultFontSize == that.shouldOverrideDefaultFontSize &&
                mainFontSize == that.mainFontSize &&
                themePreference.equals(that.themePreference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shouldOverrideDefaultFontSize, mainFontSize, themePreference);
    }
}
