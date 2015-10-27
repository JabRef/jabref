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

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.util.Util;

import java.util.*;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Manages visibility of SideShowComponents in a given newly constructed
 * sidePane.
 */
public class SidePaneManager {

    private final JabRefFrame frame;

    BasePanel panel;

    private final SidePane sidep;

    private final Map<String, SidePaneComponent> components = new LinkedHashMap<String, SidePaneComponent>();
    private final Map<SidePaneComponent, String> componentNames = new HashMap<SidePaneComponent, String>();

    private final List<SidePaneComponent> visible = new LinkedList<SidePaneComponent>();


    public SidePaneManager(JabRefFrame frame) {
        this.frame = frame;
        /*
         * Change by Morten Alver 2005.12.04: By postponing the updating of the
         * side pane components, we get rid of the annoying latency when
         * switching tabs:
         */
        frame.tabbedPane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        setActiveBasePanel((BasePanel) SidePaneManager.this.frame.tabbedPane
                                .getSelectedComponent());
                    }
                });
            }
        });
        sidep = new SidePane();
        sidep.setVisible(false);
    }

    public SidePane getPanel() {
        return sidep;
    }

    public synchronized boolean hasComponent(String name) {
        return components.get(name) != null;
    }

    public boolean isComponentVisible(String name) {
        Object o = components.get(name);
        if (o != null) {
            return visible.contains(o);
        } else {
            return false;
        }
    }

    public synchronized void toggle(String name) {
        if (isComponentVisible(name)) {
            hide(name);
        } else {
            show(name);
        }
    }

    public void show(String name) {
        Object o = components.get(name);
        if (o != null) {
            show((SidePaneComponent) o);
        } else {
            System.err.println("Side pane component '" + name + "' unknown.");
        }
    }

    public void hide(String name) {
        Object o = components.get(name);
        if (o != null) {
            hideComponent((SidePaneComponent) o);
        } else {
            System.err.println("Side pane component '" + name + "' unknown.");
        }
    }

    public synchronized void register(String name, SidePaneComponent comp) {
        components.put(name, comp);
        componentNames.put(comp, name);
    }

    public synchronized void registerAndShow(String name, SidePaneComponent comp) {
        register(name, comp);
        show(name);
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

    public SidePaneComponent getComponent(String name) {
        return components.get(name);
    }

    private String getComponentName(SidePaneComponent comp) {
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

    private Map<String, Integer> getPreferredPositions() {
        Map<String, Integer> preferredPositions = new HashMap<String, Integer>();

        String[] componentNames = Globals.prefs.getStringArray(JabRefPreferences.SIDE_PANE_COMPONENT_NAMES);
        String[] componentPositions = Globals.prefs.getStringArray(JabRefPreferences.SIDE_PANE_COMPONENT_PREFERRED_POSITIONS);

        for (int i = 0; i < componentNames.length; ++i) {
            try {
                preferredPositions.put(componentNames[i], Util.intValueOf(componentPositions[i]));
            } catch (NumberFormatException e) {
                // Invalid integer format, ignore
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

        // Split the map into a pair of parallel String arrays suitable for storage
        Set<String> var = preferredPositions.keySet();
        String[] componentNames = var.toArray(new String[var.size()]);
        String[] componentPositions = new String[preferredPositions.size()];

        for (int i = 0; i < componentNames.length; ++i) {
            componentPositions[i] = preferredPositions.get(componentNames[i]).toString();
        }

        Globals.prefs.putStringArray(JabRefPreferences.SIDE_PANE_COMPONENT_NAMES, componentNames);
        Globals.prefs.putStringArray(JabRefPreferences.SIDE_PANE_COMPONENT_PREFERRED_POSITIONS, componentPositions);
    }


    // Helper class for sorting visible componenys based on their preferred position
    private class PreferredIndexSort implements Comparator<SidePaneComponent> {

        private final Map<String, Integer> preferredPositions;


        public PreferredIndexSort() {
            preferredPositions = getPreferredPositions();
        }

        @Override
        public int compare(SidePaneComponent comp1, SidePaneComponent comp2) {
            String comp1Name = getComponentName(comp1);
            String comp2Name = getComponentName(comp2);

            // Manually provide default values, since getOrDefault() doesn't exist prior to Java 8
            int pos1 = preferredPositions.containsKey(comp1Name) ? preferredPositions.get(comp1Name) : 0;
            int pos2 = preferredPositions.containsKey(comp2Name) ? preferredPositions.get(comp2Name) : 0;

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
    private void setActiveBasePanel(BasePanel panel) {
        for (Map.Entry<String, SidePaneComponent> stringSidePaneComponentEntry : components.entrySet()) {
            stringSidePaneComponentEntry.getValue().setActiveBasePanel(panel);
        }
    }

    public void updateView() {
        sidep.setComponents(visible);
        if (!visible.isEmpty()) {
            boolean wasVisible = sidep.isVisible();
            sidep.setVisible(true);
            if (!wasVisible) {
                int width = Globals.prefs.getInt(JabRefPreferences.SIDE_PANE_WIDTH);
                if (width > 0) {
                    frame.contentPane.setDividerLocation(width);
                } else {
                    frame.contentPane.setDividerLocation(getPanel().getPreferredSize().width);
                }
            }
        } else {
            if (sidep.isVisible()) {
                Globals.prefs.putInt(JabRefPreferences.SIDE_PANE_WIDTH, frame.contentPane.getDividerLocation());
            }
            sidep.setVisible(false);

        }
    }

    public void revalidate() {
        sidep.revalidate();
        sidep.repaint();
    }
}
