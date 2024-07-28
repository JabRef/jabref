package org.jabref.preferences;

import java.util.EnumSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.model.search.SearchFlags;

public class SearchPreferences {

    private final ObservableSet<SearchFlags> searchFlags;
    private final BooleanProperty keepWindowOnTop;
    private final DoubleProperty searchWindowHeight;
    private final DoubleProperty searchWindowWidth;
    private final DoubleProperty searchWindowDividerPosition;

    public SearchPreferences(boolean isRegularExpression, boolean isFulltext, boolean isKeepSearchString, boolean isFilteringMode, boolean keepWindowOnTop, double searchWindowHeight, double searchWindowWidth, double searchWindowDividerPosition) {
        this.keepWindowOnTop = new SimpleBooleanProperty(keepWindowOnTop);
        this.searchWindowHeight = new SimpleDoubleProperty(searchWindowHeight);
        this.searchWindowWidth = new SimpleDoubleProperty(searchWindowWidth);
        this.searchWindowDividerPosition = new SimpleDoubleProperty(searchWindowDividerPosition);

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
        if (isFilteringMode) {
            searchFlags.add(SearchFlags.FILTERING_SEARCH);
        }
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

    public boolean isFilteringMode() {
        return searchFlags.contains(SearchFlags.FILTERING_SEARCH);
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

    public double getSearchWindowHeight() {
        return this.searchWindowHeight.get();
    }

    public double getSearchWindowWidth() {
        return this.searchWindowWidth.get();
    }

    public Double getSearchWindowDividerPosition() {
        return this.searchWindowDividerPosition.get();
    }

    public DoubleProperty getSearchWindowHeightProperty() {
        return this.searchWindowHeight;
    }

    public DoubleProperty getSearchWindowWidthProperty() {
        return this.searchWindowWidth;
    }

    public DoubleProperty getSearchWindowDividerPositionProperty() {
        return this.searchWindowDividerPosition;
    }

    public void setSearchWindowHeight(double height) {
        this.searchWindowHeight.set(height);
    }

    public void setSearchWindowWidth(double width) {
        this.searchWindowWidth.set(width);
    }

    public void setSearchWindowDividerPosition(double position) {
        this.searchWindowDividerPosition.set(position);
    }
}
