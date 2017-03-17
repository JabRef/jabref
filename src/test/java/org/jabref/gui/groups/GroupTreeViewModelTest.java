package org.jabref.gui.groups;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.AllEntriesGroup;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class GroupTreeViewModelTest {
    StateManager stateManager;
    GroupTreeViewModel groupTree;
    BibDatabaseContext databaseContext;

    @Before
    public void setUp() throws Exception {
        databaseContext = new BibDatabaseContext();
        stateManager = new StateManager();
        stateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));
        groupTree = new GroupTreeViewModel(stateManager, mock(DialogService.class));
    }

    @Test
    public void rootGroupIsAllEntriesByDefault() throws Exception {
        AllEntriesGroup allEntriesGroup = new AllEntriesGroup("All entries");
        assertEquals(new GroupNodeViewModel(databaseContext, stateManager, allEntriesGroup), groupTree.rootGroupProperty().getValue());
    }
}
