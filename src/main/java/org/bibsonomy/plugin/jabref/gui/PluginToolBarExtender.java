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

package org.bibsonomy.plugin.jabref.gui;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JToolBar;


import org.bibsonomy.plugin.jabref.PluginGlobals;
import org.bibsonomy.plugin.jabref.PluginSidePaneComponent;
import org.bibsonomy.plugin.jabref.action.DeleteSelectedEntriesAction;
import org.bibsonomy.plugin.jabref.action.ExportSelectedEntriesAction;
import org.bibsonomy.plugin.jabref.action.ToggleSidePaneComponentAction;

import net.sf.jabref.JabRefFrame;

/**
 * {@link PluginToolBarExtender} add the service specific buttons to the tool bar
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class PluginToolBarExtender {

	public static void extend(JabRefFrame jabRefFrame, PluginSidePaneComponent sidePaneComponent) {
		
		for(Component rp : jabRefFrame.getComponents()) {
			
			if(rp instanceof JRootPane) {
				
				for(Component lp : ((JRootPane) rp).getComponents()) {
					
					if(lp instanceof JLayeredPane) {
						
						for(Component p : ((JLayeredPane)lp).getComponents()) {
							
							if(p instanceof JPanel) {
								
								for(Component tb : ((JPanel) p).getComponents()) {
									
									if(tb instanceof JToolBar) {
										
										JToolBar toolBar = (JToolBar) tb;
										
										JButton searchEntries = new JButton(new ToggleSidePaneComponentAction(sidePaneComponent));
										searchEntries.setText(PluginGlobals.PLUGIN_NAME);
										toolBar.add(searchEntries, 5);
										
										JButton exportEntries = new JButton(new ExportSelectedEntriesAction(jabRefFrame));
										exportEntries.setText(null);
										toolBar.add(exportEntries, 6);
										
										JButton deleteEntries = new JButton(new DeleteSelectedEntriesAction(jabRefFrame));
										deleteEntries.setText(null);
										toolBar.add(deleteEntries, 7);
																				
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
