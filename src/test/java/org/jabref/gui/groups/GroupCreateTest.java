package org.jabref.gui.groups;

import java.util.Arrays;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.StateManager;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.DroppingMouseLocation;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupCreateTest {

    private StateManager stateManager;
    private BibDatabaseContext databaseContext;
    private GroupNodeViewModel viewModel;
    private TaskExecutor taskExecutor;
    private PreferencesService preferencesService;

    @BeforeEach
    void setUp() {
        stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.emptyObservableList());
        databaseContext = new BibDatabaseContext();
        taskExecutor = new CurrentThreadTaskExecutor();
        preferencesService = mock(PreferencesService.class);
        when(preferencesService.getGroupsPreferences()).thenReturn(new GroupsPreferences(
                GroupViewMode.UNION,
                true,
                true,
                new SimpleObjectProperty<>(','),
                GroupHierarchyType.REFINING));

        viewModel = getViewModelForGroup(
                new WordKeywordGroup("Test group", GroupHierarchyType.REFINING, StandardField.TITLE, "search", true, ',', false));
    }

    @Test
    void groupContextCorrect() {
        String groupName = "group";
        ExplicitGroup group = new ExplicitGroup(groupName, GroupHierarchyType.INDEPENDENT, ',');
        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);
        GroupNodeViewModel model = new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, group, new CustomLocalDragboard(), preferencesService);
        model.addEntriesToGroup(databaseContext.getEntries());
        // make sure group default hierarchy preference does not override new group
        assertEquals(model.getGroupNode().getGroup().getHierarchicalContext(),GroupHierarchyType.INDEPENDENT);
    }


    private GroupNodeViewModel getViewModelForGroup(AbstractGroup group) {
        return new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, group, new CustomLocalDragboard(), preferencesService);
    }
}
