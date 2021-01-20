package org.jabref.gui;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jabref.gui.groups.GroupSidePane;
import org.jabref.gui.importer.fetcher.WebSearchPane;
import org.jabref.gui.openoffice.OpenOfficeSidePanel;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.preferences.PreferencesService;

/**
 * Manages which {@link SidePaneComponent}s are shown.
 */
public class SidePaneManager {

    private final SidePane sidePane;
    private final Map<SidePaneType, SidePaneComponent> components = new LinkedHashMap<>();
    private final List<SidePaneComponent> visibleComponents = new LinkedList<>();
    private final PreferencesService preferencesService;

    public SidePaneManager(PreferencesService preferencesService, JabRefFrame frame, DialogService dialogService, StateManager stateManager) {
        this.preferencesService = preferencesService;
        this.sidePane = new SidePane();

        OpenOfficePreferences openOfficePreferences = preferencesService.getOpenOfficePreferences();
        Stream.of(
                new GroupSidePane(this, preferencesService, dialogService),
                new WebSearchPane(this, preferencesService, dialogService, stateManager),
                new OpenOfficeSidePanel(this, preferencesService, frame))
              .forEach(pane -> components.put(pane.getType(), pane));

        if (preferencesService.getSidePanePreferences().isGroupsPaneVisible()) {
            show(SidePaneType.GROUPS);
        }

        if (openOfficePreferences.getShowPanel()) {
            show(SidePaneType.OPEN_OFFICE);
        }

        if (preferencesService.getSidePanePreferences().isWebSearchPaneVisible()) {
            show(SidePaneType.WEB_SEARCH);
        }

        updateView();
    }

    public SidePane getPane() {
        return sidePane;
    }

    public boolean isComponentVisible(SidePaneType type) {
        return visibleComponents.contains(getComponent(type));
    }

    public SidePaneComponent getComponent(SidePaneType type) {
        SidePaneComponent component = components.get(type);
        if (component == null) {
            throw new IllegalStateException("Side component " + type + " not registered.");
        } else {
            return component;
        }
    }

    /**
     * If the given component is visible it will be hidden and the other way around.
     */
    public void toggle(SidePaneType type) {
        if (isComponentVisible(type)) {
            hide(type);
        } else {
            show(type);
        }
    }

    /**
     * Makes sure that the given component is visible.
     */
    public void show(SidePaneType type) {
        SidePaneComponent component = getComponent(type);
        if (!visibleComponents.contains(component)) {
            // Add the new component
            visibleComponents.add(component);

            // Sort the visible components by their preferred position
            visibleComponents.sort(new PreferredIndexSort(preferencesService));

            updateView();

            component.afterOpening();
        }
    }

    /**
     * Makes sure that the given component is not visible.
     */
    public void hide(SidePaneType type) {
        SidePaneComponent component = getComponent(type);
        if (visibleComponents.contains(component)) {
            component.beforeClosing();

            visibleComponents.remove(component);

            updateView();
        }
    }

    /**
     * Stores the current configuration of visible components in the preferences,
     * so that we show components at the preferred position next time.
     */
    private void updatePreferredPositions() {
        Map<SidePaneType, Integer> preferredPositions = preferencesService.getSidePanePreferences().getPreferredPositions();

        // Use the currently shown positions of all visible components
        int index = 0;
        for (SidePaneComponent comp : visibleComponents) {
            preferredPositions.put(comp.getType(), index);
            index++;
        }
        preferencesService.storeSidePanePreferences(preferencesService.getSidePanePreferences().withPreferredPositions(preferredPositions));
    }

    /**
     * Moves the given component up.
     */
    public void moveUp(SidePaneComponent component) {
        if (visibleComponents.contains(component)) {
            int currentPosition = visibleComponents.indexOf(component);
            if (currentPosition > 0) {
                int newPosition = currentPosition - 1;
                visibleComponents.remove(currentPosition);
                visibleComponents.add(newPosition, component);

                updatePreferredPositions();
                updateView();
            }
        }
    }

    /**
     * Moves the given component down.
     */
    public void moveDown(SidePaneComponent comp) {
        if (visibleComponents.contains(comp)) {
            int currentPosition = visibleComponents.indexOf(comp);
            if (currentPosition < (visibleComponents.size() - 1)) {
                int newPosition = currentPosition + 1;
                visibleComponents.remove(currentPosition);
                visibleComponents.add(newPosition, comp);

                updatePreferredPositions();
                updateView();
            }
        }
    }

    /**
     * Updates the view to reflect changes to visible components.
     */
    private void updateView() {
        sidePane.setComponents(visibleComponents);
        sidePane.setVisible(!visibleComponents.isEmpty());
    }

    /**
     * Helper class for sorting visible components based on their preferred position.
     */
    private static class PreferredIndexSort implements Comparator<SidePaneComponent> {

        private final Map<SidePaneType, Integer> preferredPositions;

        public PreferredIndexSort(PreferencesService preferencesService) {
            preferredPositions = preferencesService.getSidePanePreferences().getPreferredPositions();
        }

        @Override
        public int compare(SidePaneComponent comp1, SidePaneComponent comp2) {
            int pos1 = preferredPositions.getOrDefault(comp1.getType(), 0);
            int pos2 = preferredPositions.getOrDefault(comp2.getType(), 0);
            return Integer.compare(pos1, pos2);
        }
    }
}
