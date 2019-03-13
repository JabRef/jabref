package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;

import com.google.common.collect.ArrayListMultimap;

class PreferencesSearchHandler {

    private static PseudoClass labelHighlight = PseudoClass.getPseudoClass("search-highlight");
    private final List<PrefsTab> preferenceTabs;
    private final StringProperty searchText;
    private final ListProperty<PrefsTab> filteredPreferenceTabs;
    private final ArrayListMultimap<PrefsTab, LabeledWrapper> preferenceTabsLabelNames;
    private final ArrayList<LabeledWrapper> highlightedLabels = new ArrayList<>();

    /*
     * Wrapping Labeled
     */
    private class LabeledWrapper {

        private final Labeled labeled;

        LabeledWrapper(Labeled _labeled) {
            labeled = _labeled;
        }

        public boolean contains(String query) {
            return labeled.getText().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
        }

        public String getText() {
            return labeled.getText();
        }

        public void setHighlighted(boolean highlight) {
            labeled.pseudoClassStateChanged(labelHighlight, highlight);
        }
    }

    PreferencesSearchHandler(List<PrefsTab> preferenceTabs, StringProperty searchText) {
        this.preferenceTabs = preferenceTabs;
        this.searchText = searchText;
        this.preferenceTabsLabelNames = getPrefsTabLabelMap();
        this.filteredPreferenceTabs = new SimpleListProperty<>(FXCollections.observableArrayList(preferenceTabs));
        initializeSearchTextListener();
    }

    /**
     * Reacts upon changes in the search text.
     */
    private void initializeSearchTextListener() {
        searchText.addListener((observable, previousSearchText, newSearchText) -> {
            clearHighlights();
            if (newSearchText.isEmpty()) {
                clearSearch();
            } else {
                updateSearch(newSearchText);
            }
        });
    }

    /*
     * Filter tabs and labels based on search text
     */
    private void updateSearch(String newSearchText) {
        filterByTabName(newSearchText);
        filterByLabelName(newSearchText);
    }

    /*
     * Filter  by tab name
     */
    private void filterByTabName(String newSearchText) {
        List<PrefsTab> filteredTabs = preferenceTabs.stream()
                                                    .filter(tab -> tab.getTabName().toLowerCase(Locale.ROOT).contains(newSearchText))
                                                    .collect(Collectors.toCollection(ArrayList::new));
        filteredPreferenceTabs.setAll(filteredTabs);
    }

    /*
     * Filter by label name
     */
    private void filterByLabelName(String newSearchText) {
        for (PrefsTab tab : preferenceTabsLabelNames.keySet()) {
            // If the current tab contains a matching label
            boolean tabContainsLabel = false;
            for (LabeledWrapper labeledWrapper : preferenceTabsLabelNames.get(tab)) {
                if (labeledWrapper.contains(newSearchText)) {
                    tabContainsLabel = true;
                    highlightLabel(labeledWrapper);
                }
            }

            if (tabContainsLabel) {
                filteredPreferenceTabs.add(tab);
            }
        }
    }

    /*
     *  Highlight label
     */
    private void highlightLabel(LabeledWrapper labeledWrapper) {
        labeledWrapper.setHighlighted(true);
        highlightedLabels.add(labeledWrapper);
    }

    /*
     * Clear all previous highlights
     */
    private void clearHighlights() {
        highlightedLabels.forEach(labeledWrapper -> labeledWrapper.setHighlighted(false));
    }

    private void clearSearch() {
        filteredPreferenceTabs.setAll(preferenceTabs);
    }

    /*
     * Traverse all nodes of a PrefsTab and return a
     * mapping from PrefsTab to all its Labeled type nodes.
     */
    private ArrayListMultimap<PrefsTab, LabeledWrapper> getPrefsTabLabelMap() {
        ArrayListMultimap<PrefsTab, LabeledWrapper> prefsTabLabelMap = ArrayListMultimap.create();
        for (PrefsTab prefsTab : preferenceTabs) {
            Node builder = prefsTab.getBuilder();
            if (builder instanceof Parent) {
                Parent parentBuilder = (Parent) builder;
                for (Node child : parentBuilder.getChildrenUnmodifiable()) {
                    if (child instanceof Labeled) {
                        LabeledWrapper labeledWrapper = new LabeledWrapper((Labeled) child);
                        if (!labeledWrapper.getText().isEmpty()) {
                            prefsTabLabelMap.put(prefsTab, labeledWrapper);
                        }
                    }
                }
            }
        }
        return prefsTabLabelMap;
    }

    protected ListProperty<PrefsTab> getFilteredPreferenceTabsProperty() {
        return filteredPreferenceTabs;
    }
}
