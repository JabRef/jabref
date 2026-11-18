package org.jabref.logic.journals;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbbreviationPreferences {
    private final ObservableList<String> externalLists;

    protected AbbreviationPreferences(List<String> externalLists) {
        this.externalLists = FXCollections.observableArrayList(externalLists);
    }

    public ObservableList<String> getExternalLists() {
        return externalLists;
    }

    public void setExternalLists(List<String> externalLists) {
        this.externalLists.setAll(externalLists);
    }
}
