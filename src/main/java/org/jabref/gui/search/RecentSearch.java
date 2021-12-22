package org.jabref.gui.search;

import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class RecentSearch {
    private ListView<String> RecentSearches = new ListView<>();

    public RecentSearch(GlobalSearchBar searchBar) {
        this.RecentSearches.setOnMouseClicked(event -> {
            String query = this.RecentSearches.getSelectionModel().getSelectedItem();
            if (query != null) {
                searchBar.setSearchTerm(query);
            }
            searchBar.performSearch();
        });
    }

    public void add(String Query) {
        RecentSearches.getItems().removeIf(entry -> entry.equals(Query));
        if (Query.equals("")) {
            return;
        }

        this.RecentSearches.getItems().add(Query);
    }

    public HBox getHBox() {
        return new HBox(RecentSearches);
    }

    public VBox getVBox() {
        return new VBox(RecentSearches);
    }
}
