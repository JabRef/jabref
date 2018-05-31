package org.jabref.gui;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.gui.maintable.MainTable;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages visibility of SideShowComponents in a given newly constructed
 * sidePane.
 */
public class SidePaneManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SidePaneManager.class);

    private final JabRefFrame frame;

    private final SidePane sidep;

    private final Map<Class<? extends SidePaneComponent>, SidePaneComponent> components = new LinkedHashMap<>();

    private final List<SidePaneComponent> visible = new LinkedList<>();

    public SidePaneManager(JabRefFrame frame) {
        this.frame = frame;
        /*
         * Change by Morten Alver 2005.12.04: By postponing the updating of the
         * side pane components, we get rid of the annoying latency when
         * switching tabs:
         */
        frame.getTabbedPane().addChangeListener(event -> SwingUtilities.invokeLater(
                () -> setActiveBasePanel(SidePaneManager.this.frame.getCurrentBasePanel())));
        sidep = new SidePane();
        sidep.setVisible(false);
    }

    public SidePane getPanel() {
        return sidep;
    }

    public synchronized <T extends SidePaneComponent> boolean hasComponent(Class<T> sidePaneComponent) {
        return components.containsKey(sidePaneComponent);
    }

    public synchronized <T extends SidePaneComponent> boolean isComponentVisible(Class<T> sidePaneComponent) {
        SidePaneComponent component = components.get(sidePaneComponent);
        if (component == null) {
            return false;
        } else {
            return visible.contains(component);
        }
    }

    /**
     * If panel is visible it will be hidden and the other way around
     */
    public synchronized <T extends SidePaneComponent> void toggle(Class<T> sidePaneComponent) {
        if (isComponentVisible(sidePaneComponent)) {
            hide(sidePaneComponent);
        } else {
            show(sidePaneComponent);
        }
    }

    /**
     * If panel is hidden it will be shown and focused
     * If panel is visible but not focused it will be focused
     * If panel is visible and focused it will be hidden
     */
    public synchronized <T extends SidePaneComponent> void toggleThreeWay(Class<T> sidePaneComponent) {
        boolean isPanelFocused = Globals.getFocusListener().getFocused() == components.get(sidePaneComponent);
        if (isComponentVisible(sidePaneComponent) && isPanelFocused) {
            hide(sidePaneComponent);
        } else {
            show(sidePaneComponent);
        }
    }

    public synchronized <T extends SidePaneComponent> void show(Class<T> sidePaneComponent) {
        SidePaneComponent component = components.get(sidePaneComponent);
        if (component == null) {
            LOGGER.warn("Side pane component '" + sidePaneComponent + "' unknown.");
        } else {
            show(component);
        }
    }

    public synchronized <T extends SidePaneComponent> void hide(Class<T> sidePaneComponent) {
        SidePaneComponent component = components.get(sidePaneComponent);
        if (component == null) {
            LOGGER.warn("Side pane component '" + sidePaneComponent + "' unknown.");
        } else {
            hideComponent(component);
            if (frame.getCurrentBasePanel() != null) {
                MainTable mainTable = frame.getCurrentBasePanel().getMainTable();
                mainTable.setSelected(mainTable.getSelectedRow());
                mainTable.requestFocus();
            }
        }
    }

    public synchronized void register(SidePaneComponent comp) {
        components.put(comp.getClass(), comp);
    }

    private synchronized void show(SidePaneComponent component) {
        if (!visible.contains(component)) {
            // Put the new component at the top of the group
            visible.add(0, component);

            // Sort the visible components by their preferred position
            Collections.sort(visible, new PreferredIndexSort());

            updateView();
            component.componentOpening();
        }
        Globals.getFocusListener().setFocused(component);
        component.grabFocus();
    }

    public synchronized <T extends SidePaneComponent> SidePaneComponent getComponent(Class<T> sidePaneComponent) {
        return components.get(sidePaneComponent);
    }

    public synchronized void hideComponent(SidePaneComponent comp) {
        if (visible.contains(comp)) {
            comp.componentClosing();
            visible.remove(comp);
            updateView();
        }
    }

    public synchronized <T extends SidePaneComponent> void hideComponent(Class<T> sidePaneComponent) {
        SidePaneComponent component = components.get(sidePaneComponent);
        if (component == null) {
            return;
        }
        if (visible.contains(component)) {
            component.componentClosing();
            visible.remove(component);
            updateView();
        }
    }

    private static Map<Class<? extends SidePaneComponent>, Integer> getPreferredPositions() {
        Map<Class<? extends SidePaneComponent>, Integer> preferredPositions = new HashMap<>();

        List<String> componentNames = Globals.prefs.getStringList(JabRefPreferences.SIDE_PANE_COMPONENT_NAMES);
        List<String> componentPositions = Globals.prefs
                .getStringList(JabRefPreferences.SIDE_PANE_COMPONENT_PREFERRED_POSITIONS);

        for (int i = 0; i < componentNames.size(); ++i) {
            String componentName = componentNames.get(i);
            try {
                Class<? extends SidePaneComponent> componentClass = (Class<? extends SidePaneComponent>) Class.forName(componentName);
                preferredPositions.put(componentClass, Integer.parseInt(componentPositions.get(i)));
            } catch (ClassNotFoundException e) {
                LOGGER.debug("Following side pane could not be found: " + componentName, e);
            } catch (ClassCastException e) {
                LOGGER.debug("Following Class is no side pane: '" + componentName, e);
            } catch (NumberFormatException e) {
                LOGGER.debug("Invalid number format for side pane component '" + componentName + "'.", e);
            }
        }

        return preferredPositions;
    }

    private void updatePreferredPositions() {
        Map<Class<? extends SidePaneComponent>, Integer> preferredPositions = getPreferredPositions();

        // Update the preferred positions of all visible components
        int index = 0;
        for (SidePaneComponent comp : visible) {
            preferredPositions.put(comp.getClass(), index);
            index++;
        }

        // Split the map into a pair of parallel String lists suitable for storage
        List<String> tmpComponentNames = preferredPositions.keySet().parallelStream()
                .map(Class::getName)
                .collect(Collectors.toList());

        List<String> componentPositions = preferredPositions.values().stream().map(Object::toString)
                .collect(Collectors.toList());

        Globals.prefs.putStringList(JabRefPreferences.SIDE_PANE_COMPONENT_NAMES, tmpComponentNames);
        Globals.prefs.putStringList(JabRefPreferences.SIDE_PANE_COMPONENT_PREFERRED_POSITIONS, componentPositions);
    }


    /**
     * Helper class for sorting visible components based on their preferred position
     */
    private class PreferredIndexSort implements Comparator<SidePaneComponent> {

        private final Map<Class<? extends SidePaneComponent>, Integer> preferredPositions;


        public PreferredIndexSort() {
            preferredPositions = getPreferredPositions();
        }

        @Override
        public int compare(SidePaneComponent comp1, SidePaneComponent comp2) {
            int pos1 = preferredPositions.getOrDefault(comp1.getClass(), 0);
            int pos2 = preferredPositions.getOrDefault(comp2.getClass(), 0);
            return Integer.valueOf(pos1).compareTo(pos2);
        }
    }

    public synchronized void moveUp(SidePaneComponent comp) {
        if (visible.contains(comp)) {
            int currIndex = visible.indexOf(comp);
            if (currIndex > 0) {
                int newIndex = currIndex - 1;
                visible.remove(currIndex);
                visible.add(newIndex, comp);

                updatePreferredPositions();
                updateView();
            }
        }
    }

    public synchronized void moveDown(SidePaneComponent comp) {
        if (visible.contains(comp)) {
            int currIndex = visible.indexOf(comp);
            if (currIndex < (visible.size() - 1)) {
                int newIndex = currIndex + 1;
                visible.remove(currIndex);
                visible.add(newIndex, comp);

                updatePreferredPositions();
                updateView();
            }
        }
    }

    public synchronized <T extends SidePaneComponent> void unregisterComponent(Class<T> sidePaneComponent) {
        components.remove(sidePaneComponent);
    }

    /**
     * Update all side pane components to show information from the given
     * BasePanel.
     *
     * @param panel
     */
    private synchronized void setActiveBasePanel(BasePanel panel) {
        for (SidePaneComponent component : components.values()) {
            component.setActiveBasePanel(panel);
        }
    }

    public synchronized void updateView() {
        sidep.setComponents(visible);
        if (visible.isEmpty()) {
            if (sidep.isVisible()) {
                Globals.prefs.putInt(JabRefPreferences.SIDE_PANE_WIDTH, frame.getSplitPane().getDividerLocation());
            }
            sidep.setVisible(false);
        } else {
            boolean wasVisible = sidep.isVisible();
            sidep.setVisible(true);
            if (!wasVisible) {
                int width = Globals.prefs.getInt(JabRefPreferences.SIDE_PANE_WIDTH);
                if (width > 0) {
                    frame.getSplitPane().setDividerLocation(width);
                } else {
                    frame.getSplitPane().setDividerLocation(getPanel().getPreferredSize().width);
                }
            }
        }
    }
}
