package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;

import com.google.common.collect.ArrayListMultimap;
import org.fxmisc.easybind.EasyBind;

class PreferencesSearchHandler {

    private static PseudoClass labelHighlight = PseudoClass.getPseudoClass("search-highlight");
    private final List<PrefsTab> preferenceTabs;
    private final StringProperty searchText = new SimpleStringProperty("");
    private final ListProperty<PrefsTab> filteredPreferenceTabs;
    private final ArrayListMultimap<PrefsTab, Labeled> preferenceTabsLabelNames;
    private final ArrayList<Labeled> highlightedLabels = new ArrayList<>();

    PreferencesSearchHandler(List<PrefsTab> preferenceTabs) {
        this.preferenceTabs = preferenceTabs;
        this.preferenceTabsLabelNames = getPrefsTabLabelMap();
        this.filteredPreferenceTabs = new SimpleListProperty<>(FXCollections.observableArrayList(preferenceTabs));
        initializeSearchTextListener();
    }

    /**
     * Reacts upon changes in the search text.
     */
    private void initializeSearchTextListener() {
        EasyBind.subscribe(searchText, text -> {
            clearHighlights();
            if (text.isEmpty()) {
                clearSearch();
            } else {
                filterTabs(text.toLowerCase(Locale.ROOT));
            }
        });

    }

    private void filterTabs(String text) {
        filteredPreferenceTabs.clear();
        for (PrefsTab tab : preferenceTabsLabelNames.keySet()) {
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
     * Traverse all nodes of a PrefsTab and return a
     * mapping from PrefsTab to all its Labeled type nodes.
     */
    private ArrayListMultimap<PrefsTab, Labeled> getPrefsTabLabelMap() {
        ArrayListMultimap<PrefsTab, Labeled> prefsTabLabelMap = ArrayListMultimap.create();
        for (PrefsTab prefsTab : preferenceTabs) {
            Node builder = prefsTab.getBuilder();
            if (builder instanceof Parent) {
                Parent parentBuilder = (Parent) builder;
                for (Node child : parentBuilder.getChildrenUnmodifiable()) {
                    if (child instanceof Labeled) {
                        Labeled labeled = (Labeled) child;
                        if (!labeled.getText().isEmpty()) {
                            prefsTabLabelMap.put(prefsTab, labeled);
                        }
                    }
                }
            }
        }
        return prefsTabLabelMap;
    }

    protected ListProperty<PrefsTab> filteredPreferenceTabsProperty() {
        return filteredPreferenceTabs;
    }

    public StringProperty searchTextProperty() {
        return this.searchText;
    }

}
