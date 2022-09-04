package org.jabref.gui.groups;

import java.util.Optional;

import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.preferences.PreferencesService;

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
    private PreferencesService preferencesService;
    private DialogService dialogService;

    @BeforeEach
    void setUp() {
        databaseContext = new BibDatabaseContext();
        stateManager = new StateManager();
        stateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));
        taskExecutor = new CurrentThreadTaskExecutor();
        preferencesService = mock(PreferencesService.class);
        dialogService = mock(DialogService.class, Answers.RETURNS_DEEP_STUBS);

        when(preferencesService.getGroupsPreferences()).thenReturn(new GroupsPreferences(
                                                                                         GroupViewMode.UNION,
                                                                                         true,
                                                                                         true,
                                                                                         new SimpleObjectProperty<>(',')));
        groupTree = new GroupTreeViewModel(stateManager, mock(DialogService.class), preferencesService, taskExecutor, new CustomLocalDragboard());
    }

    @Test
    void rootGroupIsAllEntriesByDefault() {
        AllEntriesGroup allEntriesGroup = new AllEntriesGroup("All entries");
        assertEquals(new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, allEntriesGroup, new CustomLocalDragboard(), preferencesService), groupTree.rootGroupProperty().getValue());
    }

    @Test
    void rootGroupIsSelectedByDefault() {
        assertEquals(groupTree.rootGroupProperty().get().getGroupNode(), stateManager.getSelectedGroup(databaseContext).get(0));
    }

    @Test
    void explicitGroupsAreRemovedFromEntriesOnDelete() {
        ExplicitGroup group = new ExplicitGroup("group", GroupHierarchyType.INDEPENDENT, ',');
        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupNodeViewModel model = new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, group, new CustomLocalDragboard(), preferencesService);
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

        GroupNodeViewModel model = new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, group, new CustomLocalDragboard(), preferencesService);
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

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, preferencesService, taskExecutor, new CustomLocalDragboard());
        assertTrue(model.checkGroupFieldsForModificationsDialogNotNecessary(oldGroup, newGroup));
    }

    @Test
    void shouldNotShowDialogWhenGroupsAreEqual() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, preferencesService, taskExecutor, new CustomLocalDragboard());
        assertTrue(model.checkGroupFieldsForModificationsDialogNotNecessary(oldGroup, newGroup));
    }


    @Test
    void shouldShowDialogWhenKeywordDiffers() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordChanged", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, preferencesService, taskExecutor, new CustomLocalDragboard());
        assertFalse(model.checkGroupFieldsForModificationsDialogNotNecessary(oldGroup, newGroup));
    }

    @Test
    void shouldShowDialogWhenCaseSensitivyDiffers() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", false, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordChanged", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, dialogService, preferencesService, taskExecutor, new CustomLocalDragboard());
        assertFalse(model.checkGroupFieldsForModificationsDialogNotNecessary(oldGroup, newGroup));
    }
}
