/*
Copyright (C) 2003  Nizar N. Batada, Morten O. Alver

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/
package net.sf.jabref;

import java.util.*;

import net.sf.jabref.groups.*;

public class SidePaneManager {

    JabRefFrame frame;
    BasePanel panel;
    SidePane sidep;
    JabRefPreferences prefs;
    MetaData metaData;
    LinkedHashMap components = new LinkedHashMap();
    Vector visible = new Vector();
    private int visibleComponents = 0;

    public SidePaneManager(JabRefFrame frame, BasePanel panel,
			   JabRefPreferences prefs, MetaData metaData) {
	this.prefs = prefs;
	this.panel = panel;
	this.metaData = metaData;
	this.frame = frame;
	sidep = new SidePane();
    }

    public SidePane getPanel() {
	return sidep;
    }

    public void populatePanel() {

        // Groups
        if (metaData.getGroups() != null) {
            panel.groupSelector = new GroupSelector(frame, panel, metaData
                    .getGroups(), this, prefs);
            register("groups", panel.groupSelector);
            if (prefs.getBoolean("groupSelectorVisible"))
                ensureVisible("groups");
        } else {
            GroupTreeNode newGroupsRoot = new GroupTreeNode(new AllEntriesGroup());
            metaData.setGroups(newGroupsRoot);
            panel.groupSelector = new GroupSelector(frame, panel,
                    newGroupsRoot, this, prefs);
            register("groups", panel.groupSelector);
        }
        
        /*
         * if (components.size() > 0) { panel.setLeftComponent(sidep); } else
         * panel.setLeftComponent(null);
         */
        updateView();

        if (components.size() > 0)
            sidep.setVisible(true);
        else
            sidep.setVisible(false);
    }

    public boolean isPanelVisible(String name) {
      Object o = components.get(name);
      if (o != null) {
        return visible.contains(o);
      } else {
        System.err.println("Side pane component '" + name + "' unknown.");
        return false;
      }
    }

    public void togglePanel(String name) {
      Object o = components.get(name);
      if (o != null) {
        if (!visible.contains(o)) {
          visible.add(o);
          //sidep.setComponents(visible);
          updateView();
          ((SidePaneComponent)o).componentOpening();
        } else {
          visible.remove(o);
          //sidep.setComponents(visible);
          updateView();
          ((SidePaneComponent)o).componentClosing();
        }

      } else System.err.println("Side pane component '"+name+"' unknown.");
    }

    public synchronized void ensureVisible(String name) {
      Object o = components.get(name);
      if (o != null) {
        if (!visible.contains(o)) {
          visible.add(o);
          //sidep.setComponents(visible);
          updateView();
          ((SidePaneComponent)o).componentOpening();
        }
      } else System.err.println("Side pane component '"+name+"' unknown.");
	/*Object o = components.get(name);
	if ((o == null) || ((SidePaneComponent)o).hasVisibility())
	    return;
	togglePanel(name);*/
    }

    public synchronized void add(String name, SidePaneComponent comp) {
      components.put(name, comp);
      visible.add(comp);
      //sidep.setComponents(visible);
      updateView();
      comp.componentOpening();
        /*sidep.add(comp);
        components.put(name, comp);
        visibleComponents++;
        if (visibleComponents == 1)
            panel.setLeftComponent(sidep);
          comp.componentOpening();
        comp.setVisibility(true);*/
    }


    public synchronized void register(String name, SidePaneComponent comp) {
      components.put(name, comp);
      /*comp.setVisible(false);
      sidep.add(comp);
      components.put(name, comp);*/
    }

    public synchronized boolean hasComponent(String name) {
      return (components.get("name") != null);
    }

    public synchronized void hideAway(String name) {
      Object o = components.get(name);
      if (o != null) {
        ((SidePaneComponent)o).componentClosing();
        if (visible.contains(o)) {
          visible.remove(o);
          //sidep.setComponents(visible);
          updateView();
        }
      } else System.err.println("Side pane component '"+name+"' unknown.");
    }

    public synchronized void hideAway(SidePaneComponent comp) {
      comp.componentClosing();
      visible.remove(comp);
      //sidep.setComponents(visible);
      updateView();
	/*comp.componentClosing();
	comp.setVisible(false);  // Swing method to make component invisible.
	comp.setVisibility(false); // Our own boolean to keep track of visibility.
	visibleComponents--;
	if (visibleComponents == 0)
	    panel.remove(sidep);
      */
    }

    private void updateView() {
      Vector toShow = new Vector();

      for (Iterator i = components.keySet().iterator(); i.hasNext(); ) {
        Object key = i.next();
        if (visible.contains(components.get(key))) {
          toShow.add(components.get(key));
        }
      }
      sidep.setComponents(toShow);
      if (visible.size() > 0) {
	  sidep.setVisible(true);
      } else
	  sidep.setVisible(false);

    }

    public void revalidate() {
      sidep.revalidate();
      sidep.repaint();
    }
}
