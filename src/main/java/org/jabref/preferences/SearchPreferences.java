package org.jabref.preferences;

import java.util.EnumSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.gui.search.SearchDisplayMode;
import org.jabref.model.search.rules.SearchRules.SearchFlags;

import com.google.common.annotations.VisibleForTesting;

public class SearchPreferences {

    private final ObservableSet<SearchFlags> searchFlags;
    private final BooleanProperty keepWindowOnTop;
    private final DoubleProperty searchWindowHeight;
    private final DoubleProperty searchWindowWidth;
    private final DoubleProperty searchWindowDividerPosition;
    private final ObjectProperty<SearchDisplayMode> searchDisplayMode;

    public SearchPreferences(SearchDisplayMode searchDisplayMode, boolean isCaseSensitive, boolean isRegularExpression, boolean isFulltext, boolean isKeepSearchString, boolean keepWindowOnTop, double searchWindowHeight, double searchWindowWidth, double searchWindowDividerPosition) {
        this.keepWindowOnTop = new SimpleBooleanProperty(keepWindowOnTop);
        this.searchWindowHeight = new SimpleDoubleProperty(searchWindowHeight);
        this.searchWindowWidth = new SimpleDoubleProperty(searchWindowWidth);
        this.searchWindowDividerPosition = new SimpleDoubleProperty(searchWindowDividerPosition);

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

        this.searchDisplayMode = new SimpleObjectProperty<>(searchDisplayMode);
        this.setSearchWindowHeight(searchWindowHeight);
        this.setSearchWindowWidth(searchWindowWidth);
    }

    @VisibleForTesting
    public SearchPreferences(SearchDisplayMode searchDisplayMode, EnumSet<SearchFlags> searchFlags, boolean keepWindowOnTop) {
        this.searchFlags = FXCollections.observableSet(searchFlags);
        this.keepWindowOnTop = new SimpleBooleanProperty(keepWindowOnTop);
        this.searchWindowHeight = new SimpleDoubleProperty(0);
        this.searchWindowWidth = new SimpleDoubleProperty(0);
        this.searchWindowDividerPosition = new SimpleDoubleProperty(0);
        this.searchDisplayMode = new SimpleObjectProperty<>(searchDisplayMode);
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

    public SearchDisplayMode getSearchDisplayMode() {
        return searchDisplayMode.get();
    }

    public DoubleProperty getSearchWindowDividerPositionProperty() {
        return this.searchWindowDividerPosition;
    }

    public ObjectProperty<SearchDisplayMode> searchDisplayModeProperty() {
        return this.searchDisplayMode;
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

    public void setSearchDisplayMode(SearchDisplayMode searchDisplayMode) {
        this.searchDisplayMode.set(searchDisplayMode);
    }
}
