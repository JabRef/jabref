package org.jabref.gui.groups;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GroupTreeViewModelTest {
    StateManager stateManager;
    GroupTreeViewModel groupTree;
    BibDatabaseContext databaseContext;
    private TaskExecutor taskExecutor;

    @BeforeEach
    void setUp() throws Exception {
        databaseContext = new BibDatabaseContext();
        stateManager = new StateManager();
        stateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));
        taskExecutor = new CurrentThreadTaskExecutor();
        groupTree = new GroupTreeViewModel(stateManager, mock(DialogService.class), mock(PreferencesService.class), taskExecutor, new CustomLocalDragboard());
    }

    @Test
    void rootGroupIsAllEntriesByDefault() throws Exception {
        AllEntriesGroup allEntriesGroup = new AllEntriesGroup("All entries");
        assertEquals(new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, allEntriesGroup, new CustomLocalDragboard(), mock(PreferencesService.class)), groupTree.rootGroupProperty().getValue());
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

        GroupNodeViewModel model = new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, group, new CustomLocalDragboard(), mock(PreferencesService.class));
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

        GroupNodeViewModel model = new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, group, new CustomLocalDragboard(), mock(PreferencesService.class));
        model.addEntriesToGroup(databaseContext.getEntries());
        groupTree.removeGroupsAndSubGroupsFromEntries(model);

        assertEquals(groupName, entry.getField(StandardField.KEYWORDS).get());
    }
}
