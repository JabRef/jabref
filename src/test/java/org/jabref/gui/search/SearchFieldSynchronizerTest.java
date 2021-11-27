package org.jabref.gui.search;

import javafx.embed.swing.JFXPanel;

import org.controlsfx.control.textfield.CustomTextField;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("fast")
public class SearchFieldSynchronizerTest {
    CustomTextField searchField;
    SearchFieldSynchronizer searchFieldSynchronizer = new SearchFieldSynchronizer(searchField);

    @Test
    void searchStringBuilderTest() {
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
}

