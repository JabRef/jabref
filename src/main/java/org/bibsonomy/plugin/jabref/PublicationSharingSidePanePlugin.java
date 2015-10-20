/**
 *  
 *  JabRef Bibsonomy Plug-in - Plugin for the reference management 
 * 		software JabRef (http://jabref.sourceforge.net/) 
 * 		to fetch, store and delete entries from BibSonomy.
 *   
 *  Copyright (C) 2008 - 2011 Knowledge & Data Engineering Group, 
 *                            University of Kassel, Germany
 *                            http://www.kde.cs.uni-kassel.de/
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.bibsonomy.plugin.jabref;

import javax.swing.JMenuItem;

import org.bibsonomy.plugin.jabref.gui.EntryEditorTabExtender;
import org.bibsonomy.plugin.jabref.gui.PluginMenuItem;
import org.bibsonomy.plugin.jabref.gui.PluginToolBarExtender;
import org.bibsonomy.plugin.jabref.listener.PluginDataBaseChangeListener;
import org.bibsonomy.plugin.jabref.listener.TabbedPaneChangeListener;

import net.sf.jabref.JabRefFrame;
import net.sf.jabref.SidePaneComponent;
import net.sf.jabref.SidePaneManager;
import net.sf.jabref.plugin.SidePanePlugin;
/**
 * PublicationSharingSidePanelPlugin - This is the entry point of the plugin. 
 * It defines the MenuItem, the ShortcutKey and the SidePaneComponent.
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class PublicationSharingSidePanePlugin implements SidePanePlugin {
	
	/**
	 * The plugins side pane component
	 */
	private PluginSidePaneComponent sidePaneComponent;
	
	/**
	 * The plugins menu
	 */
	public JMenuItem getMenuItem() {
		
		return new PluginMenuItem(sidePaneComponent);
	}

	/**
	 * the plugins shortcut key. None defined.
	 */
	public String getShortcutKey() {
		
		return null;
	}
	
	/**
	 * the plugins side pane component
	 */
	public SidePaneComponent getSidePaneComponent() {
		
		return sidePaneComponent;
	}

	/**
	 * This method will be called from JabRef to initialize the plugin. 
	 */
	public void init(JabRefFrame jabRefFrame, SidePaneManager manager) {
		
		// create a ChangeListener to react on newly added entries.
		PluginDataBaseChangeListener l = new PluginDataBaseChangeListener(jabRefFrame);
		
		// set a ChangeListener of the Tabbed Pane which registers the databasechangelistener to all database tabs that are added later 
		jabRefFrame.getTabbedPane().addChangeListener(new TabbedPaneChangeListener(l));
		// ...but maybe we were too late: Tabs are created by another (swing)thread so the initial tab change event after tab(and database) creation may be over already.
		// Therefore add the listener to the database of the current tab if it is already present. 
		if (jabRefFrame.basePanel() != null && jabRefFrame.basePanel().database() != null) {
			jabRefFrame.basePanel().database().addDatabaseChangeListener(l);
		}
		
		this.sidePaneComponent = new PluginSidePaneComponent(manager, jabRefFrame);
		PluginToolBarExtender.extend(jabRefFrame, sidePaneComponent);
		EntryEditorTabExtender.extend();
	}

}
