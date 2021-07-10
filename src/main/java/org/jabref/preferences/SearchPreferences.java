package org.jabref.preferences;

import java.util.EnumSet;

import org.jabref.gui.search.SearchDisplayMode;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.search.rules.SearchRules.SearchFlags;

public class SearchPreferences {

    private final SearchDisplayMode searchDisplayMode;
    private final EnumSet<SearchFlags> searchFlags;

    public SearchPreferences(SearchDisplayMode searchDisplayMode, boolean isCaseSensitive, boolean isRegularExpression, boolean isFulltext) {
        this.searchDisplayMode = searchDisplayMode;
        searchFlags = EnumSet.noneOf(SearchFlags.class);
        if (isCaseSensitive) {
            searchFlags.add(SearchFlags.CASE_SENSITIVE);
        }
        if (isRegularExpression) {
            searchFlags.add(SearchFlags.REGULAR_EXPRESSION);
        }
        if (isFulltext) {
            searchFlags.add(SearchFlags.FULLTEXT);
        }
    }

    public SearchPreferences(SearchDisplayMode searchDisplayMode, EnumSet<SearchFlags> searchFlags) {
        this.searchDisplayMode = searchDisplayMode;
        this.searchFlags = searchFlags;
    }

    public SearchDisplayMode getSearchDisplayMode() {
        return searchDisplayMode;
    }

    public boolean isCaseSensitive() {
        return searchFlags.contains(SearchFlags.CASE_SENSITIVE);
    }

    public boolean isRegularExpression() {
        return searchFlags.contains(SearchFlags.REGULAR_EXPRESSION);
    }

    public boolean isFulltext() {
        return searchFlags.contains(SearchFlags.FULLTEXT);
    }

    public EnumSet<SearchFlags> getSearchFlags() {
        EnumSet<SearchFlags> searchFlags = EnumSet.noneOf(SearchFlags.class);
        if (isCaseSensitive()) {
            searchFlags.add(SearchRules.SearchFlags.CASE_SENSITIVE);
        }
        if (isRegularExpression()) {
            searchFlags.add(SearchRules.SearchFlags.REGULAR_EXPRESSION);
        }
        if (isFulltext()) {
            searchFlags.add(SearchRules.SearchFlags.FULLTEXT);
        }
        return searchFlags;
    }

    public SearchPreferences withSearchDisplayMode(SearchDisplayMode newSearchDisplayMode) {
        return new SearchPreferences(newSearchDisplayMode, isCaseSensitive(), isRegularExpression(), isFulltext());
    }

    public SearchPreferences withCaseSensitive(boolean newCaseSensitive) {
        return new SearchPreferences(searchDisplayMode, newCaseSensitive, isRegularExpression(), isFulltext());
    }

    public SearchPreferences withRegularExpression(boolean newRegularExpression) {
        return new SearchPreferences(searchDisplayMode, isCaseSensitive(), newRegularExpression, isFulltext());
    }

    public SearchPreferences withFulltext(boolean newFulltext) {
        return new SearchPreferences(searchDisplayMode, isCaseSensitive(), isRegularExpression(), newFulltext);
    }
}
