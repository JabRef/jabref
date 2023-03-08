package org.jabref.logic.search;

import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SearchHistoryItem {
    private StringProperty searchString;
    private StringProperty lastSearched;

    public SearchHistoryItem(String searchString) {
        this.searchString = new SimpleStringProperty(searchString);
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.lastSearched = new SimpleStringProperty(time);
    }

    public StringProperty searchStringProperty() {
        return this.searchString;
    }

    public StringProperty lastSearchedProperty() {
        return this.lastSearched;
    }
}
