package org.jabref.preferences;

import org.jabref.gui.util.Theme;

public class AppearancePreferences {
    private final boolean shouldOverrideDefaultFontSize;
    private final int mainFontSize;
    private final Theme theme;

    public AppearancePreferences(boolean shouldOverrideDefaultFontSize, int mainFontSize, Theme theme) {
        this.shouldOverrideDefaultFontSize = shouldOverrideDefaultFontSize;
        this.mainFontSize = mainFontSize;
        this.theme = theme;
    }

    public boolean shouldOverrideDefaultFontSize() {
        return shouldOverrideDefaultFontSize;
    }

    public int getMainFontSize() {
        return mainFontSize;
    }

    public Theme getTheme() {
        return theme;
    }
}
