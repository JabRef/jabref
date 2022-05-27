package org.jabref.gui.autocompleter;

import javafx.util.StringConverter;

public class StringStringConverter extends StringConverter<String> {

    public StringStringConverter() { }

    @Override
    public String toString(String string) {
        return string;
    }

    @Override
    public String fromString(String string) {
        return string;
    }
}
