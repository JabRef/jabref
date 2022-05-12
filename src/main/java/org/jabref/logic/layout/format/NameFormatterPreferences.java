package org.jabref.logic.layout.format;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class NameFormatterPreferences {

    private final ObservableList<String> nameFormatterKey;
    private final ObservableList<String> nameFormatterValue;

    public NameFormatterPreferences(List<String> nameFormatterKey, List<String> nameFormatterValue) {
        this.nameFormatterKey = FXCollections.observableList(nameFormatterKey);
        this.nameFormatterValue = FXCollections.observableList(nameFormatterValue);
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
