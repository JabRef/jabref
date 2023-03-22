package org.jabref.logic.journals;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public abstract class AbbreviationPreferences {

    protected ObservableList<String> externalLists;

    public AbbreviationPreferences(List<String> externalLists) {
        this.externalLists = FXCollections.observableArrayList(externalLists);
    }

    public ObservableList<String> getExternalLists() {
        return externalLists;
    }

    public void setExternalLists(List<String> list) {
        externalLists.clear();
        externalLists.addAll(list);
    }
}
