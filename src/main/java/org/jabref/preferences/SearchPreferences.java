package org.jabref.preferences;

import org.jabref.gui.search.SearchDisplayMode;

public class SearchPreferences {

    private final SearchDisplayMode searchDisplayMode;
    private final boolean isCaseSensitive;
    private final boolean isRegularExpression;

    public SearchPreferences(SearchDisplayMode searchDisplayMode, boolean isCaseSensitive, boolean isRegularExpression) {
        this.searchDisplayMode = searchDisplayMode;
        this.isCaseSensitive = isCaseSensitive;
        this.isRegularExpression = isRegularExpression;
    }

    public SearchDisplayMode getSearchDisplayMode() {
        return searchDisplayMode;
    }

    public boolean isCaseSensitive() {
        return isCaseSensitive;
    }

    public boolean isRegularExpression() {
        return isRegularExpression;
    }

    public SearchPreferences withSearchDisplayMode(SearchDisplayMode newSearchDisplayMode) {
        return new SearchPreferences(newSearchDisplayMode, isCaseSensitive, isRegularExpression);
    }

    public SearchPreferences withCaseSensitive(boolean newCaseSensitive) {
        return new SearchPreferences(searchDisplayMode, newCaseSensitive, isRegularExpression);
    }

    public SearchPreferences withRegularExpression(boolean newRegularExpression) {
        return new SearchPreferences(searchDisplayMode, isCaseSensitive, newRegularExpression);
    }
}
