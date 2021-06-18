package org.jabref.preferences;

import org.jabref.gui.search.SearchDisplayMode;

public class SearchPreferences {

    private final SearchDisplayMode searchDisplayMode;
    private final boolean isCaseSensitive;
    private final boolean isRegularExpression;
    private final boolean isFulltext;

    public SearchPreferences(SearchDisplayMode searchDisplayMode, boolean isCaseSensitive, boolean isRegularExpression, boolean isFulltext) {
        this.searchDisplayMode = searchDisplayMode;
        this.isCaseSensitive = isCaseSensitive;
        this.isRegularExpression = isRegularExpression;
        this.isFulltext = isFulltext;
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

    public boolean isFulltext() {
        return isFulltext;
    }

    public SearchPreferences withSearchDisplayMode(SearchDisplayMode newSearchDisplayMode) {
        return new SearchPreferences(newSearchDisplayMode, isCaseSensitive, isRegularExpression, isFulltext);
    }

    public SearchPreferences withCaseSensitive(boolean newCaseSensitive) {
        return new SearchPreferences(searchDisplayMode, newCaseSensitive, isRegularExpression, isFulltext);
    }

    public SearchPreferences withRegularExpression(boolean newRegularExpression) {
        return new SearchPreferences(searchDisplayMode, isCaseSensitive, newRegularExpression, isFulltext);
    }

    public SearchPreferences withFulltext(boolean newFulltext) {
        return new SearchPreferences(searchDisplayMode, isCaseSensitive, isRegularExpression, newFulltext);
    }
}
