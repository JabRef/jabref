package org.jabref.gui.search;

import java.util.ArrayList;

import org.controlsfx.control.textfield.CustomTextField;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dropdown")
class SearchFieldSynchronizerTest {
    private CustomTextField searchField;

    @Test
    void testGetSearchString() {
        // tests whether the actual text in the search field matches the one being recalled
        searchField = SearchTextField.create();
        searchField.setText("author:Ruh AND year:2015 OR title:Corona");
        SearchFieldSynchronizer sync = new SearchFieldSynchronizer(searchField);
        assertEquals(sync.getSearchString(), searchField.getText());
    }

    @Test
    void testSearchStringBuilder() {
        // tests whether the searchStringBuilder() method parses the correct string from searchItemList
        // TODO: currently AND/OR are stored in itemType, hence the test fails
        searchField = SearchTextField.create();
        searchField.setText("");
        SearchFieldSynchronizer sync = new SearchFieldSynchronizer(searchField);
        sync.searchItemList.add(new SearchItem("author:", "Ruh"));
        sync.searchItemList.add(new SearchItem("logicalOperator", "AND"));
        sync.searchItemList.add(new SearchItem("year:", "2015"));
        sync.searchItemList.add(new SearchItem("logicalOperator", "OR"));
        sync.searchItemList.add(new SearchItem("title:", "Corona"));
        assertEquals(sync.searchStringBuilder(), "author:Ruh AND year:2015 OR title:Corona");
    }

    @Test
    void testSearchItemList() {
        // tests whether the searchItemList is constructed correctly TODO: overhaul itemType
        // TODO: currently AND/OR are stored in itemType, hence the test fails
        searchField = SearchTextField.create();
        searchField.setText("");
        SearchFieldSynchronizer sync = new SearchFieldSynchronizer(searchField);
        sync.searchItemList.add(new SearchItem("author:", "Ruh"));
        sync.searchItemList.add(new SearchItem("logicalOperator", "AND"));
        sync.searchItemList.add(new SearchItem("year:", "2015"));
        sync.searchItemList.add(new SearchItem("logicalOperator", "OR"));
        sync.searchItemList.add(new SearchItem("title:", "Corona"));
        assertEquals(5, sync.searchItemList.size());
        assertEquals("author:", sync.searchItemList.get(0).getItemType());
        assertEquals("Ruh", sync.searchItemList.get(0).getItem());
        assertEquals("logicalOperator", sync.searchItemList.get(1).getItemType());
        assertEquals("AND", sync.searchItemList.get(1).getItem());
        assertEquals("year:", sync.searchItemList.get(2).getItemType());
        assertEquals("2015", sync.searchItemList.get(2).getItem());
        assertEquals("logicalOperator", sync.searchItemList.get(3).getItemType());
        assertEquals("OR", sync.searchItemList.get(3).getItem());
        assertEquals("title:", sync.searchItemList.get(4).getItemType());
        assertEquals("Corona", sync.searchItemList.get(4).getItem());
    }

    @Test
    void testUpdateSearchItemList() {
        // tests whether the method can parse the text from the search field correctly onto the searchItemList
        searchField = SearchTextField.create();
        searchField.setText("author:Ruh AND year:2015 OR title:Corona");
        SearchFieldSynchronizer sync = new SearchFieldSynchronizer(searchField);

        String str = searchField.getText();
        String[]words = str.split("(?<=:)|\\ ");
        ArrayList<String> list = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            if (words[i].startsWith("\"")) {
                boolean isWordAfterwards = i + 1 < words.length;
                if (isWordAfterwards && words[i + 1].endsWith("\"") && !words[i].endsWith(":")) {
                    String str2 = words[i] + " " + words[i + 1];
                    list.add(str2);
                    i++;
                } else {
                    list.add(words[i]);
                }
            } else {
                list.add(words[i]);
            }
        }

        sync.updateSearchItemList(list);

        assertEquals("author:", sync.searchItemList.get(0).getItemType());
        assertEquals("Ruh", sync.searchItemList.get(0).getItem());
        assertEquals("logicalOperator", sync.searchItemList.get(1).getItemType());
        assertEquals("AND", sync.searchItemList.get(1).getItem());
        assertEquals("year:", sync.searchItemList.get(2).getItemType());
        assertEquals("2015", sync.searchItemList.get(2).getItem());
        assertEquals("logicalOperator", sync.searchItemList.get(1).getItemType());
        assertEquals("OR", sync.searchItemList.get(1).getItem());
        assertEquals("title:", sync.searchItemList.get(2).getItemType());
        assertEquals("Corona", sync.searchItemList.get(2).getItem());
    }

    @Deprecated
    @Test
    void testIsPrevAttribute() {
        // deprecated
        searchField = SearchTextField.create();
        searchField.setText("");
        SearchFieldSynchronizer sync = new SearchFieldSynchronizer(searchField);
        sync.searchItemList.add(new SearchItem("author:", "Ruh"));
        assertEquals(true, sync.isPrevAttribute());

        sync.searchItemList.add(new SearchItem("logicalOperator", "AND"));
        assertEquals(false, sync.isPrevAttribute());

        sync.searchItemList.add(new SearchItem("year:", "2015"));
        assertEquals(true, sync.isPrevAttribute());

        sync.searchItemList.add(new SearchItem("logicalOperator", "OR"));
        assertEquals(false, sync.isPrevAttribute());

        sync.searchItemList.add(new SearchItem("title:", "Corona"));
        assertEquals(true, sync.isPrevAttribute());
    }

    @Deprecated
    @Test
    void testIsPrevOperator() {
        // deprecated
        searchField = SearchTextField.create();
        searchField.setText("");
        SearchFieldSynchronizer sync = new SearchFieldSynchronizer(searchField);
        sync.searchItemList.add(new SearchItem("author:", "Ruh"));
        assertEquals(true, sync.isPrevOperator());

        sync.searchItemList.add(new SearchItem("logicalOperator", "AND"));
        assertEquals(false, sync.isPrevOperator());

        sync.searchItemList.add(new SearchItem("year:", "2015"));
        assertEquals(true, sync.isPrevOperator());

        sync.searchItemList.add(new SearchItem("logicalOperator", "OR"));
        assertEquals(false, sync.isPrevOperator());

        sync.searchItemList.add(new SearchItem("title:", "Corona"));
        assertEquals(true, sync.isPrevOperator());
    }
}
