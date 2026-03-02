package org.jabref.logic.journals;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// Abstract base class for abbreviation preferences (journals and conferences)
public abstract class AbbreviationPreferences {

    protected final ObservableList<String> externalLists;

    protected AbbreviationPreferences(List<String> externalLists) {
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
