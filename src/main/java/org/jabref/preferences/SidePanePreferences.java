package org.jabref.preferences;

import java.util.Map;
import java.util.Set;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

import org.jabref.gui.sidepane.SidePaneType;

public class SidePanePreferences {
    private final ObservableSet<SidePaneType> visiblePanes;
    private final ObservableMap<SidePaneType, Integer> preferredPositions;
    private final IntegerProperty webSearchFetcherSelected;

    private final DoubleProperty sidePaneWidth;

    public SidePanePreferences(Set<SidePaneType> visiblePanes,
            Map<SidePaneType, Integer> preferredPositions,
            int webSearchFetcherSelected,
            double sidePaneWidth) {
        this.visiblePanes = FXCollections.observableSet(visiblePanes);
        this.preferredPositions = FXCollections.observableMap(preferredPositions);
        this.webSearchFetcherSelected = new SimpleIntegerProperty(webSearchFetcherSelected);
        this.sidePaneWidth = new SimpleDoubleProperty(sidePaneWidth);
    }

    public ObservableSet<SidePaneType> visiblePanes() {
        return visiblePanes;
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

    public DoubleProperty sidePaneWidthProperty() {
        return sidePaneWidth;
    }

    public double getSidePaneWidth() {
        return sidePaneWidthProperty().get();
    }

    public void setSidePaneWidth(double width) {
        sidePaneWidthProperty().set(width);
    }
}
