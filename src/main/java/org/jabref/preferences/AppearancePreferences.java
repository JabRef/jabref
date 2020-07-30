package org.jabref.preferences;

public class AppearancePreferences {
    private final boolean shouldOverrideDefaultFontSize;
    private final int mainFontSize;
    private final String pathToTheme;

    public AppearancePreferences(boolean shouldOverrideDefaultFontSize, int mainFontSize, String pathToTheme) {
        this.shouldOverrideDefaultFontSize = shouldOverrideDefaultFontSize;
        this.mainFontSize = mainFontSize;
        this.pathToTheme = pathToTheme;
    }

    public boolean shouldOverrideDefaultFontSize() {
        return shouldOverrideDefaultFontSize;
    }

    public int getMainFontSize() {
        return mainFontSize;
    }

    public String getPathToTheme() {
        return pathToTheme;
    }
}
