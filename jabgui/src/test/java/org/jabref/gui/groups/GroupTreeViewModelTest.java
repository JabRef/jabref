package org.jabref.gui.groups;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.HeadlessGuiUndoManager;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.search.NoOpSearchBackend;
import org.jabref.logic.search.SearchContext;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.undo.CompoundEdit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// org.jabref.gui.groups.GroupNodeViewModel.refreshGroup is used, which uses "Platform.runlater"
@ExtendWith(ApplicationExtension.class)
class GroupTreeViewModelTest {

    private StateManager stateManager;
    private GroupTreeViewModel groupTree;
    private BibDatabaseContext databaseContext;
    private TaskExecutor taskExecutor;
    private GuiPreferences preferences;
    private DialogService dialogService;
    private HeadlessGuiUndoManager journal;

    @BeforeEach
    void setUp() {
        databaseContext = new BibDatabaseContext();

        stateManager = mock(JabRefGuiStateManager.class);
        OptionalObjectProperty<BibDatabaseContext> activeDb = OptionalObjectProperty.empty();
        activeDb.setValue(Optional.of(databaseContext));
        when(stateManager.activeDatabaseProperty()).thenReturn(activeDb);
        when(stateManager.getSearchContext(databaseContext)).thenReturn(new SearchContext(
                new SimpleBooleanProperty(false),
                NoOpSearchBackend::new,
                NoOpSearchBackend::new));
        when(stateManager.getSelectedGroups(databaseContext)).thenReturn(FXCollections.emptyObservableList());
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.emptyObservableList());
        journal = new HeadlessGuiUndoManager();
        when(stateManager.getUndoManager(databaseContext)).thenReturn(journal);

        taskExecutor = new CurrentThreadTaskExecutor();
        preferences = mock(GuiPreferences.class);
        dialogService = mock(DialogService.class, Answers.RETURNS_DEEP_STUBS);

