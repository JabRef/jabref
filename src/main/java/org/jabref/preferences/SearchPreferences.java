package org.jabref.preferences;

import java.util.EnumSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.model.search.rules.SearchRules.SearchFlags;

public class SearchPreferences {

    private final ObservableSet<SearchFlags> searchFlags;
    private final BooleanProperty keepWindowOnTop;

    public SearchPreferences(boolean isRegularExpression, boolean isFulltext, boolean isKeepSearchString, boolean isFloatingMode, boolean isSortByScore, boolean keepWindowOnTop) {
        this.keepWindowOnTop = new SimpleBooleanProperty(keepWindowOnTop);

        searchFlags = FXCollections.observableSet(EnumSet.noneOf(SearchFlags.class));
        if (isRegularExpression) {
            searchFlags.add(SearchFlags.REGULAR_EXPRESSION);
        }
        if (isFulltext) {
            searchFlags.add(SearchFlags.FULLTEXT);
        }
        if (isKeepSearchString) {
            searchFlags.add(SearchFlags.KEEP_SEARCH_STRING);
        }
        if (isFloatingMode) {
            searchFlags.add(SearchFlags.FLOATING_SEARCH);
        }
        if (isSortByScore) {
            searchFlags.add(SearchFlags.SORT_BY_SCORE);
        }
    }

    public SearchPreferences(EnumSet<SearchFlags> searchFlags, boolean keepWindowOnTop) {
        this.keepWindowOnTop = new SimpleBooleanProperty(keepWindowOnTop);

        this.searchFlags = FXCollections.observableSet(searchFlags);
    }

    public EnumSet<SearchFlags> getSearchFlags() {
        // copy of returns an exception when the EnumSet is empty
        if (searchFlags.isEmpty()) {
            return EnumSet.noneOf(SearchFlags.class);
        }
        return EnumSet.copyOf(searchFlags);
    }

    public ObservableSet<SearchFlags> getObservableSearchFlags() {
        return searchFlags;
    }

    public void setSearchFlag(SearchFlags flag, boolean value) {
        if (searchFlags.contains(flag) && !value) {
            searchFlags.remove(flag);
        } else if (!searchFlags.contains(flag) && value) {
            searchFlags.add(flag);
        }
    }

    public boolean isRegularExpression() {
        return searchFlags.contains(SearchFlags.REGULAR_EXPRESSION);
    }

    public boolean isFulltext() {
        return searchFlags.contains(SearchFlags.FULLTEXT);
    }

    public boolean shouldKeepSearchString() {
        return searchFlags.contains(SearchFlags.KEEP_SEARCH_STRING);
    }

    public boolean isFloatingMode() {
        return searchFlags.contains(SearchFlags.FLOATING_SEARCH);
    }

    public boolean isSortByScore() {
        return searchFlags.contains(SearchFlags.SORT_BY_SCORE);
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
