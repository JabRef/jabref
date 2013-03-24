/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref;

import java.util.*;

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

	Map<String, SidePaneComponent> components = new LinkedHashMap<String, SidePaneComponent>();

	List<SidePaneComponent> visible = new LinkedList<SidePaneComponent>();

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

    public SidePaneComponent getComponent(String name) {
        return components.get(name);
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
		for (Iterator<String> i = components.keySet().iterator(); i.hasNext();) {
			Object key = i.next();
			components.get(key).setActiveBasePanel(panel);
		}
	}

	public void updateView() {
		sidep.setComponents(visible);
		if (visible.size() > 0) {
			boolean wasVisible = sidep.isVisible();
			sidep.setVisible(true);
			if (!wasVisible) {
                            int width = Globals.prefs.getInt("sidePaneWidth");
                            if (width > 0)
                                frame.contentPane.setDividerLocation(width);
                            else
                                frame.contentPane.setDividerLocation(getPanel().getPreferredSize().width);
                        }
		} else {
                    if (sidep.isVisible())
                        Globals.prefs.putInt("sidePaneWidth", frame.contentPane.getDividerLocation());
                    sidep.setVisible(false);
                    
                }
	}

	public void revalidate() {
		sidep.revalidate();
		sidep.repaint();
	}
}
