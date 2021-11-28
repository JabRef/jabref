package org.jabref.gui.search;

import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;

import org.controlsfx.control.textfield.CustomTextField;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("fast")
public class SearchFieldSynchronizerTest {
    CustomTextField searchField;
    SearchFieldSynchronizer searchFieldSynchronizer = new SearchFieldSynchronizer(searchField);

    @Test
    void searchStringBuilderMixed() {
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
}

