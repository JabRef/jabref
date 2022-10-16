package org.jabref.preferences;

import java.util.EnumSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.model.search.rules.SearchRules;

public class SearchPreferences {

    private final ObservableSet<SearchRules.SearchFlags> searchFlags;
    private final BooleanProperty keepWindowOnTop;

    public SearchPreferences(boolean isRegularExpression, boolean isFulltext, boolean isKeepSearchString, boolean isFilteringMode, boolean isSortByScore, boolean keepWindowOnTop) {
        this.keepWindowOnTop = new SimpleBooleanProperty(keepWindowOnTop);

        searchFlags = FXCollections.observableSet(EnumSet.noneOf(SearchRules.SearchFlags.class));
        if (isRegularExpression) {
            searchFlags.add(SearchRules.SearchFlags.REGULAR_EXPRESSION);
        }
        if (isFulltext) {
            searchFlags.add(SearchRules.SearchFlags.FULLTEXT);
        }
        if (isKeepSearchString) {
            searchFlags.add(SearchRules.SearchFlags.KEEP_SEARCH_STRING);
        }
        if (isFilteringMode) {
            searchFlags.add(SearchRules.SearchFlags.FILTERING_SEARCH);
        }
        if (isSortByScore) {
            searchFlags.add(SearchRules.SearchFlags.SORT_BY_SCORE);
        }
    }

    public SearchPreferences(EnumSet<SearchRules.SearchFlags> searchFlags, boolean keepWindowOnTop) {
        this.keepWindowOnTop = new SimpleBooleanProperty(keepWindowOnTop);

        this.searchFlags = FXCollections.observableSet(searchFlags);
    }

    public EnumSet<SearchRules.SearchFlags> getSearchFlags() {
        // copy of returns an exception when the EnumSet is empty
        if (searchFlags.isEmpty()) {
            return EnumSet.noneOf(SearchRules.SearchFlags.class);
        }
        return EnumSet.copyOf(searchFlags);
    }

    public ObservableSet<SearchRules.SearchFlags> getObservableSearchFlags() {
        return searchFlags;
    }

    public void setSearchFlag(SearchRules.SearchFlags flag, boolean value) {
        if (searchFlags.contains(flag) && !value) {
            searchFlags.remove(flag);
        } else if (!searchFlags.contains(flag) && value) {
            searchFlags.add(flag);
        }
    }

    public boolean isRegularExpression() {
        return searchFlags.contains(SearchRules.SearchFlags.REGULAR_EXPRESSION);
    }

    public boolean isFulltext() {
        return searchFlags.contains(SearchRules.SearchFlags.FULLTEXT);
    }

    public boolean shouldKeepSearchString() {
        return searchFlags.contains(SearchRules.SearchFlags.KEEP_SEARCH_STRING);
    }

    public boolean isFilteringMode() {
        return searchFlags.contains(SearchRules.SearchFlags.FILTERING_SEARCH);
    }

    public boolean isSortByScore() {
        return searchFlags.contains(SearchRules.SearchFlags.SORT_BY_SCORE);
    }

    public boolean shouldKeepWindowOnTop() {
        return keepWindowOnTop.get();
    }

    public BooleanProperty keepWindowOnTopProperty() {
        return keepWindowOnTop;
    }

    public void setKeepWindowOnTop(boolean keepWindowOnTop) {
        this.keepWindowOnTop.set(keepWindowOnTop);
    }
}
