package org.jabref.gui.groups;

import javafx.collections.FXCollections;

import org.jabref.gui.StateManager;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.WordKeywordGroup;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GroupNodeViewModelTest {

    private StateManager stateManager;
    private BibDatabaseContext databaseContext;
    private GroupNodeViewModel viewModel;
    private TaskExecutor taskExecutor;

    @Before
    public void setUp() throws Exception {
        stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.emptyObservableList());
        databaseContext = new BibDatabaseContext();
        taskExecutor = new CurrentThreadTaskExecutor();

        viewModel = getViewModelForGroup(
                new WordKeywordGroup("Test group", GroupHierarchyType.INDEPENDENT, "test", "search", true, ',', false));

    }

    @Test
    public void getDisplayNameConvertsLatexToUnicode() throws Exception {
        GroupNodeViewModel viewModel = getViewModelForGroup(
                new WordKeywordGroup("\\beta", GroupHierarchyType.INDEPENDENT, "test", "search", true, ',', false));
        assertEquals("β", viewModel.getDisplayName());
    }

    @Test
    public void alwaysMatchedByEmptySearchString() throws Exception {
        assertTrue(viewModel.isMatchedBy(""));
    }

    @Test
    public void isMatchedIfContainsPartOfSearchString() throws Exception {
        assertTrue(viewModel.isMatchedBy("est"));
    }

    private GroupNodeViewModel getViewModelForGroup(AbstractGroup group) {
        return new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, group);
    }
}
