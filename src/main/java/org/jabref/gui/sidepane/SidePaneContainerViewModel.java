package org.jabref.gui.sidepane;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.preferences.PreferencesService;

public class SidePaneContainerViewModel extends AbstractViewModel {
    private final PreferencesService preferencesService;
    // TODO('Use preferencesService.getSidePanePreferences().visiblePanes() as the single source of truth')
    private final ObservableList<SidePaneType> visiblePanes = FXCollections.observableArrayList();

    public SidePaneContainerViewModel(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    /**
     * Stores the current configuration of visible panes in the preferences,
     * so that we show panes at the preferred position next time.
     */
    private void updatePreferredPositions() {
        Map<SidePaneType, Integer> preferredPositions = new HashMap<>(preferencesService.getSidePanePreferences().getPreferredPositions());
        IntStream.range(0, visiblePanes.size()).forEach(i -> preferredPositions.put(visiblePanes.get(i), i));
        preferencesService.getSidePanePreferences().setPreferredPositions(preferredPositions);
    }

    public ObservableList<SidePaneType> getVisiblePanes() {
        return visiblePanes;
    }

    /**
     * @return True if <b>sidePane</b> is visible, and it can still move up therefore we should update the view
     */
    public boolean moveUp(SidePaneType sidePane) {
        if (visiblePanes.contains(sidePane)) {
            int currentPosition = visiblePanes.indexOf(sidePane);
            if (currentPosition > 0) {
                int newPosition = currentPosition - 1;
                visiblePanes.remove(currentPosition);
                visiblePanes.add(newPosition, sidePane);

                updatePreferredPositions();
                return true;
            }
        }
        return false;
    }

    /**
     * @return True if <b>sidePane</b> is visible, and it can still move down therefore we should update the view
     */
    public boolean moveDown(SidePaneType sidePane) {
        if (visiblePanes.contains(sidePane)) {
            int currentPosition = visiblePanes.indexOf(sidePane);
            if (currentPosition < (visiblePanes.size() - 1)) {
                int newPosition = currentPosition + 1;
                visiblePanes.remove(currentPosition);
                visiblePanes.add(newPosition, sidePane);

                updatePreferredPositions();
                return true;
            }
        }
        return false;
    }

    /**
     * @return True if <b>sidePane</b> is not already shown which means the view needs to be updated
     */
    public boolean show(SidePaneType sidePane) {
        if (!visiblePanes.contains(sidePane)) {
            visiblePanes.add(sidePane);
            preferencesService.getSidePanePreferences().visiblePanes().add(sidePane);
            visiblePanes.sorted(new PreferredIndexSort(preferencesService));
            return true;
        }
        return false;
    }

    /**
     * @return True if <b>sidePane</b> is visible which means the view needs to be updated
     */
    public boolean hide(SidePaneType sidePane) {
        if (visiblePanes.contains(sidePane)) {
            // TODO('Before closing')
            visiblePanes.remove(sidePane);
            preferencesService.getSidePanePreferences().visiblePanes().remove(sidePane);
            return true;
        } else {
            return false;
        }
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
