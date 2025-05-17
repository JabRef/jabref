package org.jabref.gui.preferences;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;

import com.google.common.collect.ArrayListMultimap;

/**
 * This class is responsible for filtering and searching inside the preferences view based on a search query.
 */
class PreferencesSearchHandler {

    private static final PseudoClass CONTROL_HIGHLIGHT = PseudoClass.getPseudoClass("search-highlight");

    private final List<PreferencesTab> preferenceTabs;
    private final ListProperty<PreferencesTab> filteredPreferenceTabs;
    private final ArrayListMultimap<PreferencesTab, Control> preferenceTabsControls;

    /**
     * Initializes the PreferencesSearchHandler with the given list of preference tabs.
     *
     * @param preferenceTabs The list of preference tabs.
     */
    PreferencesSearchHandler(List<PreferencesTab> preferenceTabs) {
        this.preferenceTabs = preferenceTabs;
        this.preferenceTabsControls = getPreferenceTabsControlsMap(preferenceTabs);
        this.filteredPreferenceTabs = new SimpleListProperty<>(FXCollections.observableArrayList(preferenceTabs));
    }

    /**
     * Filters the preference tabs based on the provided search query.
     * Highlights matching controls within tabs.
     *
     * @param query The search query to filter tabs.
     */
    public void filterTabs(String query) {
        clearHighlights();

        if (query.isBlank()) {
            resetSearch();
            return;
        }

        String searchQuery = query.toLowerCase(Locale.ROOT);

        List<PreferencesTab> matchedTabs = preferenceTabs.stream()
                .filter(tab -> tabMatchesQuery(tab, searchQuery))
                .collect(Collectors.toList());

        filteredPreferenceTabs.setAll(matchedTabs);
    }

    /**
     * Checks if a tab matches the given search query either by its name or by its controls.
     *
     * @param tab The preferences tab to check.
     * @param query The search query.
     * @return True if the tab matches the query.
     */
    private boolean tabMatchesQuery(PreferencesTab tab, String query) {
        boolean tabNameMatches = tab.getTabName().toLowerCase(Locale.ROOT).contains(query);

        boolean controlMatches = preferenceTabsControls.get(tab).stream()
                .filter(control -> controlMatchesQuery(control, query))
                .peek(this::highlightControl)
                .findAny()
                .isPresent();

        return tabNameMatches || controlMatches;
    }

    /**
     * Checks if a control contains the given query in its content.
     * <p>
     * Matching criteria based on control type:
     * <ul>
     *     <li><b>Labeled</b> (e.g., Label, Button): Matches if its text contains the query (case-insensitive).</li>
     *     <li><b>ComboBox</b>: Matches if any item (converted to string) contains the query (case-insensitive).</li>
     *     <li><b>TextField</b>: Matches if its content contains the query (case-insensitive).</li>
     * </ul>
     *
     * @param control The control to check.
     * @param query   The search query.
     * @return true if the control contains the query, otherwise false.
     */
    private boolean controlMatchesQuery(Control control, String query) {
        if (control instanceof Labeled labeled && labeled.getText() != null) {
            return labeled.getText().toLowerCase(Locale.ROOT).contains(query);
        }

        if (control instanceof ComboBox<?> comboBox && !comboBox.getItems().isEmpty()) {
            return comboBox.getItems().stream()
                    .map(Object::toString)
                    .anyMatch(item -> item.toLowerCase(Locale.ROOT).contains(query));
        }

        if (control instanceof TextField textField && textField.getText() != null) {
            return textField.getText().toLowerCase(Locale.ROOT).contains(query);
        }

        return false;
    }

    /**
     * Highlights the given control to indicate a match.
     *
     * @param control The control to highlight.
     */
    private void highlightControl(Control control) {
        control.pseudoClassStateChanged(CONTROL_HIGHLIGHT, true);
    }

    /**
     * Clears all highlights from controls.
     */
    private void clearHighlights() {
        preferenceTabsControls.values().forEach(control -> control.pseudoClassStateChanged(CONTROL_HIGHLIGHT, false));
    }

    /**
     * Resets the search, displaying all preference tabs.
     */
    private void resetSearch() {
        filteredPreferenceTabs.setAll(preferenceTabs);
    }

    /**
     * Provides the property representing the filtered list of preferences tabs.
     *
     * @return The filtered preference tabs as a ListProperty.
     */
    protected ListProperty<PreferencesTab> filteredPreferenceTabsProperty() {
        return filteredPreferenceTabs;
    }

    /**
     * Builds a map of controls for each preferences tab.
     *
     * @param tabs The list of preferences tabs.
     * @return A map of preferences tabs to their controls.
     */
    private ArrayListMultimap<PreferencesTab, Control> getPreferenceTabsControlsMap(List<PreferencesTab> tabs) {
        ArrayListMultimap<PreferencesTab, Control> controlMap = ArrayListMultimap.create();
        tabs.forEach(tab -> scanControls(tab.getBuilder(), controlMap, tab));
        return controlMap;
    }

    /**
     * Recursively scans nodes and collects all controls.
     *
     * @param node The current node being scanned.
     * @param controlMap Map storing tabs and their corresponding controls.
     * @param tab The PreferencesTab associated with the current node.
     */
    private void scanControls(Node node, ArrayListMultimap<PreferencesTab, Control> controlMap, PreferencesTab tab) {
        if (node instanceof Control control) {
            controlMap.put(tab, control);
        } else if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> scanControls(child, controlMap, tab));
        }
    }
}
