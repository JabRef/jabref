package org.jabref.preferences;

import org.jabref.gui.util.Theme;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;

public class AppearancePreferences {
    private final BooleanProperty shouldOverrideDefaultFontSize;
    private final IntegerProperty mainFontSize;
    private final ObjectProperty<Theme> theme;

    public AppearancePreferences(boolean shouldOverrideDefaultFontSize, int mainFontSize, Theme theme) {
        this.shouldOverrideDefaultFontSize = new BooleanProperty(shouldOverrideDefaultFontSize);
        this.mainFontSize = new IntegerProperty(mainFontSize);
        this.theme = new SimpleObjectProperty(theme);
    }

    public boolean shouldOverrideDefaultFontSize() {
        return shouldOverrideDefaultFontSize.get();
    }

    public boolean setShouldOverrideDefaultFontSize(boolean newValue) {
        return shouldOverrideDefaultFontSize.set(newValue);
    }

    public BooleanProperty shouldOverrideDefaultFontSizeProperty() {
        return shouldOverrideDefaultFontSize;
    }

    public int getMainFontSize() {
        return mainFontSize;
    }

    public Theme getTheme() {
        return theme;
    }
}
