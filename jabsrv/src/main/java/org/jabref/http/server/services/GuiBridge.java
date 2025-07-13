package org.jabref.http.server.services;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class GuiBridge {

    private ObservableList<BibDatabaseContext> openDatabases = FXCollections.observableArrayList();
    private ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList();
    private final ObservableList<BibEntry> selectEntries = FXCollections.observableArrayList();
    private boolean runningInCli = false;

    public GuiBridge() {
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

    public void setOpenDatabases(ObservableList<BibDatabaseContext> openDatabases) {
        this.openDatabases = openDatabases;
    }

    public ObservableList<BibDatabaseContext> getOpenDatabases() {
        return openDatabases;
    }

    public boolean isRunningInCli() {
        return runningInCli;
    }

    public void setRunningInCli(boolean runningInCli) {
        this.runningInCli = runningInCli;
    }
}
