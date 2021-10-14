package org.jabref.preferences;

import java.util.EnumSet;

import org.jabref.gui.search.SearchDisplayMode;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.search.rules.SearchRules.SearchFlags;

public class SearchPreferences {

    private final SearchDisplayMode searchDisplayMode;
    private final EnumSet<SearchFlags> searchFlags;
    private final boolean keepWindowOnTop;

    public SearchPreferences(SearchDisplayMode searchDisplayMode, boolean isCaseSensitive, boolean isRegularExpression, boolean isFulltext, boolean isKeepSearchString, boolean keepWindowOnTop) {
        this.searchDisplayMode = searchDisplayMode;
        this.keepWindowOnTop = keepWindowOnTop;
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
        if (isKeepSearchString) {
            searchFlags.add(SearchFlags.KEEP_SEARCH_STRING);
        }
    }

    public SearchPreferences(SearchDisplayMode searchDisplayMode, EnumSet<SearchFlags> searchFlags, boolean keepWindowOnTop) {
        this.searchDisplayMode = searchDisplayMode;
        this.searchFlags = searchFlags;
        this.keepWindowOnTop = keepWindowOnTop;
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

    public boolean isKeepSearchString() {
        return searchFlags.contains(SearchFlags.KEEP_SEARCH_STRING);
    }

    public boolean isKeepWindowOnTop() {
        return keepWindowOnTop;
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
        if (isKeepSearchString()) {
            searchFlags.add(SearchRules.SearchFlags.KEEP_SEARCH_STRING);
        }
        return searchFlags;
    }

    public SearchPreferences withSearchDisplayMode(SearchDisplayMode newSearchDisplayMode) {
        return new SearchPreferences(newSearchDisplayMode, isCaseSensitive(), isRegularExpression(), isFulltext(), isKeepSearchString(), isKeepWindowOnTop());
    }

    public SearchPreferences withCaseSensitive(boolean newCaseSensitive) {
        return new SearchPreferences(searchDisplayMode, newCaseSensitive, isRegularExpression(), isFulltext(), isKeepSearchString(), isKeepWindowOnTop());
    }

    public SearchPreferences withRegularExpression(boolean newRegularExpression) {
        return new SearchPreferences(searchDisplayMode, isCaseSensitive(), newRegularExpression, isFulltext(), isKeepSearchString(), isKeepWindowOnTop());
    }

    public SearchPreferences withFulltext(boolean newFulltext) {
        return new SearchPreferences(searchDisplayMode, isCaseSensitive(), isRegularExpression(), newFulltext, isKeepSearchString(), isKeepWindowOnTop());
    }

    public SearchPreferences withKeepSearchString(boolean newKeepSearchString) {
        return new SearchPreferences(searchDisplayMode, isCaseSensitive(), isRegularExpression(), isFulltext(), newKeepSearchString, isKeepWindowOnTop());
    }

    public SearchPreferences withKeepGlobalSearchDialogOnTop(boolean keepWindowOnTop) {
        return new SearchPreferences(searchDisplayMode, isCaseSensitive(), isRegularExpression(), isFulltext(), isKeepSearchString(), keepWindowOnTop);
    }
}
