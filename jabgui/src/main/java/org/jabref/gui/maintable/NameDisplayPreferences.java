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


    private final ObjectProperty<DisplayStyle> displayStyle = new SimpleObjectProperty<>();
    private final ObjectProperty<AbbreviationStyle> abbreviationStyle = new SimpleObjectProperty<>();




    public NameDisplayPreferences(DisplayStyle displayStyle,
                                  AbbreviationStyle abbreviationStyle) {
        this.displayStyle.set(displayStyle);
        this.abbreviationStyle.set(abbreviationStyle);
    }
    private NameDisplayPreferences() {
        this(DisplayStyle.LASTNAME_FIRSTNAME, AbbreviationStyle.NONE);
    }
    public static NameDisplayPreferences getDefault() {
        return new NameDisplayPreferences();
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
    public void setAll(NameDisplayPreferences other) {
        setDisplayStyle(other.getDisplayStyle());
        setAbbreviationStyle(other.getAbbreviationStyle());
    }
}

