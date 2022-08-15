package org.jabref.gui;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.util.Pair;

import org.jabref.gui.edit.automaticfiededitor.LastAutomaticFieldEditorEdit;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.DialogWindowState;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.pdf.search.LuceneSearchResults;
import org.jabref.model.util.OptionalUtil;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyBinding;

/**
 * This class manages the GUI-state of JabRef, including:
 *
 * <ul>
 *   <li>currently selected database</li>
 *   <li>currently selected group</li>
 *   <li>active search</li>
 *   <li>active number of search results</li>
 *   <li>focus owner</li>
 *   <li>dialog window sizes/positions</li>
 * </ul>
 */
public class StateManager {

    private final CustomLocalDragboard localDragboard = new CustomLocalDragboard();
    private final ObservableList<BibDatabaseContext> openDatabases = FXCollections.observableArrayList();
    private final OptionalObjectProperty<BibDatabaseContext> activeDatabase = OptionalObjectProperty.empty();
    private final ReadOnlyListWrapper<GroupTreeNode> activeGroups = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList();
    private final ObservableMap<BibDatabaseContext, ObservableList<GroupTreeNode>> selectedGroups = FXCollections.observableHashMap();
    private final OptionalObjectProperty<SearchQuery> activeSearchQuery = OptionalObjectProperty.empty();
    private final ObservableMap<BibEntry, LuceneSearchResults> searchResults = FXCollections.observableHashMap();
    private final OptionalObjectProperty<Node> focusOwner = OptionalObjectProperty.empty();
    private final ObservableList<Pair<BackgroundTask, Task<?>>> backgroundTasks = FXCollections.observableArrayList(task -> new Observable[] {task.getValue().progressProperty(), task.getValue().runningProperty()});
    private final EasyBinding<Boolean> anyTaskRunning = EasyBind.reduce(backgroundTasks, tasks -> tasks.map(Pair::getValue).anyMatch(Task::isRunning));
    private final EasyBinding<Boolean> anyTasksThatWillNotBeRecoveredRunning = EasyBind.reduce(backgroundTasks, tasks -> tasks.anyMatch(task -> !task.getKey().willBeRecoveredAutomatically() && task.getValue().isRunning()));
    private final EasyBinding<Double> tasksProgress = EasyBind.reduce(backgroundTasks, tasks -> tasks.map(Pair::getValue).filter(Task::isRunning).mapToDouble(Task::getProgress).average().orElse(1));
    private final ObservableMap<String, DialogWindowState> dialogWindowStates = FXCollections.observableHashMap();
    private final ObservableList<SidePaneType> visibleSidePanes = FXCollections.observableArrayList();

    private final ObjectProperty<LastAutomaticFieldEditorEdit> lastAutomaticFieldEditorEdit = new SimpleObjectProperty<>();

    public StateManager() {
        activeGroups.bind(Bindings.valueAt(selectedGroups, activeDatabase.orElse(null)));
    }

    public ObservableList<SidePaneType> getVisibleSidePaneComponents() {
        return visibleSidePanes;
    }

    public CustomLocalDragboard getLocalDragboard() {
        return localDragboard;
    }

    public ObservableList<BibDatabaseContext> getOpenDatabases() {
        return openDatabases;
    }

    public OptionalObjectProperty<BibDatabaseContext> activeDatabaseProperty() {
        return activeDatabase;
    }

    public OptionalObjectProperty<SearchQuery> activeSearchQueryProperty() {
        return activeSearchQuery;
    }

    public IntegerProperty getSearchResultSize() {
        IntegerProperty size = new SimpleIntegerProperty();
        size.bind(Bindings.size(searchResults));
        return size;
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

    public OptionalObjectProperty<Node> focusOwnerProperty() {
        return focusOwner;
    }

    public Optional<Node> getFocusOwner() {
        return focusOwner.get();
    }

    public ObservableList<Task<?>> getBackgroundTasks() {
        return EasyBind.map(backgroundTasks, Pair::getValue);
    }

    public void addBackgroundTask(BackgroundTask backgroundTask, Task<?> task) {
        this.backgroundTasks.add(0, new Pair<>(backgroundTask, task));
    }

    public EasyBinding<Boolean> getAnyTaskRunning() {
        return anyTaskRunning;
    }

    public EasyBinding<Boolean> getAnyTasksThatWillNotBeRecoveredRunning() {
        return anyTasksThatWillNotBeRecoveredRunning;
    }

    public EasyBinding<Double> getTasksProgress() {
        return tasksProgress;
    }

    public DialogWindowState getDialogWindowState(String className) {
        return dialogWindowStates.get(className);
    }

    public void setDialogWindowState(String className, DialogWindowState state) {
        dialogWindowStates.put(className, state);
    }

    public ObjectProperty<LastAutomaticFieldEditorEdit> lastAutomaticFieldEditorEditProperty() {
        return lastAutomaticFieldEditorEdit;
    }

    public LastAutomaticFieldEditorEdit getLastAutomaticFieldEditorEdit() {
        return lastAutomaticFieldEditorEditProperty().get();
    }

    public void setLastAutomaticFieldEditorEdit(LastAutomaticFieldEditorEdit automaticFieldEditorEdit) {
        lastAutomaticFieldEditorEditProperty().set(automaticFieldEditorEdit);
    }

    public ObservableMap<BibEntry, LuceneSearchResults> getSearchResults() {
        return searchResults;
    }
}
