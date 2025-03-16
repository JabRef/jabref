package org.jabref.gui.groups;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.search.SearchFlags;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupTreeViewModelTest {

    private StateManager stateManager;
    private GroupTreeViewModel groupTree;
    private BibDatabaseContext databaseContext;
    private GroupNodeViewModel rootGroupViewModel;
    private TaskExecutor taskExecutor;
    private GuiPreferences preferences;
    private DialogService dialogService;

    @BeforeEach
    void setUp() {
        databaseContext = new BibDatabaseContext();
        stateManager = new StateManager();
        stateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));
        taskExecutor = new CurrentThreadTaskExecutor();
        preferences = mock(GuiPreferences.class);
        dialogService = mock(DialogService.class, Answers.RETURNS_DEEP_STUBS);

        when(preferences.getGroupsPreferences()).thenReturn(new GroupsPreferences(
                EnumSet.noneOf(GroupViewMode.class),
                true,
                true,
                GroupHierarchyType.INDEPENDENT));
        groupTree = new GroupTreeViewModel(stateManager, mock(DialogService.class), mock(AiService.class), preferences, taskExecutor, new CustomLocalDragboard());
        rootGroupViewModel = groupTree.rootGroupProperty().get();
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

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, taskExecutor, new CustomLocalDragboard());
        assertTrue(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void shouldNotShowDialogWhenGroupsAreEqual() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, taskExecutor, new CustomLocalDragboard());
        assertTrue(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void shouldShowDialogWhenKeywordDiffers() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordChanged", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, taskExecutor, new CustomLocalDragboard());
        assertFalse(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void shouldShowDialogWhenCaseSensitivyDiffers() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", false, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordChanged", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, taskExecutor, new CustomLocalDragboard());
        assertFalse(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void addSuggestedSubGroupCreatesCorrectGroups() {
        Mockito.reset(dialogService);

        GroupTreeViewModel testGroupTree = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, taskExecutor, new CustomLocalDragboard());
        GroupNodeViewModel testRootGroup = testGroupTree.rootGroupProperty().get();

        testGroupTree.addSuggestedSubGroup(testRootGroup);

        verify(dialogService, times(2)).notify(anyString());

        List<GroupNodeViewModel> children = testRootGroup.getChildren();

        assertEquals(2, children.size());

        GroupNodeViewModel firstGroup = children.get(0);
        assertEquals(Localization.lang("Entries without linked files"), firstGroup.getDisplayName());

        GroupNodeViewModel secondGroup = children.get(1);
        assertEquals(Localization.lang("Entries without groups"), secondGroup.getDisplayName());

        AbstractGroup firstGroupObj = firstGroup.getGroupNode().getGroup();
        assertTrue(firstGroupObj instanceof SearchGroup);
        SearchGroup firstSearchGroup = (SearchGroup) firstGroupObj;
        assertEquals("file !=~.*", firstSearchGroup.getSearchExpression());
        assertTrue(firstSearchGroup.getSearchFlags().contains(SearchFlags.CASE_INSENSITIVE));

        AbstractGroup secondGroupObj = secondGroup.getGroupNode().getGroup();
        assertTrue(secondGroupObj instanceof SearchGroup);
        SearchGroup secondSearchGroup = (SearchGroup) secondGroupObj;
        assertEquals("groups !=~.*", secondSearchGroup.getSearchExpression());
        assertTrue(secondSearchGroup.getSearchFlags().contains(SearchFlags.CASE_INSENSITIVE));
    }

    @Test
    void addSuggestedSubGroupDoesNotCreateDuplicateGroups() {
        Mockito.reset(dialogService);

        GroupTreeViewModel testGroupTree = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, taskExecutor, new CustomLocalDragboard());
        GroupNodeViewModel testRootGroup = testGroupTree.rootGroupProperty().get();

        testGroupTree.addSuggestedSubGroup(testRootGroup);

        Mockito.reset(dialogService);

        testGroupTree.addSuggestedSubGroup(testRootGroup);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(dialogService, times(1)).notify(messageCaptor.capture());
        assertEquals(Localization.lang("All suggested groups already exist."), messageCaptor.getValue());

        List<GroupNodeViewModel> children = testRootGroup.getChildren();
        assertEquals(2, children.size());
    }

    @Test
    void addSuggestedSubGroupWritesChangesToMetaData() {
        GroupTreeViewModel spyGroupTree = Mockito.spy(groupTree);

        spyGroupTree.addSuggestedSubGroup(rootGroupViewModel);

        verify(spyGroupTree).writeGroupChangesToMetaData();
    }

    @Test
    void addSuggestedSubGroupAddsOnlyMissingFilesGroup() {
        Mockito.reset(dialogService);

        GroupTreeViewModel testGroupTree = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, taskExecutor, new CustomLocalDragboard());
        GroupNodeViewModel testRootGroup = testGroupTree.rootGroupProperty().get();

        SearchGroup withoutGroupsGroup = new SearchGroup(
                Localization.lang("Entries without groups"),
                GroupHierarchyType.INDEPENDENT,
                "groups !=~.*",
                EnumSet.of(SearchFlags.CASE_INSENSITIVE)
        );
        testRootGroup.addSubgroup(withoutGroupsGroup);

        assertEquals(1, testRootGroup.getChildren().size());

        testGroupTree.addSuggestedSubGroup(testRootGroup);

        verify(dialogService, times(1)).notify(anyString());

        List<GroupNodeViewModel> children = testRootGroup.getChildren();
        assertEquals(2, children.size());

        boolean hasWithoutFilesGroup = children.stream()
                                               .anyMatch(group -> group.getDisplayName().equals(Localization.lang("Entries without linked files")));
        assertTrue(hasWithoutFilesGroup);
    }

    @Test
    void addSuggestedSubGroupAddsOnlyMissingGroupsGroup() {
        Mockito.reset(dialogService);

        GroupTreeViewModel testGroupTree = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, taskExecutor, new CustomLocalDragboard());
        GroupNodeViewModel testRootGroup = testGroupTree.rootGroupProperty().get();

        SearchGroup withoutFilesGroup = new SearchGroup(
                Localization.lang("Entries without linked files"),
                GroupHierarchyType.INDEPENDENT,
                "file !=~.*",
                EnumSet.of(SearchFlags.CASE_INSENSITIVE)
        );
        testRootGroup.addSubgroup(withoutFilesGroup);

        assertEquals(1, testRootGroup.getChildren().size());

        testGroupTree.addSuggestedSubGroup(testRootGroup);

        verify(dialogService, times(1)).notify(anyString());

        List<GroupNodeViewModel> children = testRootGroup.getChildren();
        assertEquals(2, children.size());

        boolean hasWithoutGroupsGroup = children.stream()
                                                .anyMatch(group -> group.getDisplayName().equals(Localization.lang("Entries without groups")));
        assertTrue(hasWithoutGroupsGroup);
    }

    @Test
    void addSuggestedSubGroupUpdatesSelectedGroups() {
        GroupTreeViewModel testGroupTree = new GroupTreeViewModel(stateManager, dialogService, mock(AiService.class), preferences, taskExecutor, new CustomLocalDragboard());
        GroupNodeViewModel testRootGroup = testGroupTree.rootGroupProperty().get();

        testGroupTree.addSuggestedSubGroup(testRootGroup);

        assertFalse(testGroupTree.selectedGroupsProperty().isEmpty());
        assertEquals(2, testGroupTree.selectedGroupsProperty().size());
    }
}
