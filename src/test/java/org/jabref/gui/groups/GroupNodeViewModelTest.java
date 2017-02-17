package org.jabref.gui.groups;

import javafx.collections.FXCollections;

import org.jabref.gui.StateManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.WordKeywordGroup;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GroupNodeViewModelTest {

    private StateManager stateManager;
    private BibDatabaseContext databaseContext;

    @Before
    public void setUp() throws Exception {
        stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.emptyObservableList());
        databaseContext = new BibDatabaseContext();
    }

    @Test
    public void getDisplayNameConvertsLatexToUnicode() throws Exception {
        GroupNodeViewModel viewModel = getViewModelForGroup(
                new WordKeywordGroup("\beta", GroupHierarchyType.INDEPENDENT, "test", "search", true, ',', false));
        assertEquals("baeiabb", viewModel.getDisplayName());
    }

    private GroupNodeViewModel getViewModelForGroup(AbstractGroup group) {
        return new GroupNodeViewModel(databaseContext, stateManager, group);
    }
}
