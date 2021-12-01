package org.jabref.preferences;

import java.util.EnumSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.gui.search.SearchDisplayMode;
import org.jabref.model.search.rules.SearchRules.SearchFlags;

public class SearchPreferences {

    private final ObjectProperty<SearchDisplayMode> searchDisplayMode;
    private final ObservableSet<SearchFlags> searchFlags;
    private final BooleanProperty keepWindowOnTop;

    public SearchPreferences(SearchDisplayMode searchDisplayMode, boolean isCaseSensitive, boolean isRegularExpression, boolean isFulltext, boolean isKeepSearchString, boolean keepWindowOnTop) {
        this.searchDisplayMode = new SimpleObjectProperty<>(searchDisplayMode);
        this.keepWindowOnTop = new SimpleBooleanProperty(keepWindowOnTop);

        searchFlags = FXCollections.observableSet(EnumSet.noneOf(SearchFlags.class));
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
        this.searchDisplayMode = new SimpleObjectProperty<>(searchDisplayMode);
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

    protected ObservableSet<SearchFlags> getObservableSearchFlags() {
        return searchFlags;
    }

    public SearchDisplayMode getSearchDisplayMode() {
        return searchDisplayMode.get();
    }

    public ObjectProperty<SearchDisplayMode> searchDisplayModeProperty() {
        return searchDisplayMode;
    }

    public void setSearchDisplayMode(SearchDisplayMode searchDisplayMode) {
        this.searchDisplayMode.set(searchDisplayMode);
    }

    public boolean isCaseSensitive() {
        return searchFlags.contains(SearchFlags.CASE_SENSITIVE);
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
