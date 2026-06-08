package org.jabref.logic.layout.format;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class NameFormatterPreferences {

    private static final List<String> DEFAULT_NAME_FORMATTER_KEY = List.of();   // No default name formatter keys
    private static final List<String> DEFAULT_NAME_FORMATTER_VALUE = List.of(); // No default name formatter values

    private final ObservableList<String> nameFormatterKey;
    private final ObservableList<String> nameFormatterValue;

    private NameFormatterPreferences() {
        this(
                DEFAULT_NAME_FORMATTER_KEY,   // Name formatter keys
                DEFAULT_NAME_FORMATTER_VALUE  // Name formatter values
        );
    }

    public NameFormatterPreferences(List<String> nameFormatterKey, List<String> nameFormatterValue) {
        this.nameFormatterKey = FXCollections.observableArrayList(nameFormatterKey);
        this.nameFormatterValue = FXCollections.observableArrayList(nameFormatterValue);
    }

    public static NameFormatterPreferences getDefault() {
        return new NameFormatterPreferences();
    }

    public void setAll(NameFormatterPreferences preferences) {
        this.nameFormatterKey.setAll(preferences.getNameFormatterKey());
        this.nameFormatterValue.setAll(preferences.getNameFormatterValue());
    }

    public ObservableList<String> getNameFormatterKey() {
        return nameFormatterKey;
    }

    public ObservableList<String> getNameFormatterValue() {
        return nameFormatterValue;
    }

    public void setNameFormatterKey(List<String> list) {
        nameFormatterKey.clear();
        nameFormatterKey.addAll(list);
    }

    public void setNameFormatterValue(List<String> list) {
        nameFormatterValue.clear();
        nameFormatterValue.addAll(list);
    }
}
