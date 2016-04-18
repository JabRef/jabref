/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

/**
 * Manages visibility of SideShowComponents in a given newly constructed
 * sidePane.
 */
public class SidePaneManager {

    private static final Log LOGGER = LogFactory.getLog(SidePaneManager.class);

    private final JabRefFrame frame;

    private final SidePane sidep;

    private final Map<String, SidePaneComponent> components = new LinkedHashMap<>();
    private final Map<SidePaneComponent, String> componentNames = new HashMap<>();

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

    public synchronized boolean hasComponent(String name) {
        return components.containsKey(name);
    }

    public synchronized boolean isComponentVisible(String name) {
        Object o = components.get(name);
        if (o == null) {
            return false;
        } else {
            return visible.contains(o);
        }
    }

    public synchronized void toggle(String name) {
        if (isComponentVisible(name)) {
            hide(name);
        } else {
            show(name);
        }
    }

    public synchronized void show(String name) {
        Object o = components.get(name);
        if (o == null) {
            LOGGER.warn("Side pane component '" + name + "' unknown.");
        } else {
            show((SidePaneComponent) o);
        }
    }

    public synchronized void hide(String name) {
        Object o = components.get(name);
        if (o == null) {
            LOGGER.warn("Side pane component '" + name + "' unknown.");
        } else {
            hideComponent((SidePaneComponent) o);
        }
    }

    public synchronized void register(String name, SidePaneComponent comp) {
        components.put(name, comp);
        componentNames.put(comp, name);
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
    }

    public synchronized SidePaneComponent getComponent(String name) {
        return components.get(name);
    }

    private synchronized String getComponentName(SidePaneComponent comp) {
        return componentNames.get(comp);
    }

    public synchronized void hideComponent(SidePaneComponent comp) {
        if (visible.contains(comp)) {
            comp.componentClosing();
            visible.remove(comp);
            updateView();
        }
    }

    public synchronized void hideComponent(String name) {
        SidePaneComponent comp = components.get(name);
        if (comp == null) {
            return;
        }
        if (visible.contains(comp)) {
            comp.componentClosing();
            visible.remove(comp);
            updateView();
        }
    }

    private static Map<String, Integer> getPreferredPositions() {
        Map<String, Integer> preferredPositions = new HashMap<>();

        List<String> componentNames = Globals.prefs.getStringList(JabRefPreferences.SIDE_PANE_COMPONENT_NAMES);
        List<String> componentPositions = Globals.prefs
                .getStringList(JabRefPreferences.SIDE_PANE_COMPONENT_PREFERRED_POSITIONS);

        for (int i = 0; i < componentNames.size(); ++i) {
            try {
                preferredPositions.put(componentNames.get(i), Integer.parseInt(componentPositions.get(i)));
            } catch (NumberFormatException e) {
                LOGGER.info("Invalid number format for side pane component '" + componentNames.get(i) + "'.", e);
            }
        }

        return preferredPositions;
    }

    private void updatePreferredPositions() {
        Map<String, Integer> preferredPositions = getPreferredPositions();

        // Update the preferred positions of all visible components
        int index = 0;
        for (SidePaneComponent comp : visible) {
            String componentName = getComponentName(comp);
            preferredPositions.put(componentName, index);
            index++;
        }

        // Split the map into a pair of parallel String lists suitable for storage
        List<String> tmpComponentNames = new ArrayList<>(preferredPositions.keySet());
        List<String> componentPositions = preferredPositions.values().stream().map(Object::toString)
                .collect(Collectors.toList());

        Globals.prefs.putStringList(JabRefPreferences.SIDE_PANE_COMPONENT_NAMES, tmpComponentNames);
        Globals.prefs.putStringList(JabRefPreferences.SIDE_PANE_COMPONENT_PREFERRED_POSITIONS, componentPositions);
    }


    // Helper class for sorting visible components based on their preferred position
    private class PreferredIndexSort implements Comparator<SidePaneComponent> {

        private final Map<String, Integer> preferredPositions;


        public PreferredIndexSort() {
            preferredPositions = getPreferredPositions();
        }

        @Override
        public int compare(SidePaneComponent comp1, SidePaneComponent comp2) {
            int pos1 = preferredPositions.getOrDefault(getComponentName(comp1), 0);
            int pos2 = preferredPositions.getOrDefault(getComponentName(comp2), 0);
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

    public synchronized void unregisterComponent(String name) {
        componentNames.remove(components.get(name));
        components.remove(name);
    }

    /**
     * Update all side pane components to show information from the given
     * BasePanel.
     *
     * @param panel
     */

    private synchronized void setActiveBasePanel(BasePanel panel) {
        for (Map.Entry<String, SidePaneComponent> stringSidePaneComponentEntry : components.entrySet()) {
            stringSidePaneComponentEntry.getValue().setActiveBasePanel(panel);
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
