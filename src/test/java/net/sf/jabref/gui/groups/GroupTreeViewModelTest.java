package net.sf.jabref.gui.groups;

import java.util.Optional;

import net.sf.jabref.gui.DialogService;
import net.sf.jabref.gui.StateManager;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.groups.AllEntriesGroup;

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
        assertEquals(new GroupNodeViewModel(databaseContext, allEntriesGroup), groupTree.rootGroupProperty().getValue());
    }
}
