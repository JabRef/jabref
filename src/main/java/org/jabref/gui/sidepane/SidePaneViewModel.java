package org.jabref.gui.sidepane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.StateManager;
import org.jabref.preferences.PreferencesService;

public class SidePaneViewModel extends AbstractViewModel {
    private final PreferencesService preferencesService;
    private final StateManager stateManager;

    public SidePaneViewModel(PreferencesService preferencesService, StateManager stateManager) {
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
    }

    /**
     * Stores the current configuration of visible panes in the preferences,
     * so that we show panes at the preferred position next time.
     */
    private void updatePreferredPositions() {
        Map<SidePaneType, Integer> preferredPositions = new HashMap<>(preferencesService.getSidePanePreferences().getPreferredPositions());
        IntStream.range(0, getVisiblePanes().size()).forEach(i -> preferredPositions.put(getVisiblePanes().get(i), i));
        preferencesService.getSidePanePreferences().setPreferredPositions(preferredPositions);
    }

    public ObservableList<SidePaneType> getVisiblePanes() {
        return stateManager.getVisibleSidePaneComponents();
    }

    /**
     * @return True if <b>pane</b> is visible, and it can still move up therefore we should update the view
     */
    public boolean moveUp(SidePaneType pane) {
        if (getVisiblePanes().contains(pane)) {
            int currentPosition = getVisiblePanes().indexOf(pane);
            if (currentPosition > 0) {
                int newPosition = currentPosition - 1;
                swap(getVisiblePanes(), currentPosition, newPosition);
                updatePreferredPositions();
                return true;
            }
        }
        return false;
    }

    /**
     * @return True if <b>pane</b> is visible, and it can still move down therefore we should update the view
     */
    public boolean moveDown(SidePaneType pane) {
        if (getVisiblePanes().contains(pane)) {
            int currentPosition = getVisiblePanes().indexOf(pane);
            if (currentPosition < (getVisiblePanes().size() - 1)) {
                int newPosition = currentPosition + 1;
                swap(getVisiblePanes(), currentPosition, newPosition);
                updatePreferredPositions();
                return true;
            }
        }
        return false;
    }

    /**
     * @return True if <b>pane</b> is not already shown which means the view needs to be updated
     */
    public boolean show(SidePaneType pane) {
        if (!getVisiblePanes().contains(pane)) {
            getVisiblePanes().add(pane);
            getVisiblePanes().sort(new PreferredIndexSort(preferencesService));
            return true;
        }
        return false;
    }

    /**
     * @return True if <b>pane</b> is visible which means the view needs to be updated
     */
    public boolean hide(SidePaneType pane) {
        if (getVisiblePanes().contains(pane)) {
            getVisiblePanes().remove(pane);
            return true;
        } else {
            return false;
        }
    }

    public boolean isPaneVisible(SidePaneType pane) {
        return getVisiblePanes().contains(pane);
    }
    
    private <T> void swap(ObservableList<T> observableList, int i, int j) {
        List<T> placeholder = new ArrayList<>(observableList);
        Collections.swap(placeholder, i, j);
        observableList.sort(Comparator.comparingInt(placeholder::indexOf));
    }

    /**
     * Helper class for sorting visible side panes based on their preferred position.
     */
    private static class PreferredIndexSort implements Comparator<SidePaneType> {

        private final Map<SidePaneType, Integer> preferredPositions;

        public PreferredIndexSort(PreferencesService preferencesService) {
            preferredPositions = preferencesService.getSidePanePreferences().getPreferredPositions();
        }

        @Override
        public int compare(SidePaneType type1, SidePaneType type2) {
            int pos1 = preferredPositions.getOrDefault(type1, 0);
            int pos2 = preferredPositions.getOrDefault(type2, 0);
            return Integer.compare(pos1, pos2);
        }
    }
}
