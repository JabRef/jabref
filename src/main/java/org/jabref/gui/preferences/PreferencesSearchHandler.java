package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;

class PreferencesSearchHandler {

    private final ObservableList<PrefsTab> preferenceTabs;
    private final StringProperty searchText;
    private final List<String> labelNames;

    PreferencesSearchHandler(ObservableList<PrefsTab> preferenceTabs, StringProperty searchText) {
        this.preferenceTabs = preferenceTabs;
        this.searchText = searchText;
        this.labelNames = getLabelNames();
        initializeSearchTextListener();
    }

    /**
     * Reacts upon changes in the search text.
     */
    private void initializeSearchTextListener() {
        searchText.addListener((observable, previousSearchText, newSearchText) -> {
            if (newSearchText.isEmpty()) {
                clearSearch();
            } else {
                updateSearch(newSearchText.toLowerCase());
            }
        });
    }

    private void updateSearch(String newSearchText) {
        System.out.println("Searched: " + newSearchText); // TODO: remove system out

        // TODO: check if necessary to check tabname, the labelname could be also a tab
        preferenceTabs.stream() //
                      .map(PrefsTab::getTabName) //
                      .filter(tabName -> tabName.toLowerCase().contains(newSearchText)) //
                      .forEach(tabName -> System.out.println("Found tabname: " + tabName)); // TODO: remove system out

        for (String label : labelNames) {
            if (label.toLowerCase().contains(newSearchText)) {
                System.out.println("Found label: " + label); // TODO: remove system out
                // mark entries
            }
        }
    }

    private void clearSearch() {
        // clear
    }

    private List<String> getLabelNames() {
        List<String> labelNames = new ArrayList<>();
        for (PrefsTab prefsTab : preferenceTabs) {
            Node builder = prefsTab.getBuilder();
            if (builder instanceof Parent) {
                Parent parentBuilder = (Parent) builder;
                ObservableList<Node> children = parentBuilder.getChildrenUnmodifiable();
                for (Node child : children) {
                    if (child instanceof Labeled) {
                        Labeled childLabel = (Labeled) child;
                        labelNames.add(childLabel.getText());
                    }
                }
            }
        }
        return labelNames;
    }
}
