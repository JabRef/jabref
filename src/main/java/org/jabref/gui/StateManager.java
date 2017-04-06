package org.jabref.gui;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.util.OptionalUtil;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;

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
    private final ReadOnlyObjectWrapper<Optional<GroupTreeNode>> activeGroup = new ReadOnlyObjectWrapper<>(Optional.empty());
    private final ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList();
    private final ObservableMap<BibDatabaseContext, GroupTreeNode> selectedGroups = FXCollections.observableHashMap();

    public StateManager() {
        MonadicBinding<BibDatabaseContext> currentDatabase = EasyBind.map(activeDatabase, database -> database.orElse(null));
        activeGroup.bind(EasyBind.map(Bindings.valueAt(selectedGroups, currentDatabase), Optional::ofNullable));
    }

    public ObjectProperty<Optional<BibDatabaseContext>> activeDatabaseProperty() {
        return activeDatabase;
    }

    public ReadOnlyObjectProperty<Optional<GroupTreeNode>> activeGroupProperty() {
        return activeGroup.getReadOnlyProperty();
    }

    public ObservableList<BibEntry> getSelectedEntries() {
        return FXCollections.unmodifiableObservableList(selectedEntries);
    }

    public void setSelectedEntries(List<BibEntry> newSelectedEntries) {
        selectedEntries.clear();
        selectedEntries.addAll(newSelectedEntries);
    }

    public void setSelectedGroup(BibDatabaseContext database, GroupTreeNode newSelectedGroup) {
        Objects.requireNonNull(newSelectedGroup);
        selectedGroups.put(database, newSelectedGroup);
    }

    public Optional<GroupTreeNode> getSelectedGroup(BibDatabaseContext database) {
        return Optional.ofNullable(selectedGroups.get(database));
    }

    public void clearSelectedGroup(BibDatabaseContext database) {
        selectedGroups.remove(database);
    }

    public List<BibEntry> getEntriesInCurrentDatabase() {
        return OptionalUtil.flatMap(activeDatabase.get(), BibDatabaseContext::getEntries)
                .collect(Collectors.toList());
    }
}
