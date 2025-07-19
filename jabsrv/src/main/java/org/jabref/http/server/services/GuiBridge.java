package org.jabref.http.server.services;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class GuiBridge {

    private ObservableList<BibDatabaseContext> openDatabases = FXCollections.observableArrayList();
    private ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList();
    private final ObjectProperty<Pair<BibDatabaseContext, List<BibEntry>>> selectEntries = new SimpleObjectProperty<>();
    private BibDatabaseContext activeDatabase;
    private boolean runningInCli = false;

    public void setSelectedEntries(ObservableList<BibEntry> selectedEntries) {
        this.selectedEntries = selectedEntries;
    }

    public ObservableList<BibEntry> getSelectedEntries() {
        return selectedEntries;
    }

    public void setSelectEntries(BibDatabaseContext context, List<BibEntry> selectEntries) {
        this.selectEntries.setValue(new Pair<>(context, selectEntries));
    }

    public ObservableObjectValue<Pair<BibDatabaseContext, List<BibEntry>>> getSelectEntries() {
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

    public Optional<BibDatabaseContext> getActiveDatabase() {
        return Optional.ofNullable(activeDatabase);
    }

    public void setActiveDatabase(BibDatabaseContext activeDatabase) {
        this.activeDatabase = activeDatabase;
    }
}
