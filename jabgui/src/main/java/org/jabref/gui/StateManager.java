package org.jabref.gui;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;

import org.jabref.gui.ai.chat.AiGroupChatWindow;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.undo.GuiUndoManager;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.DialogWindowState;
import org.jabref.gui.walkthrough.Walkthrough;
import org.jabref.http.SrvStateManager;
import org.jabref.logic.search.SearchContext;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.query.SearchQuery;

import com.tobiasdiez.easybind.EasyBinding;
import org.jspecify.annotations.NonNull;

/// This class manages the GUI-state of JabRef, including:
///
///
/// - currently selected database
/// - currently selected group
/// - active search
/// - active number of search results
/// - focus owner
/// - dialog window sizes/positions
/// - opened AI chat window (controlled by [org.jabref.logic.ai.AiService])
///
public interface StateManager extends SrvStateManager {

    ObservableList<SidePaneType> getVisibleSidePaneComponents();

    CustomLocalDragboard getLocalDragboard();

    OptionalObjectProperty<LibraryTab> activeTabProperty();

    /// The undo journal of the library `context` describes.
    ///
    /// Each library has its own, so a caller names the library it is recording against instead of
    /// holding a journal handed to it when it was built. Which library that is has to be decided
    /// where the change is made, not where the change lands: a task that finishes after the user
    /// switched libraries still belongs to the one it ran on.
    ///
    /// This is where a library's journal lives, so it hands out the whole of it. A caller that only
    /// records says so by what it declares — the parameter it passes the journal to, or
    /// [org.jabref.gui.LibraryTab#getUndoManager], which hands its collaborators the recording half.
    GuiUndoManager getUndoManager(BibDatabaseContext context);

    /// Discards the journal of a library that is closing, with the changes it holds and the entries
    /// those changes keep alive.
    void removeUndoManager(BibDatabaseContext context);

    OptionalObjectProperty<SearchQuery> activeSearchQuery(SearchType type);

    StringProperty searchQueryProperty();

    IntegerProperty searchResultSize(SearchType type);

    void setSearchContext(BibDatabaseContext database, SearchContext searchContext);

    void setSelectedEntries(List<BibEntry> newSelectedEntries);

    void setSelectedGroups(BibDatabaseContext context, List<GroupTreeNode> newSelectedGroups);

    ObservableList<GroupTreeNode> getSelectedGroups(BibDatabaseContext context);

    void clearSelectedGroups(BibDatabaseContext context);

    void setActiveDatabase(BibDatabaseContext database);

    void replaceActiveDatabase(@NonNull BibDatabaseContext database);

    OptionalObjectProperty<Node> focusOwnerProperty();

    Optional<Node> getFocusOwner();

    ObservableList<Task<?>> getBackgroundTasks();

    ObservableList<Task<?>> getRunningBackgroundTasks();

    void addBackgroundTask(BackgroundTask<?> backgroundTask, Task<?> task);

    EasyBinding<Boolean> getAnyTasksThatWillNotBeRecoveredRunning();

    DialogWindowState getDialogWindowState(String className);

    void setDialogWindowState(String className, DialogWindowState state);

    void addSearchHistory(String search);

    ObservableList<String> getWholeSearchHistory();

    List<String> getLastSearchHistory(int size);

    void clearSearchHistory();

    Optional<AiGroupChatWindow> getAiChatWindowForGroup(BibDatabaseContext context, String groupName);

    void setAiChatWindowForGroup(BibDatabaseContext context, String groupName, AiGroupChatWindow aiGroupChatWindow);

    void removeAiChatWindowForGroup(BibDatabaseContext context, String groupName);

    BooleanProperty getEditorShowing();

    void setActiveWalkthrough(Walkthrough walkthrough);

    Optional<Walkthrough> getActiveWalkthrough();

    BooleanProperty canGoBackProperty();

    BooleanProperty canGoForwardProperty();
}
