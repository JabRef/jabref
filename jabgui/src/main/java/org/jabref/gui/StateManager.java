package org.jabref.gui;

import java.util.List;
import java.util.Optional;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;

import org.jabref.gui.ai.components.aichat.AiChatWindow;
import org.jabref.gui.edit.automaticfiededitor.LastAutomaticFieldEditorEdit;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.DialogWindowState;
import org.jabref.gui.walkthrough.Walkthrough;
import org.jabref.http.SrvStateManager;
import org.jabref.logic.search.IndexManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.query.SearchQuery;

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
 *   <li>opened AI chat window (controlled by {@link org.jabref.logic.ai.AiService})</li>
 * </ul>
 */
public interface StateManager extends SrvStateManager {

    ObservableList<SidePaneType> getVisibleSidePaneComponents();

    CustomLocalDragboard getLocalDragboard();

    OptionalObjectProperty<LibraryTab> activeTabProperty();

    OptionalObjectProperty<SearchQuery> activeSearchQuery(SearchType type);

    StringProperty searchQueryProperty();

    IntegerProperty searchResultSize(SearchType type);

    void setIndexManager(BibDatabaseContext database, IndexManager indexManager);

    void setSelectedEntries(List<BibEntry> newSelectedEntries);

    void setSelectedGroups(BibDatabaseContext context, List<GroupTreeNode> newSelectedGroups);

    ObservableList<GroupTreeNode> getSelectedGroups(BibDatabaseContext context);

    void clearSelectedGroups(BibDatabaseContext context);

    void setActiveDatabase(BibDatabaseContext database);

    OptionalObjectProperty<Node> focusOwnerProperty();

    Optional<Node> getFocusOwner();

    ObservableList<Task<?>> getBackgroundTasks();

    ObservableList<Task<?>> getRunningBackgroundTasks();

    void addBackgroundTask(BackgroundTask<?> backgroundTask, Task<?> task);

    BooleanBinding getAnyTaskRunning();

    EasyBinding<Boolean> getAnyTasksThatWillNotBeRecoveredRunning();

    EasyBinding<Double> getTasksProgress();

    DialogWindowState getDialogWindowState(String className);

    void setDialogWindowState(String className, DialogWindowState state);

    ObjectProperty<LastAutomaticFieldEditorEdit> lastAutomaticFieldEditorEditProperty();

    void setLastAutomaticFieldEditorEdit(LastAutomaticFieldEditorEdit automaticFieldEditorEdit);

    void addSearchHistory(String search);

    ObservableList<String> getWholeSearchHistory();

    List<String> getLastSearchHistory(int size);

    void clearSearchHistory();

    List<AiChatWindow> getAiChatWindows();

    BooleanProperty getEditorShowing();

    void setActiveWalkthrough(Walkthrough walkthrough);

    Optional<Walkthrough> getActiveWalkthrough();

    BooleanProperty canGoBackProperty();

    BooleanProperty canGoForwardProperty();
}
