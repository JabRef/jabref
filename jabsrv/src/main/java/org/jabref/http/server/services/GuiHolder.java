package org.jabref.http.server.services;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class GuiHolder {

    private ObservableList<BibDatabaseContext> contextsToServe;
    private ObservableList<BibEntry> selectedEntries;
    private final ObservableList<BibEntry> selectEntries = FXCollections.observableArrayList();

    public GuiHolder() {
    }

    public void setSelectedEntries(ObservableList<BibEntry> selectedEntries) {
        this.selectedEntries = selectedEntries;
    }

    public ObservableList<BibEntry> getSelectedEntries() {
        return selectedEntries;
    }

    public void setSelectEntries(List<BibEntry> selectEntries) {
        this.selectEntries.setAll(selectEntries);
    }

    public ObservableList<BibEntry> getSelectEntries() {
        return selectEntries;
    }

    public void setContextsToServe(ObservableList<BibDatabaseContext> contextsToServe) {
        this.contextsToServe = contextsToServe;
    }

    public ObservableList<BibDatabaseContext> getContextsToServe() {
        return contextsToServe;
    }
}
