package org.jabref.gui.maintable;

public class MainTableNameFormatPreferences {

    public enum DisplayStyle {
        NATBIB, AS_IS, FIRSTNAME_LASTNAME, LASTNAME_FIRSTNAME
    }

    public enum AbbreviationStyle {
        NONE, LASTNAME_ONLY, FULL
    }

    private final DisplayStyle displayStyle;
    private final AbbreviationStyle abbreviationStyle;

    public MainTableNameFormatPreferences(DisplayStyle displayStyle,
                                          AbbreviationStyle abbreviationStyle) {

        this.displayStyle = displayStyle;
        this.abbreviationStyle = abbreviationStyle;
    }

    public DisplayStyle getDisplayStyle() {
        return displayStyle;
    }

    public AbbreviationStyle getAbbreviationStyle() {
        return abbreviationStyle;
    }
}
