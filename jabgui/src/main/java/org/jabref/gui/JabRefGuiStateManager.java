package org.jabref.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.util.Pair;

import org.jabref.gui.ai.components.aichat.AiChatWindow;
import org.jabref.gui.edit.automaticfiededitor.LastAutomaticFieldEditorEdit;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.DialogWindowState;
import org.jabref.gui.walkthrough.Walkthrough;
import org.jabref.logic.command.CommandSelectionTab;
import org.jabref.logic.search.IndexManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.query.SearchQuery;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyBinding;
import com.tobiasdiez.easybind.PreboundBinding;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *   <li>opened AI chat window (controlled by {@link org.jabref.logic.ai.AiService})</li>
 * </ul>
 */
public class JabRefGuiStateManager implements StateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefGuiStateManager.class);
    private final CustomLocalDragboard localDragboard = new CustomLocalDragboard();
    private final ObservableList<BibDatabaseContext> openDatabases = FXCollections.observableArrayList();
    private final OptionalObjectProperty<BibDatabaseContext> activeDatabase = OptionalObjectProperty.empty();
    private final OptionalObjectProperty<LibraryTab> activeTab = OptionalObjectProperty.empty();
    private final ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList();
    private final ObservableMap<String, ObservableList<GroupTreeNode>> selectedGroups = FXCollections.observableHashMap();
    private final ObservableMap<String, IndexManager> indexManagers = FXCollections.observableHashMap();
    private final OptionalObjectProperty<SearchQuery> activeSearchQuery = OptionalObjectProperty.empty();
    private final OptionalObjectProperty<SearchQuery> activeGlobalSearchQuery = OptionalObjectProperty.empty();
    private final StringProperty searchQueryProperty = new SimpleStringProperty();
    private final IntegerProperty searchResultSize = new SimpleIntegerProperty(0);
    private final IntegerProperty globalSearchResultSize = new SimpleIntegerProperty(0);
    private final OptionalObjectProperty<Node> focusOwner = OptionalObjectProperty.empty();
    private final ObservableList<Pair<BackgroundTask<?>, Task<?>>> backgroundTasksPairs = FXCollections.observableArrayList(task -> new Observable[] {task.getValue().progressProperty(), task.getValue().runningProperty()});
    private final ObservableList<Task<?>> backgroundTasks = EasyBind.map(backgroundTasksPairs, Pair::getValue);
    private final FilteredList<Task<?>> runningBackgroundTasks = new FilteredList<>(backgroundTasks, Task::isRunning);
    private final BooleanBinding anyTaskRunning = Bindings.createBooleanBinding(() -> !runningBackgroundTasks.isEmpty(), runningBackgroundTasks);
    private final EasyBinding<Boolean> anyTasksThatWillNotBeRecoveredRunning = EasyBind.reduce(backgroundTasksPairs, tasks -> tasks.anyMatch(task -> !task.getKey().willBeRecoveredAutomatically() && task.getValue().isRunning()));
    private final EasyBinding<Double> tasksProgress = EasyBind.reduce(backgroundTasksPairs, tasks -> tasks.map(Pair::getValue).filter(Task::isRunning).mapToDouble(Task::getProgress).average().orElse(1));
    private final ObservableMap<String, DialogWindowState> dialogWindowStates = FXCollections.observableHashMap();
    private final ObservableList<SidePaneType> visibleSidePanes = FXCollections.observableArrayList();
    private final ObjectProperty<LastAutomaticFieldEditorEdit> lastAutomaticFieldEditorEdit = new SimpleObjectProperty<>();
    private final ObservableList<String> searchHistory = FXCollections.observableArrayList();
    private final List<AiChatWindow> aiChatWindows = new ArrayList<>();
    private final BooleanProperty editorShowing = new SimpleBooleanProperty(false);
    private final OptionalObjectProperty<Walkthrough> activeWalkthrough = OptionalObjectProperty.empty();
    private final BooleanProperty canGoBack = new SimpleBooleanProperty(false);
    private final BooleanProperty canGoForward = new SimpleBooleanProperty(false);

    @Override
    public ObservableList<SidePaneType> getVisibleSidePaneComponents() {
        return visibleSidePanes;
    }

    @Override
    public CustomLocalDragboard getLocalDragboard() {
        return localDragboard;
    }

    @Override
    public ObservableList<BibDatabaseContext> getOpenDatabases() {
        return openDatabases;
    }

    @Override
    public OptionalObjectProperty<BibDatabaseContext> activeDatabaseProperty() {
        return activeDatabase;
    }

    @Override
    public OptionalObjectProperty<LibraryTab> activeTabProperty() {
        return activeTab;
    }

    @Override
    public OptionalObjectProperty<SearchQuery> activeSearchQuery(SearchType type) {
        return type == SearchType.NORMAL_SEARCH ? activeSearchQuery : activeGlobalSearchQuery;
    }

    @Override
    public StringProperty searchQueryProperty() {
        return searchQueryProperty;
    }

    @Override
    public IntegerProperty searchResultSize(SearchType type) {
        return type == SearchType.NORMAL_SEARCH ? searchResultSize : globalSearchResultSize;
    }

    @Override
    public ObservableList<BibEntry> getSelectedEntries() {
        return selectedEntries;
    }

    @Override
    public void setSelectedEntries(List<BibEntry> newSelectedEntries) {
        selectedEntries.setAll(newSelectedEntries);
    }

    @Override
    public void setSelectedGroups(BibDatabaseContext context, @NonNull List<GroupTreeNode> newSelectedGroups) {
        selectedGroups.computeIfAbsent(context.getUid(), k -> FXCollections.observableArrayList()).setAll(newSelectedGroups);
    }

    @Override
    public ObservableList<GroupTreeNode> getSelectedGroups(BibDatabaseContext context) {
        return selectedGroups.computeIfAbsent(context.getUid(), k -> FXCollections.observableArrayList());
    }

    @Override
    public void clearSelectedGroups(BibDatabaseContext context) {
        selectedGroups.computeIfAbsent(context.getUid(), k -> FXCollections.observableArrayList()).clear();
    }

    @Override
    public void setIndexManager(BibDatabaseContext database, IndexManager indexManager) {
        indexManagers.put(database.getUid(), indexManager);
    }

    @Override
    public Optional<IndexManager> getIndexManager(BibDatabaseContext database) {
        return Optional.ofNullable(indexManagers.get(database.getUid()));
    }

    @Override
    public Optional<BibDatabaseContext> getActiveDatabase() {
        return activeDatabase.get();
    }

    @Override
    public void setActiveDatabase(BibDatabaseContext database) {
        if (database == null) {
            LOGGER.info("No open database detected");
            activeDatabaseProperty().set(Optional.empty());
        } else {
            activeDatabaseProperty().set(Optional.of(database));
        }
    }

    @Override
    public OptionalObjectProperty<Node> focusOwnerProperty() {
        return focusOwner;
    }

    @Override
    public Optional<Node> getFocusOwner() {
        return focusOwner.get();
    }

    @Override
    public ObservableList<Task<?>> getBackgroundTasks() {
        return backgroundTasks;
    }

    @Override
    public ObservableList<Task<?>> getRunningBackgroundTasks() {
        return runningBackgroundTasks;
    }

    @Override
    public void addBackgroundTask(BackgroundTask<?> backgroundTask, Task<?> task) {
        this.backgroundTasksPairs.addFirst(new Pair<>(backgroundTask, task));
    }

    @Override
    public BooleanBinding getAnyTaskRunning() {
        return anyTaskRunning;
    }

    @Override
    public EasyBinding<Boolean> getAnyTasksThatWillNotBeRecoveredRunning() {
        return anyTasksThatWillNotBeRecoveredRunning;
    }

    @Override
    public EasyBinding<Double> getTasksProgress() {
        return tasksProgress;
    }

    @Override
    public DialogWindowState getDialogWindowState(String className) {
        return dialogWindowStates.get(className);
    }

    @Override
    public void setDialogWindowState(String className, DialogWindowState state) {
        dialogWindowStates.put(className, state);
    }

    @Override
    public ObjectProperty<LastAutomaticFieldEditorEdit> lastAutomaticFieldEditorEditProperty() {
        return lastAutomaticFieldEditorEdit;
    }

    @Override
    public void setLastAutomaticFieldEditorEdit(LastAutomaticFieldEditorEdit automaticFieldEditorEdit) {
        lastAutomaticFieldEditorEditProperty().set(automaticFieldEditorEdit);
    }

    @Override
    public List<String> getAllDatabasePaths() {
        List<String> list = new ArrayList<>();
        getOpenDatabases().stream()
                          .map(BibDatabaseContext::getDatabasePath)
                          .forEachOrdered(pathOptional -> pathOptional.ifPresentOrElse(
                                  path -> list.add(path.toAbsolutePath().toString()),
                                  () -> list.add("")));
        return list;
    }

    @Override
    public ObjectBinding<Optional<CommandSelectionTab>> getActiveSelectionTabProperty() {
        return new PreboundBinding<>(activeTab) {
            @Override
            protected Optional<CommandSelectionTab> computeValue() {
                return activeTab.getValue().map(Function.identity());
            }
        };
    }

    @Override
    public void addSearchHistory(String search) {
        searchHistory.remove(search);
        searchHistory.add(search);
    }

    @Override
    public ObservableList<String> getWholeSearchHistory() {
        return searchHistory;
    }

    @Override
    public List<String> getLastSearchHistory(int size) {
        int sizeSearches = searchHistory.size();
        if (size < sizeSearches) {
            return searchHistory.subList(sizeSearches - size, sizeSearches);
        }
        return searchHistory;
    }

    @Override
    public void clearSearchHistory() {
        searchHistory.clear();
    }

    @Override
    public List<AiChatWindow> getAiChatWindows() {
        return aiChatWindows;
    }

    @Override
    public BooleanProperty getEditorShowing() {
        return editorShowing;
    }

    @Override
    public void setActiveWalkthrough(Walkthrough walkthrough) {
        activeWalkthrough.set(Optional.ofNullable(walkthrough));
    }

    @Override
    public Optional<Walkthrough> getActiveWalkthrough() {
        return activeWalkthrough.get();
    }

    @Override
    public BooleanProperty canGoBackProperty() {
        return canGoBack;
    }

    @Override
    public BooleanProperty canGoForwardProperty() {
        return canGoForward;
    }
}
