package org.jabref.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.util.Theme;

public class AppearancePreferences {
    private final BooleanProperty shouldOverrideDefaultFontSize;
    private final IntegerProperty mainFontSize;
    private final ObjectProperty<Theme> theme;

    public AppearancePreferences(boolean shouldOverrideDefaultFontSize, int mainFontSize, Theme theme) {
        this.shouldOverrideDefaultFontSize = new SimpleBooleanProperty(shouldOverrideDefaultFontSize);
        this.mainFontSize = new SimpleIntegerProperty(mainFontSize);
        this.theme = new SimpleObjectProperty<>(theme);
    }

    public boolean shouldOverrideDefaultFontSize() {
        return shouldOverrideDefaultFontSize.get();
    }

    public void setShouldOverrideDefaultFontSize(boolean newValue) {
        shouldOverrideDefaultFontSize.set(newValue);
    }

    public BooleanProperty shouldOverrideDefaultFontSizeProperty() {
        return shouldOverrideDefaultFontSize;
    }

    public int getMainFontSize() {
        return mainFontSize.get();
    }

    public void setMainFontSize(int mainFontSize) {
        this.mainFontSize.set(mainFontSize);
    }

    public IntegerProperty mainFontSizeProperty() {
        return mainFontSize;
    }

    public Theme getTheme() {
        return theme.get();
    }

    public void setTheme(Theme theme) {
        this.theme.set(theme);
    }

    public ObjectProperty<Theme> themeProperty() {
        return theme;
    }
}
