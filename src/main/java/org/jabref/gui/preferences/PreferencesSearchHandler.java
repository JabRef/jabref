package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;

import com.google.common.collect.ArrayListMultimap;

class PreferencesSearchHandler {

    private static PseudoClass labelHighlight = PseudoClass.getPseudoClass("search-highlight");
    private final List<PreferencesTab> preferenceTabs;
    private final ListProperty<PreferencesTab> filteredPreferenceTabs;
    private final ArrayListMultimap<PreferencesTab, Labeled> preferenceTabsLabelNames;
    private final ArrayList<Labeled> highlightedLabels = new ArrayList<>();

    PreferencesSearchHandler(List<PreferencesTab> preferenceTabs) {
        this.preferenceTabs = preferenceTabs;
        this.preferenceTabsLabelNames = getPrefsTabLabelMap();
        this.filteredPreferenceTabs = new SimpleListProperty<>(FXCollections.observableArrayList(preferenceTabs));
    }

    public void filterTabs(String text) {
        clearHighlights();
        if (text.isEmpty()) {
            clearSearch();
            return;
        }

        filteredPreferenceTabs.clear();
        for (PreferencesTab tab : preferenceTabsLabelNames.keySet()) {
            boolean tabContainsLabel = false;
            for (Labeled labeled : preferenceTabsLabelNames.get(tab)) {
                if (labelContainsText(labeled, text)) {
                    tabContainsLabel = true;
                    highlightLabel(labeled);
                }
            }
            boolean tabNameIsMatchedByQuery = tab.getTabName().toLowerCase(Locale.ROOT).contains(text);
            if (tabContainsLabel || tabNameIsMatchedByQuery) {
                filteredPreferenceTabs.add(tab);
            }
        }
    }

    private boolean labelContainsText(Labeled labeled, String text) {
        return labeled.getText().toLowerCase(Locale.ROOT).contains(text);
    }

    private void highlightLabel(Labeled labeled) {
        labeled.pseudoClassStateChanged(labelHighlight, true);
        highlightedLabels.add(labeled);
    }

    private void clearHighlights() {
        highlightedLabels.forEach(labeled -> labeled.pseudoClassStateChanged(labelHighlight, false));
    }

    private void clearSearch() {
        filteredPreferenceTabs.setAll(preferenceTabs);
    }

    /*
     * Traverse all nodes of a PreferencesTab and return a
     * mapping from PreferencesTab to all its Labeled type nodes.
     */
    private ArrayListMultimap<PreferencesTab, Labeled> getPrefsTabLabelMap() {
        ArrayListMultimap<PreferencesTab, Labeled> prefsTabLabelMap = ArrayListMultimap.create();
        for (PreferencesTab preferencesTab : preferenceTabs) {
            Node builder = preferencesTab.getBuilder();
            if (builder instanceof Parent) {
                Parent parentBuilder = (Parent) builder;
                scanLabeledControls(parentBuilder, prefsTabLabelMap, preferencesTab);

            }
        }
        return prefsTabLabelMap;
    }

    protected ListProperty<PreferencesTab> filteredPreferenceTabsProperty() {
        return filteredPreferenceTabs;
    }

    private static void scanLabeledControls(Parent parent, ArrayListMultimap<PreferencesTab, Labeled> prefsTabLabelMap, PreferencesTab preferencesTab) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (!(child instanceof Labeled)) {

                scanLabeledControls((Parent) child, prefsTabLabelMap, preferencesTab);
            } else {

                Labeled labeled = (Labeled) child;
                if (!labeled.getText().isEmpty()) {
                    prefsTabLabelMap.put(preferencesTab, labeled);
                }
            }
        }
    }
}
