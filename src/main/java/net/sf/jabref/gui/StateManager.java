package net.sf.jabref.gui;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.groups.GroupTreeNode;

/**
 * This class manages the GUI-state of JabRef, including:
 * - currently selected database
 * - currently selected group
 * Coming soon:
 * - open databases
 * - active search
 */
public class StateManager {

    private final ObjectProperty<Optional<BibDatabaseContext>> activeDatabase = new SimpleObjectProperty<>(Optional.empty());
    private final ObjectProperty<Optional<GroupTreeNode>> activeGroup = new SimpleObjectProperty<>(Optional.empty());
    private final ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList();

    public ObjectProperty<Optional<BibDatabaseContext>> activeDatabaseProperty() {
        return activeDatabase;
    }

    public ObjectProperty<Optional<GroupTreeNode>> activeGroupProperty() {
        return activeGroup;
    }

    public ObservableList<BibEntry> getSelectedEntries() {
        return FXCollections.unmodifiableObservableList(selectedEntries);
    }

    public void setSelectedEntries(List<BibEntry> newSelectedEntries) {
        selectedEntries.clear();
        selectedEntries.addAll(newSelectedEntries);
    }
}
