package org.jabref.preferences;

import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.jabref.gui.SidePaneType;

public class SidePanePreferences {
    private final BooleanProperty webSearchPaneVisible;
    // private final boolean openOfficePaneVisible;
    private final BooleanProperty groupsPaneVisible;
    private final ObservableMap<SidePaneType, Integer> preferredPositions;
    private final IntegerProperty webSearchFetcherSelected;

    public SidePanePreferences(boolean webSearchPaneVisible,
                               boolean groupsPaneVisible,
                               Map<SidePaneType, Integer> preferredPositions,
                               int webSearchFetcherSelected) {
        this.webSearchPaneVisible = new SimpleBooleanProperty(webSearchPaneVisible);
        this.groupsPaneVisible = new SimpleBooleanProperty(groupsPaneVisible);
        this.preferredPositions = FXCollections.observableMap(preferredPositions);
        this.webSearchFetcherSelected = new SimpleIntegerProperty(webSearchFetcherSelected);
    }

    public boolean isWebSearchPaneVisible() {
        return webSearchPaneVisible.get();
    }

    public BooleanProperty webSearchPaneVisibleProperty() {
        return webSearchPaneVisible;
    }

    public void setWebSearchPaneVisible(boolean webSearchPaneVisible) {
        this.webSearchPaneVisible.set(webSearchPaneVisible);
    }

    public boolean isGroupsPaneVisible() {
        return groupsPaneVisible.get();
    }

    public BooleanProperty groupsPaneVisibleProperty() {
        return groupsPaneVisible;
    }

    public void setGroupsPaneVisible(boolean groupsPaneVisible) {
        this.groupsPaneVisible.set(groupsPaneVisible);
    }

    public ObservableMap<SidePaneType, Integer> getPreferredPositions() {
        return preferredPositions;
    }

    public void setPreferredPositions(Map<SidePaneType, Integer> positions) {
        preferredPositions.clear();
        preferredPositions.putAll(positions);
    }

    public int getWebSearchFetcherSelected() {
        return webSearchFetcherSelected.get();
    }

    public IntegerProperty webSearchFetcherSelectedProperty() {
        return webSearchFetcherSelected;
    }

    public void setWebSearchFetcherSelected(int webSearchFetcherSelected) {
        this.webSearchFetcherSelected.set(webSearchFetcherSelected);
    }
}
