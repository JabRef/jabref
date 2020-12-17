package org.jabref.preferences;

import java.util.Map;

import org.jabref.gui.SidePaneType;

public class SidePanePreferences {
    private boolean webSearchPaneVisible;
    // private final boolean openOfficePaneVisible;
    private boolean groupsPaneVisible;
    private Map<SidePaneType, Integer> preferredPositions;
    private int webSearchFetcherSelected;

    public SidePanePreferences(boolean webSearchPaneVisible,
                               boolean groupsPaneVisible,
                               Map<SidePaneType, Integer> preferredPositions,
                               int webSearchFetcherSelected) {
        this.webSearchPaneVisible = webSearchPaneVisible;
        this.groupsPaneVisible = groupsPaneVisible;
        this.preferredPositions = preferredPositions;
        this.webSearchFetcherSelected = webSearchFetcherSelected;
    }

    public boolean isWebSearchPaneVisible() {
        return webSearchPaneVisible;
    }

    public SidePanePreferences withWebSearchPaneVisible(boolean webSearchPaneVisible) {
        this.webSearchPaneVisible = webSearchPaneVisible;
        return this;
    }

    public boolean isGroupsPaneVisible() {
        return groupsPaneVisible;
    }

    public SidePanePreferences withGroupsPaneVisible(boolean groupsPaneVisible) {
        this.groupsPaneVisible = groupsPaneVisible;
        return this;
    }

    public Map<SidePaneType, Integer> getPreferredPositions() {
        return preferredPositions;
    }

    public SidePanePreferences withPreferredPositions(Map<SidePaneType, Integer> preferredPositions) {
        this.preferredPositions = preferredPositions;
        return this;
    }

    public int getWebSearchFetcherSelected() {
        return webSearchFetcherSelected;
    }

    public SidePanePreferences withWebSearchFetcherSelected(int webSearchFetcherSelected) {
        this.webSearchFetcherSelected = webSearchFetcherSelected;
        return this;
    }
}
