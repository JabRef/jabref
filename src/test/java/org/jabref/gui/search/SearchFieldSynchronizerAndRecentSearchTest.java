package org.jabref.gui.search;

import java.util.ArrayList;
import java.util.Arrays;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.preferences.PreferencesService;

import org.controlsfx.control.textfield.CustomTextField;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("fast")
public class SearchFieldSynchronizerAndRecentSearchTest {
    CustomTextField searchField;
    SearchFieldSynchronizer searchFieldSynchronizer = new SearchFieldSynchronizer(searchField);

    @Test
    void searchStringBuilderBuildsMixedStringCorrectly() {
        JFXPanel fxPanel = new JFXPanel();
        searchField = new CustomTextField();
        searchFieldSynchronizer = new SearchFieldSynchronizer(searchField);
        SearchItem item1 = new SearchItem("attribute","author:");
        SearchItem item2 = new SearchItem("query","julian");
        SearchItem item3 = new SearchItem("logical","OR");
        SearchItem item4 = new SearchItem("attribute","title:");
        SearchItem item5 = new SearchItem("query","Algebra");

        searchFieldSynchronizer.addSearchItem(item1);
        searchFieldSynchronizer.addSearchItem(item2);
        searchFieldSynchronizer.addSearchItem(item3);
        searchFieldSynchronizer.addSearchItem(item4);
        searchFieldSynchronizer.addSearchItem(item5);

        String searchString = searchFieldSynchronizer.searchStringBuilder();
        String trueSearchString = "author:julian OR title:Algebra";
        System.out.println(searchString);
        assertEquals(trueSearchString, searchString);
    }

    @Test
    void textFieldToListTest() {
        JFXPanel fxPanel = new JFXPanel();
        searchField = new CustomTextField();
        searchFieldSynchronizer = new SearchFieldSynchronizer(searchField);
        searchField.setText("author:Jon OR title:\"Software Engineering\"");
        ArrayList<String> list = searchFieldSynchronizer.textFieldToList();

        String[] expectedList = new String[5];
        expectedList[0] = "author:";
        expectedList[1] = "Jon";
        expectedList[2] = "OR";
        expectedList[3] = "title:";
        expectedList[4] = "\"Software Engineering\"";
        assertEquals(new ArrayList<String>(Arrays.stream(expectedList).toList()), list);

    }

    @Test
    void bracketsBalancedTest() {
        searchFieldSynchronizer = new SearchFieldSynchronizer(searchField);
        SearchItem item1 = new SearchItem("bracket", "(");
        SearchItem item2 = new SearchItem("bracket", "(");
        SearchItem item3 = new SearchItem("bracket", "(");
        SearchItem item4 = new SearchItem("bracket", ")");
        SearchItem item5 = new SearchItem("bracket", ")");
        SearchItem item6 = new SearchItem("bracket", ")");

        ObservableList<SearchItem> searchItemList = FXCollections.observableList(new ArrayList<SearchItem>());
        searchItemList.add(item1);
        searchItemList.add(item2);
        searchItemList.add(item3);
        searchItemList.add(item4);
        searchItemList.add(item5);
        searchItemList.add(item6);

        assertTrue(searchFieldSynchronizer.bracketsBalanced(searchItemList));

        SearchItem item7 = new SearchItem("bracket", ")");
        searchItemList.add(item7);

        assertFalse(searchFieldSynchronizer.bracketsBalanced(searchItemList));
    }

    @Test
    void RecentSearchRemovesDuplicates() {
        Stage mainStage = new Stage();
        JabRefFrame frame = new JabRefFrame(mainStage);
        StateManager stateManager = new StateManager();
        PreferencesService preferencesService = Globals.prefs;
        CountingUndoManager undoManager = new CountingUndoManager();
        GlobalSearchBar globalSearchBar = new GlobalSearchBar(frame, stateManager, preferencesService, undoManager);
        RecentSearch recentSearch = new RecentSearch(globalSearchBar);

        recentSearch.add("author:John");
        recentSearch.add("Software Engineering");
        recentSearch.add("title:programming");
        recentSearch.add("author:John");
        recentSearch.add("Software Engineering");


        ListView<String> RecentSearches = new ListView<>();
        RecentSearches.getItems().add("author:John");
        RecentSearches.getItems().add("Software Engineering");
        RecentSearches.getItems().add("title:programming");

        assertEquals(recentSearch.getList().getItems().toString(), RecentSearches.getItems().toString());
    }

    @Test
    void SearchBarHighlightingWorks() {
        JFXPanel fxPanel = new JFXPanel();
        searchField = new CustomTextField();
        searchFieldSynchronizer = new SearchFieldSynchronizer(searchField);
        searchField.clear();
        searchField.setStyle("-fx-border-color: blue");

        // correct syntax
        searchField.setText("author:testauthor AND title:TestTitle");

        searchFieldSynchronizer.updateSearchItemList(searchFieldSynchronizer.textFieldToList());
        searchFieldSynchronizer.syntaxHighlighting();
        assertEquals("-fx-border-color: green", searchField.getStyle());

        // wrong syntax
        searchField.setText("AND author:test");

        searchFieldSynchronizer.updateSearchItemList(searchFieldSynchronizer.textFieldToList());
        searchFieldSynchronizer.syntaxHighlighting();
        assertEquals("-fx-border-color: red", searchField.getStyle());
    }

    @Test
    void addItemDoesNotCreateInvalidSearch() {
        JFXPanel fxPanel = new JFXPanel();
        searchField = new CustomTextField();
        searchFieldSynchronizer = new SearchFieldSynchronizer(searchField);

        SearchItem item1 = new SearchItem("logical","AND");
        SearchItem item2 = new SearchItem("logical", "OR");

        searchFieldSynchronizer.addSearchItem(item1);
        searchFieldSynchronizer.addSearchItem(item2);

        assertTrue(searchFieldSynchronizer.searchItemList.isEmpty());

    }

    @Test
    void returnLatestReallyReturnsLatest() {
        JFXPanel fxPanel = new JFXPanel();
        searchField = new CustomTextField();
        searchFieldSynchronizer = new SearchFieldSynchronizer(searchField);

        searchFieldSynchronizer.addSearchItem("query","one");
        searchFieldSynchronizer.addSearchItem("query","two");

        SearchItem Three = new SearchItem("query", "three");
        searchFieldSynchronizer.addSearchItem(Three);

        assertEquals(Three, searchFieldSynchronizer.returnLatest(searchFieldSynchronizer.searchItemList));

    }
}

