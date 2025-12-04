package org.jabref.gui.groups;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.AdaptVisibleTabs;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.WordKeywordGroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupTreeViewModelTest {

    private StateManager stateManager;
    private GroupTreeViewModel groupTree;
    private BibDatabaseContext databaseContext;
    private TaskExecutor taskExecutor;
    private GuiPreferences preferences;
    private DialogService dialogService;

    @BeforeEach
    void setUp() {
        databaseContext = new BibDatabaseContext();
        stateManager = new JabRefGuiStateManager();
        stateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));
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
                GroupHierarchyType.INDEPENDENT));
        BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);
        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');
        when(preferences.getBibEntryPreferences()).thenReturn(bibEntryPreferences);
        groupTree = new GroupTreeViewModel(stateManager, mock(DialogService.class), mock(AiService.class), preferences, mock(AdaptVisibleTabs.class), taskExecutor, new CustomLocalDragboard());
    }

    @Test
    void rootGroupIsAllEntriesByDefault() {
        AllEntriesGroup allEntriesGroup = new AllEntriesGroup("All entries");
        assertEquals(new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, allEntriesGroup, new CustomLocalDragboard(), preferences), groupTree.rootGroupProperty().getValue());
    }

    @Test
    void rootGroupIsSelectedByDefault() {
        assertEquals(groupTree.rootGroupProperty().get().getGroupNode(), stateManager.getSelectedGroups(databaseContext).getFirst());
    }

    @Test
    void explicitGroupsAreRemovedFromEntriesOnDelete() {
        ExplicitGroup group = new ExplicitGroup("group", GroupHierarchyType.INDEPENDENT, ',');
        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupNodeViewModel model = new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, group, new CustomLocalDragboard(), preferences);
        model.addEntriesToGroup(databaseContext.getEntries());
        groupTree.removeGroupsAndSubGroupsFromEntries(model);

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
        groupTree.removeGroupsAndSubGroupsFromEntries(model);

        assertEquals(groupName, entry.getField(StandardField.KEYWORDS).get());
    }

    @Test
    void shouldNotShowDialogWhenGroupNameChanges() {
        AbstractGroup oldGroup = new ExplicitGroup("group", GroupHierarchyType.INDEPENDENT, ',');
        AbstractGroup newGroup = new ExplicitGroup("newGroupName", GroupHierarchyType.INDEPENDENT, ',');
        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, mock(AdaptVisibleTabs.class), taskExecutor, new CustomLocalDragboard());
        assertTrue(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void shouldNotShowDialogWhenGroupsAreEqual() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, mock(AdaptVisibleTabs.class), taskExecutor, new CustomLocalDragboard());
        assertTrue(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void shouldShowDialogWhenKeywordDiffers() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordChanged", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, mock(AdaptVisibleTabs.class), taskExecutor, new CustomLocalDragboard());
        assertFalse(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void shouldShowDialogWhenCaseSensitivyDiffers() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", false, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordChanged", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, mock(AdaptVisibleTabs.class), taskExecutor, new CustomLocalDragboard());
        assertFalse(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void rootNodeShouldNotHaveSuggestedGroupsByDefault() {
        GroupNodeViewModel rootGroup = groupTree.rootGroupProperty().getValue();
        assertFalse(rootGroup.hasAllSuggestedGroups());
    }

    @Test
    void shouldAddsAllSuggestedGroupsWhenNoneExist() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, mock(AdaptVisibleTabs.class), taskExecutor, new CustomLocalDragboard());
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        assertFalse(rootGroup.hasAllSuggestedGroups());

        model.addSuggestedGroups(rootGroup);

        assertEquals(2, rootGroup.getChildren().size());
        assertTrue(rootGroup.hasAllSuggestedGroups());
    }

    @Test
    void shouldAddOnlyMissingGroup() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, mock(AdaptVisibleTabs.class), taskExecutor, new CustomLocalDragboard());
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutFilesGroup());
        assertEquals(1, rootGroup.getChildren().size());

        model.addSuggestedGroups(rootGroup);

        assertEquals(2, rootGroup.getChildren().size());
        assertTrue(rootGroup.hasAllSuggestedGroups());
    }

    @Test
    void shouldNotAddSuggestedGroupsWhenAllExist() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, mock(AdaptVisibleTabs.class), taskExecutor, new CustomLocalDragboard());
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutFilesGroup());
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutGroupsGroup());
        assertEquals(2, rootGroup.getChildren().size());

        model.addSuggestedGroups(rootGroup);

        assertEquals(2, rootGroup.getChildren().size());
        assertTrue(rootGroup.hasAllSuggestedGroups());
    }

    @Test
    void shouldCreateImportedEntriesGroupWhenEnabled() {
        preferences.getLibraryPreferences().setAddImportedEntries(true);

        List<GroupNodeViewModel> prevModel = groupTree.rootGroupProperty().getValue().getChildren();
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, mock(AdaptVisibleTabs.class), taskExecutor, new CustomLocalDragboard());
        String actualGrpName = model.rootGroupProperty().getValue().getChildren().getFirst().getDisplayName();

        assertEquals(0, prevModel.size());
        assertEquals("Imported entries", actualGrpName);
    }

    @Test
    void shouldReflectUpdatedNameForImportedEntriesGroup() {
        preferences.getLibraryPreferences().setAddImportedEntries(true);
        preferences.getLibraryPreferences().setAddImportedEntriesGroupName("Review list");

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, mock(AdaptVisibleTabs.class), taskExecutor, new CustomLocalDragboard());
        String actualGrpName = model.rootGroupProperty().getValue().getChildren().getFirst().getDisplayName();

        assertEquals("Review list", actualGrpName);
    }
}
