package org.jabref.gui.preferences;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;

/// Filters the preference tabs by a search query and highlights the matching elements.
///
/// Tabs declare what is searchable themselves: the visible texts (with the node they caption)
/// come from {@link PreferencesTab#getSearchableElements()}, extra invisible synonyms from
/// {@link PreferencesTab#getSearchKeywords()}. There is no scene-graph scanning.
class PreferencesSearchHandler {

    private static final PseudoClass SEARCH_HIGHLIGHT = PseudoClass.getPseudoClass("search-highlight");

    private final List<PreferencesTab> preferenceTabs;
    private final ListProperty<PreferencesTab> filteredPreferenceTabs;
    private final Map<PreferencesTab, List<SearchableElement>> searchableElements;

    PreferencesSearchHandler(List<PreferencesTab> preferenceTabs) {
        this.preferenceTabs = preferenceTabs;
        this.searchableElements = preferenceTabs.stream()
                                                .collect(Collectors.toMap(Function.identity(), PreferencesTab::getSearchableElements));
        this.filteredPreferenceTabs = new SimpleListProperty<>(FXCollections.observableArrayList(preferenceTabs));
    }

    public void filterTabs(String query) {
        clearHighlights();

        if (query.isBlank()) {
            filteredPreferenceTabs.setAll(preferenceTabs);
            return;
        }

        String searchQuery = query.toLowerCase(Locale.ROOT);
        filteredPreferenceTabs.setAll(preferenceTabs.stream()
                                                    .filter(tab -> tabMatchesQuery(tab, searchQuery))
                                                    .toList());
    }

    /// @return true if the query occurs in the tab's keywords or visible texts; matching visible
    ///         elements are highlighted as a side effect.
    private boolean tabMatchesQuery(PreferencesTab tab, String query) {
        boolean keywordMatches = tab.getSearchKeywords().stream()
                                    .anyMatch(keyword -> keyword.toLowerCase(Locale.ROOT).contains(query));

        List<SearchableElement> matches = searchableElements.get(tab).stream()
                                                            .filter(element -> element.text().toLowerCase(Locale.ROOT).contains(query))
                                                            .toList();
        matches.forEach(element -> element.node().pseudoClassStateChanged(SEARCH_HIGHLIGHT, true));

        return keywordMatches || !matches.isEmpty();
    }

    private void clearHighlights() {
        searchableElements.values().stream()
                          .flatMap(List::stream)
                          .forEach(element -> element.node().pseudoClassStateChanged(SEARCH_HIGHLIGHT, false));
    }

    protected ListProperty<PreferencesTab> filteredPreferenceTabsProperty() {
        return filteredPreferenceTabs;
    }
}
