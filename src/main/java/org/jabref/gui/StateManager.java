package org.jabref.gui;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.scene.Node;

import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.gui.util.uithreadaware.UiThreadObservableList;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.util.OptionalUtil;

/**
 * This class manages the GUI-state of JabRef, including:
 * - currently selected database
 * - currently selected group
 * - active search
 * - active number of search results
 * - focus owner
 */
public class StateManager {

    private final CustomLocalDragboard localDragboard = new CustomLocalDragboard();
    private final OptionalObjectProperty<BibDatabaseContext> activeDatabase = OptionalObjectProperty.empty();
    private final ReadOnlyListWrapper<GroupTreeNode> activeGroups = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList();
    private final ObservableMap<BibDatabaseContext, ObservableList<GroupTreeNode>> selectedGroups = FXCollections.observableHashMap();
    private final OptionalObjectProperty<SearchQuery> activeSearchQuery = OptionalObjectProperty.empty();
    private final ObservableMap<BibDatabaseContext, IntegerProperty> searchResultMap = FXCollections.observableHashMap();
    private final OptionalObjectProperty<Node> focusOwner = OptionalObjectProperty.empty();
    private final UiThreadObservableList<Task> backgroundTasks = new UiThreadObservableList(FXCollections.observableArrayList());

    public StateManager() {
        activeGroups.bind(Bindings.valueAt(selectedGroups, activeDatabase.orElse(null)));
    }

    public CustomLocalDragboard getLocalDragboard() {
        return localDragboard;
    }

    public OptionalObjectProperty<BibDatabaseContext> activeDatabaseProperty() {
        return activeDatabase;
    }

    public OptionalObjectProperty<SearchQuery> activeSearchQueryProperty() {
        return activeSearchQuery;
    }

    public void setActiveSearchResultSize(BibDatabaseContext database, IntegerProperty resultSize) {
        searchResultMap.put(database, resultSize);
    }

    public IntegerProperty getSearchResultSize() {
        return searchResultMap.getOrDefault(activeDatabase.getValue().orElse(new BibDatabaseContext()), new SimpleIntegerProperty(0));
    }

    public ReadOnlyListProperty<GroupTreeNode> activeGroupProperty() {
        return activeGroups.getReadOnlyProperty();
    }

    public ObservableList<BibEntry> getSelectedEntries() {
        return selectedEntries;
    }

    public void setSelectedEntries(List<BibEntry> newSelectedEntries) {
        selectedEntries.setAll(newSelectedEntries);
    }

    public void setSelectedGroups(BibDatabaseContext database, List<GroupTreeNode> newSelectedGroups) {
        Objects.requireNonNull(newSelectedGroups);
        selectedGroups.put(database, FXCollections.observableArrayList(newSelectedGroups));
    }

    public ObservableList<GroupTreeNode> getSelectedGroup(BibDatabaseContext database) {
        ObservableList<GroupTreeNode> selectedGroupsForDatabase = selectedGroups.get(database);
        return selectedGroupsForDatabase != null ? selectedGroupsForDatabase : FXCollections.observableArrayList();
    }

    public void clearSelectedGroups(BibDatabaseContext database) {
        selectedGroups.remove(database);
    }

    public Optional<BibDatabaseContext> getActiveDatabase() {
        return activeDatabase.get();
    }

    public List<BibEntry> getEntriesInCurrentDatabase() {
        return OptionalUtil.flatMap(activeDatabase.get(), BibDatabaseContext::getEntries)
                           .collect(Collectors.toList());
    }

    public void clearSearchQuery() {
        activeSearchQuery.setValue(Optional.empty());
    }

    public void setSearchQuery(SearchQuery searchQuery) {
        activeSearchQuery.setValue(Optional.of(searchQuery));
    }

    public OptionalObjectProperty<Node> focusOwnerProperty() { return focusOwner; }

    public Optional<Node> getFocusOwner() { return focusOwner.get(); }

    public UiThreadObservableList<Task> getBackgroundTasks() {
        return backgroundTasks;
    }

    public void addBackgroundTask(Task backgroundTask) {
        this.backgroundTasks.add(backgroundTask);
    }

    public BooleanBinding anyTaskRunningBinding = Bindings.createBooleanBinding(
            () -> backgroundTasks.stream().anyMatch(Task::isRunning)
    );

    public DoubleBinding tasksProgressBinding = Bindings.createDoubleBinding(
            () -> backgroundTasks.stream().filter(Task::isRunning).mapToDouble(Task::getProgress).average().orElse(1)
    );
}
