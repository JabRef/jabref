package org.jabref.gui.groups;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.StateManager;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
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

    @Test
    public void treeOfAutomaticKeywordGroupIsCombined() throws Exception {
        BibEntry entryOne = new BibEntry().withField("keywords", "A > B > B1, A > C");
        BibEntry entryTwo = new BibEntry().withField("keywords", "A > D, E");
        BibEntry entryThree = new BibEntry().withField("keywords", "A > B > B2");
        databaseContext.getDatabase().insertEntries(entryOne, entryTwo, entryThree);

        AutomaticKeywordGroup group = new AutomaticKeywordGroup("Keywords", GroupHierarchyType.INDEPENDENT, "keywords", ',', '>');
        GroupNodeViewModel groupViewModel = getViewModelForGroup(group);

        WordKeywordGroup expectedGroupA = new WordKeywordGroup("A", GroupHierarchyType.INCLUDING, "keywords", "A", true, ',', true);
        WordKeywordGroup expectedGroupB = new WordKeywordGroup("B", GroupHierarchyType.INCLUDING, "keywords", "A > B", true, ',', true);
        WordKeywordGroup expectedGroupB1 = new WordKeywordGroup("B1", GroupHierarchyType.INCLUDING, "keywords", "A > B > B1", true, ',', true);
        WordKeywordGroup expectedGroupB2 = new WordKeywordGroup("B2", GroupHierarchyType.INCLUDING, "keywords", "A > B > B2", true, ',', true);
        WordKeywordGroup expectedGroupC = new WordKeywordGroup("C", GroupHierarchyType.INCLUDING, "keywords", "A > C", true, ',', true);
        WordKeywordGroup expectedGroupD = new WordKeywordGroup("D", GroupHierarchyType.INCLUDING, "keywords", "A > D", true, ',', true);
        WordKeywordGroup expectedGroupE = new WordKeywordGroup("E", GroupHierarchyType.INCLUDING, "keywords", "E", true, ',', true);
        GroupNodeViewModel expectedA = getViewModelForGroup(expectedGroupA);
        GroupTreeNode expectedB = expectedA.addSubgroup(expectedGroupB);
        expectedB.addSubgroup(expectedGroupB1);
        expectedB.addSubgroup(expectedGroupB2);
        expectedA.addSubgroup(expectedGroupC);
        expectedA.addSubgroup(expectedGroupD);
        GroupNodeViewModel expectedE = getViewModelForGroup(expectedGroupE);
        ObservableList<GroupNodeViewModel> expected = FXCollections.observableArrayList(expectedA, expectedE);

        assertEquals(expected, groupViewModel.getChildren());
    }

    private GroupNodeViewModel getViewModelForGroup(AbstractGroup group) {
        return new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, group);
    }
}
