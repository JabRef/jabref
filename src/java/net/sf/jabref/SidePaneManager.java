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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Manages visibility of SideShowComponents in a given newly constructed
 * sidePane.
 * 
 * @version $Revision$ ($Date$)
 * 
 */
public class SidePaneManager {

	JabRefFrame frame;

	BasePanel panel;

	SidePane sidep;

	Map components = new LinkedHashMap();

	List visible = new LinkedList();

	public SidePaneManager(JabRefFrame frame) {
		this.frame = frame;
		/*
		 * Change by Morten Alver 2005.12.04: By postponing the updating of the
		 * side pane components, we get rid of the annoying latency when
		 * switching tabs:
		 */
		frame.tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				SwingUtilities.invokeLater(new Runnable() {
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
		return (components.get(name) != null);
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
		} else
			System.err.println("Side pane component '" + name + "' unknown.");
	}

	public void hide(String name) {
		Object o = components.get(name);
		if (o != null) {
			hideComponent((SidePaneComponent) o);
		} else
			System.err.println("Side pane component '" + name + "' unknown.");
	}

	public synchronized void register(String name, SidePaneComponent comp) {
		components.put(name, comp);
	}

	public synchronized void registerAndShow(String name, SidePaneComponent comp) {
		register(name, comp);
		show(name);
	}

	private synchronized void show(SidePaneComponent component) {
		if (!visible.contains(component)) {
			// Put the new component at the top of the group
			visible.add(0, component);
			updateView();
			component.componentOpening();
		}
	}

	public synchronized void hideComponent(SidePaneComponent comp) {
		if (visible.contains(comp)) {
			comp.componentClosing();
			visible.remove(comp);
			updateView();
		}
	}

    public synchronized void hideComponent(String name) {
	SidePaneComponent comp = (SidePaneComponent)components.get(name);
	if (comp == null)
	    return;
	if (visible.contains(comp)) {
	    comp.componentClosing();
	    visible.remove(comp);
	    updateView();
	}
    }

	public synchronized void unregisterComponent(String name) {
	    components.remove(name);
	}



	/**
	 * Update all side pane components to show information from the given
	 * BasePanel.
	 * 
	 * @param panel
	 */
	public void setActiveBasePanel(BasePanel panel) {
		for (Iterator i = components.keySet().iterator(); i.hasNext();) {
			Object key = i.next();
			((SidePaneComponent) components.get(key)).setActiveBasePanel(panel);
		}
	}

	public void updateView() {
		sidep.setComponents(visible);
		if (visible.size() > 0) {
			boolean wasVisible = sidep.isVisible();
			sidep.setVisible(true);
			if (!wasVisible) {
                            frame.contentPane.setDividerLocation(getPanel().getPreferredSize().width);
                        }
		} else
			sidep.setVisible(false);
	}

	public void revalidate() {
		sidep.revalidate();
		sidep.repaint();
	}
}
