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

import javax.swing.*;
import java.util.HashMap;
import java.util.Vector;
import net.sf.jabref.groups.GroupSelector;

public class SidePaneManager {

    JabRefFrame frame;
    BasePanel panel;
    SidePane sidep;
    JabRefPreferences prefs;
    MetaData metaData;
    HashMap components = new HashMap();
    private int visibleComponents = 0;

    public SidePaneManager(JabRefFrame frame, BasePanel panel,
			   JabRefPreferences prefs, MetaData metaData) {
	this.prefs = prefs;
	this.panel = panel;
	this.metaData = metaData;
	this.frame = frame;
    }
	
    public void populatePanel() {
	sidep = new SidePane(panel);
	// Groups
	if (prefs.getBoolean("groupSelectorVisible")
	    && (metaData.getData("groups") != null)) {

	    GroupSelector gs = new GroupSelector
		(frame, panel, metaData.getData("groups"), this, prefs);
	    add("groups", gs);
	}
	    
	if (components.size() > 0)
	    panel.setLeftComponent(sidep);
    }

    public void togglePanel(String name) {
	if (components.get(name) != null) {
	    if (!((SidePaneComponent)components.get(name)).isVisible()) {
		visibleComponents++;
		((SidePaneComponent)components.get(name)).setVisible(true);
		if (visibleComponents == 1)
		    panel.setLeftComponent(sidep);
		((SidePaneComponent)components.get(name)).componentOpening();
	    } else {
		hideAway((SidePaneComponent)components.get(name));
	    }
	    return; // Component already there.
	}
	if (name.equals("groups")) {
	    if (metaData.getData("groups") == null)
		metaData.putData("groups", new Vector());
	    GroupSelector gs = new GroupSelector
		(frame, panel, metaData.getData("groups"), this, prefs);
	    add("groups", gs);
	}

    }

    private synchronized void add(String name, SidePaneComponent comp) {
	sidep.add(comp);
	components.put(name, comp);
	visibleComponents++;
	if (visibleComponents == 1)
	    panel.setLeftComponent(sidep);
	comp.componentOpening();
    }

    public synchronized void hideAway(SidePaneComponent comp) {
	comp.componentClosing();
	comp.setVisible(false);
	visibleComponents--;
	if (visibleComponents == 0)
	    panel.remove(sidep);
	
    }
}
