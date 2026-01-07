package org.jabref.gui.frame;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

import org.jabref.gui.sidepane.SidePaneType;

public class SidePanePreferences {
    private final ObservableSet<SidePaneType> visiblePanes;
    private final ObservableMap<SidePaneType, Integer> preferredPositions;
    private final IntegerProperty webSearchFetcherSelected;

    public SidePanePreferences(Set<SidePaneType> visiblePanes,
                               Map<SidePaneType, Integer> preferredPositions,
                               int webSearchFetcherSelected) {
        this.visiblePanes = FXCollections.observableSet(visiblePanes);
        this.preferredPositions = FXCollections.observableMap(preferredPositions);
        this.webSearchFetcherSelected = new SimpleIntegerProperty(webSearchFetcherSelected);
    }

    private SidePanePreferences() {
        this(
                EnumSet.of(SidePaneType.GROUPS), // Default visible panes (OPEN_OFFICE omitted)
                Collections.emptyMap(),                                   // Default preferred positions
                0                                                         // Default web search fetcher index
        );
    }

    public static SidePanePreferences getDefault() {
        return new SidePanePreferences();
    }

    public ObservableSet<SidePaneType> visiblePanes() {
        return visiblePanes;
    }

    public void setVisiblePanes(Set<SidePaneType> panes) {
        visiblePanes.clear();
        visiblePanes.addAll(panes);
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

    public void setAll(SidePanePreferences preferences) {
        if (!preferences.visiblePanes().isEmpty()) {
            this.setVisiblePanes(preferences.visiblePanes());
        }
        this.setPreferredPositions(preferences.getPreferredPositions());
        this.webSearchFetcherSelected.set(preferences.getWebSearchFetcherSelected());
    }
}
