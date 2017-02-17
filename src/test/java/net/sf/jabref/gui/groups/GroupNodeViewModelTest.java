package net.sf.jabref.gui.groups;

import javafx.collections.FXCollections;

import net.sf.jabref.gui.StateManager;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.groups.AbstractGroup;
import net.sf.jabref.model.groups.GroupHierarchyType;
import net.sf.jabref.model.groups.WordKeywordGroup;

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
