package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;

class PreferencesSearchHandler {

    private final ObservableList<PrefsTab> preferenceTabs;
    private final StringProperty searchText;
    private final List<String> labelNames;
    private final ObservableList<PrefsTab> filteredPreferenceTabs;
    private final Property<ObservableList<PrefsTab>> filteredPreferenceTabsProperty;


    PreferencesSearchHandler(ObservableList<PrefsTab> preferenceTabs, StringProperty searchText) {
        this.preferenceTabs = preferenceTabs;
        this.searchText = searchText;
        this.labelNames = getLabelNames();
        this.filteredPreferenceTabs = FXCollections.observableArrayList(preferenceTabs);
        this.filteredPreferenceTabsProperty = new SimpleObjectProperty<>(filteredPreferenceTabs);
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
        List<PrefsTab> filteredTabs = preferenceTabs.stream()
                .filter(tab -> tab.getTabName().toLowerCase().contains(newSearchText))
                .collect(Collectors.toCollection(ArrayList::new));

        filteredPreferenceTabs.setAll(filteredTabs);
        filteredTabs.forEach(tab -> System.out.println("Found tabname: " + tab.getTabName())); // TODO: remove system out

        for (String label : labelNames) {
            if (label.toLowerCase().contains(newSearchText)) {
                System.out.println("Found label: " + label); // TODO: remove system out
                // mark entries
            }
        }
    }

    private void clearSearch() {
        filteredPreferenceTabs.setAll(preferenceTabs);
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

    Property<ObservableList<PrefsTab>> getFilteredPreferenceTabsProperty() {
        return filteredPreferenceTabsProperty;
    }
}
