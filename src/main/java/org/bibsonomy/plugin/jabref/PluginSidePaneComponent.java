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

import java.awt.Dimension;

import org.bibsonomy.plugin.jabref.gui.PluginSidePanel;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.SidePaneComponent;
import net.sf.jabref.SidePaneManager;

/**
 * {@link PluginSidePaneComponent} holds the dimension of the {@link PluginSidePanel}.
 * Additionally it sets the icon and the name.
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class PluginSidePaneComponent extends SidePaneComponent {

	private static final long serialVersionUID = 8823318411871094917L;
	
	/**
	 * the side pane manager
	 */
	private SidePaneManager manager;
	
	/**
	 * the jabref frame
	 */
	private JabRefFrame jabRefFrame;
	
	public PluginSidePaneComponent(SidePaneManager manager, JabRefFrame jabRefFrame) {
		
		// set the icon and the name
		super(manager, PluginSidePaneComponent.class.getResource("/images/tag-label.png"), "BibSonomy");
		
		this.manager = manager;
		this.jabRefFrame = jabRefFrame;
		
		// add the sidepanel
		super.add(new PluginSidePanel(jabRefFrame));
	}

	/**
	 * get the jabRefFrame
	 * @return the {@link JabRefFrame}
	 */
	public JabRefFrame getJabRefFrame() {
		
		return jabRefFrame;
	}
	
	/**
	 * get the sidePaneManager
	 * @return the {@link SidePaneManager}
	 */
	public SidePaneManager getSidePaneManager() {
		
		return manager;
	}
	
	/**
	 * set the preferred size to 550 pixels
	 */
	@Override
	public Dimension getPreferredSize() {
	
		return new Dimension(GUIGlobals.SPLIT_PANE_DIVIDER_LOCATION, 550);
	}
	
	/**
	 * set the maximum size to 550 pixels
	 */
	@Override
	public Dimension getMaximumSize() {
		
		return getPreferredSize();
	}
}
