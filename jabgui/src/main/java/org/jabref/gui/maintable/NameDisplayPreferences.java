package org.jabref.gui.maintable;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class NameDisplayPreferences {

    public enum DisplayStyle {
        NATBIB, AS_IS, FIRSTNAME_LASTNAME, LASTNAME_FIRSTNAME
    }

    public enum AbbreviationStyle {
        NONE, LASTNAME_ONLY, FULL
    }

    // Default values (used for reset)
    public static final DisplayStyle DEFAULT_DISPLAY_STYLE = DisplayStyle.NATBIB;
    public static final AbbreviationStyle DEFAULT_ABBREVIATION_STYLE = AbbreviationStyle.FULL;

    private final ObjectProperty<DisplayStyle> displayStyle =
            new SimpleObjectProperty<>(DEFAULT_DISPLAY_STYLE);
    private final ObjectProperty<AbbreviationStyle> abbreviationStyle =
            new SimpleObjectProperty<>(DEFAULT_ABBREVIATION_STYLE);

    public NameDisplayPreferences(DisplayStyle displayStyle,
                                  AbbreviationStyle abbreviationStyle) {
        this.displayStyle.set(displayStyle);
        this.abbreviationStyle.set(abbreviationStyle);
    }

    public DisplayStyle getDisplayStyle() {
        return displayStyle.get();
    }

    public ObjectProperty<DisplayStyle> displayStyleProperty() {
        return displayStyle;
    }

    public void setDisplayStyle(DisplayStyle displayStyle) {
        this.displayStyle.set(displayStyle);
    }

    public AbbreviationStyle getAbbreviationStyle() {
        return abbreviationStyle.get();
    }

    public ObjectProperty<AbbreviationStyle> abbreviationStyleProperty() {
        return abbreviationStyle;
    }

    public void setAbbreviationStyle(AbbreviationStyle abbreviationStyle) {
        this.abbreviationStyle.set(abbreviationStyle);
    }

    public void resetToDefaults() {
        setDisplayStyle(DEFAULT_DISPLAY_STYLE);
        setAbbreviationStyle(DEFAULT_ABBREVIATION_STYLE);
    }
}