        when(preferences.getLibraryPreferences()).thenReturn(new LibraryPreferences(
                databaseContext.getMode(),
                false,
                false,
                false,
                "Imported entries"
        ));
        when(preferences.getGroupsPreferences()).thenReturn(new GroupsPreferences(
                EnumSet.noneOf(GroupViewMode.class),
                true,
                true,
                false,
                GroupHierarchyType.INDEPENDENT,
                false));
        BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);
        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');
        when(preferences.getBibEntryPreferences()).thenReturn(bibEntryPreferences);
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
    }

    @Test
    void rootGroupIsAllEntriesByDefault() {
        AllEntriesGroup allEntriesGroup = new AllEntriesGroup("All entries");
        assertEquals(new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, allEntriesGroup, new CustomLocalDragboard(), preferences), groupTree.rootGroupProperty().getValue());
    }

    /// Group operations reach the model by editing the tree in place and writing the root back, so
    /// they are recorded as the tree they produced - one step, undone by installing the tree that
    /// was there before.
    @Test
    // [utest->req~logic.undo.group-operations-recorded~1]
    void reorderingSubgroupsIsUndoable() {
        GroupTreeNode root = GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new ExplicitGroup("B", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new ExplicitGroup("A", GroupHierarchyType.INDEPENDENT, ','));
        databaseContext.getMetaData().setGroups(root);
        // The view model reads the tree when it is told about the library, and refreshes on later
        // changes only through Platform.runLater - so it is rebuilt here rather than waited on.
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);

        groupTree.sortAlphabeticallyRecursive(databaseContext.getMetaData().getGroups().orElseThrow());
        assertEquals(List.of("A", "B"), childNames());

        assertTrue(journal.canUndo(), "the sort was not recorded");
        journal.undo();
        assertEquals(List.of("B", "A"), childNames());
    }

    /// A step that describes only half of what happened is worse than none: the library would hold
    /// a tree the journal cannot take back.
    @Test
    void aFailingOperationStillRecordsWhatItChanged() {
        GroupTreeNode root = GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ','));
        databaseContext.getMetaData().setGroups(root);
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);

        assertThrows(IllegalStateException.class, () -> groupTree.recordTreeChange("failing", () -> {
            databaseContext.getMetaData().getGroups().orElseThrow()
                           .addSubgroup(new ExplicitGroup("Books", GroupHierarchyType.INDEPENDENT, ','));
            throw new IllegalStateException("operation failed");
        }));

        assertEquals(List.of("Books"), childNames());
        assertTrue(journal.canUndo(), "what the failed operation changed was not recorded");
        journal.undo();
        assertEquals(List.of(), childNames());
    }

    /// Sorting a group that is already sorted changes nothing, and an undo step that does nothing
    /// makes the next Ctrl+Z look broken.
    @Test
    void anOperationThatChangesNothingIsNotAnUndoStep() {
        GroupTreeNode root = GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new ExplicitGroup("A", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new ExplicitGroup("B", GroupHierarchyType.INDEPENDENT, ','));
        databaseContext.getMetaData().setGroups(root);
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);

        groupTree.sortAlphabeticallyRecursive(databaseContext.getMetaData().getGroups().orElseThrow());

        assertFalse(journal.canUndo(), "sorting an already sorted group became an undo step");
    }

    private List<String> childNames() {
        return databaseContext.getMetaData().getGroups().orElseThrow().getChildren().stream()
                              .map(node -> node.getGroup().getName())
                              .toList();
    }

    @Test
    void explicitGroupsAreRemovedFromEntriesOnDelete() {
        ExplicitGroup group = new ExplicitGroup("group", GroupHierarchyType.INDEPENDENT, ',');
        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupNodeViewModel model = new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, group, new CustomLocalDragboard(), preferences);
        model.addEntriesToGroup(databaseContext.getEntries());
        groupTree.removeGroupsAndSubGroupsFromEntries(model, new CompoundEdit("test"));

        assertEquals(Optional.empty(), entry.getField(StandardField.GROUPS));
    }

    @Test
    void keywordGroupsAreNotRemovedFromEntriesOnDelete() {
        String groupName = "A";
        WordKeywordGroup group = new WordKeywordGroup(groupName, GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, groupName, true, ',', true);
        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupNodeViewModel model = new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, group, new CustomLocalDragboard(), preferences);
        model.addEntriesToGroup(databaseContext.getEntries());
        groupTree.removeGroupsAndSubGroupsFromEntries(model, new CompoundEdit("test"));

        assertEquals(groupName, entry.getField(StandardField.KEYWORDS).get());
    }

    @Test
    void shouldNotShowDialogWhenGroupNameChanges() {
        AbstractGroup oldGroup = new ExplicitGroup("group", GroupHierarchyType.INDEPENDENT, ',');
        AbstractGroup newGroup = new ExplicitGroup("newGroupName", GroupHierarchyType.INDEPENDENT, ',');
        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        assertTrue(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void shouldNotShowDialogWhenGroupsAreEqual() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        assertTrue(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void shouldShowDialogWhenKeywordDiffers() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordChanged", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        assertFalse(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void shouldShowDialogWhenCaseSensitivyDiffers() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", false, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordChanged", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        assertFalse(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void rootNodeShouldNotHaveSuggestedGroupsByDefault() {
        GroupNodeViewModel rootGroup = groupTree.rootGroupProperty().getValue();
        assertFalse(rootGroup.hasAllSuggestedGroups());
    }

    @Test
    void shouldAddsAllSuggestedGroupsWhenNoneExist() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        assertFalse(rootGroup.hasAllSuggestedGroups());

        model.addSuggestedGroups(rootGroup);

        assertEquals(2, rootGroup.getChildren().size());
        assertTrue(rootGroup.hasAllSuggestedGroups());
    }

    @Test
    void shouldAddOnlyMissingGroup() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutFilesGroup());
        assertEquals(1, rootGroup.getChildren().size());

        model.addSuggestedGroups(rootGroup);

        assertEquals(2, rootGroup.getChildren().size());
        assertTrue(rootGroup.hasAllSuggestedGroups());
    }

    @Test
    void shouldNotAddSuggestedGroupsWhenAllExist() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutFilesGroup());
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutGroupsGroup());
        assertEquals(2, rootGroup.getChildren().size());

        model.addSuggestedGroups(rootGroup);

        assertEquals(2, rootGroup.getChildren().size());
        assertTrue(rootGroup.hasAllSuggestedGroups());
    }

    @Test
    void shouldNotCreateImportedEntriesGroupWhenEnabled() {
        preferences.getLibraryPreferences().setAddImportedEntries(true);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        List<GroupNodeViewModel> groups = model.rootGroupProperty().getValue().getChildren();

        assertEquals(0, groups.size());
    }

    @Test
    void shouldNotCreateImportedEntriesGroupWhenCustomNameIsSet() {
        preferences.getLibraryPreferences().setAddImportedEntries(true);
        preferences.getLibraryPreferences().setAddImportedEntriesGroupName("Review list");

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        List<GroupNodeViewModel> groups = model.rootGroupProperty().getValue().getChildren();

        assertEquals(0, groups.size());
    }
}
