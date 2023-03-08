package org.jabref.logic.search;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SearchHistory {
    private ObservableList<SearchHistoryItem> history = FXCollections.observableArrayList();
    private final int historyMaxSize = 10;

    public SearchHistory() {
    }

    public ObservableList<SearchHistoryItem> getHistory() {
        return this.history;
    }

    public void remove(String toRemove) {
        int index = -1;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).searchStringProperty().get().equals(toRemove)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            // Remove the item
            history.remove(index);
        }
    }

    public void addSearch(String toAdd) {
        toAdd = toAdd.trim();

        int index = -1;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).searchStringProperty().get().equals(toAdd)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            // Remove the item
            history.remove(index);
        }

        // Create an updated item
        SearchHistoryItem newItem = new SearchHistoryItem(toAdd);

        // Add it to the end
        history.add(newItem);

        if (history.size() > historyMaxSize) {
            history.remove(0);
        }
    }
}
