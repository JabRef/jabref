package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;

class PreferencesSearchHandler {

    private final List<PrefsTab> preferenceTabs;
    private final StringProperty searchText;
    private final ListProperty<PrefsTab> filteredPreferenceTabs;
    private final Map<LabelWrapper, Set<PrefsTab>> preferenceTabsLabelNames;
    private final ArrayList<LabelWrapper> highlightedLabels = new ArrayList<>();
    private static PseudoClass labelHighlight = PseudoClass.getPseudoClass("search-highlight");

    /*
     * Wrapping Labeled
     */
    private class LabelWrapper {

        private final Labeled labeled;

        LabelWrapper(Labeled _labeled) {
            labeled = _labeled;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof LabelWrapper) {
                LabelWrapper labelObj = (LabelWrapper) obj;
                return labelObj.getText() == this.getText();
            }

            return false;
        }

        @Override
        public int hashCode() {
            return labeled.hashCode();
        }

        public boolean contains(String query) {
            return labeled.getText().toLowerCase().contains(query.toLowerCase());
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
                                                    .filter(tab -> tab.getTabName().toLowerCase().contains(newSearchText))
                                                    .collect(Collectors.toCollection(ArrayList::new));
        filteredPreferenceTabs.setAll(filteredTabs);
    }

    /*
     * Filter by label name
     */
    private void filterByLabelName(String newSearchText) {
        for (LabelWrapper labelWrapper : preferenceTabsLabelNames.keySet()) {
            if (labelWrapper.contains(newSearchText)) {
                Set<PrefsTab> prefsTabs = preferenceTabsLabelNames.get(labelWrapper);
                for (PrefsTab tab : prefsTabs) {
                    if (!filteredPreferenceTabs.contains(tab)) {
                        filteredPreferenceTabs.add(tab);
                    }
                }
                highlightLabel(labelWrapper);
            }
        }
    }

    /*
     *  Highlight label
     */
    private void highlightLabel(LabelWrapper labelWrapper) {
        labelWrapper.setHighlighted(true);
        highlightedLabels.add(labelWrapper);
    }

    /*
     * Clear all previous highlights
     */
    private void clearHighlights() {
        highlightedLabels.forEach(labelWrapper -> labelWrapper.setHighlighted(false));
    }

    private void clearSearch() {
        filteredPreferenceTabs.setAll(preferenceTabs);
    }

    /*
     * Returns a Mapping from a label name to the corresponding tabs
     *
     * [LabelWrapper] -> {PrefsTab1, PrefsTab2, . . . }
     */
    private Map<LabelWrapper, Set<PrefsTab>> getPrefsTabLabelMap() {
        Map<LabelWrapper, Set<PrefsTab>> prefsTabLabelMap = new HashMap<>();
        for (PrefsTab prefsTab : preferenceTabs) {
            Node builder = prefsTab.getBuilder();
            if (builder instanceof Parent) {
                Parent parentBuilder = (Parent) builder;
                for (Node child : parentBuilder.getChildrenUnmodifiable()) {
                    if (child instanceof Labeled) {
                        LabelWrapper labelWrapper = new LabelWrapper((Labeled) child);
                        if (!labelWrapper.getText().isEmpty()) {
                            Set<PrefsTab> prefsTabsForLabel = prefsTabLabelMap.getOrDefault(labelWrapper, new HashSet<>());
                            prefsTabsForLabel.add(prefsTab);
                            prefsTabLabelMap.put(labelWrapper, prefsTabsForLabel);
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
