/*  Copyright (C) 2003-2012 JabRef contributors.
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;

/**
 * Adds popup functionality to DragDropPane 
 * 
 * Code inspired by http://forums.devx.com/showthread.php?t=151270
 */
public class DragDropPopupPane extends DragDropPane {
	
	private JPopupMenu popupMenu = null;

	public DragDropPopupPane(AbstractAction manageSelectorsAction, AbstractAction databasePropertiesAction, AbstractAction bibtexKeyPatternAction) {
		super();
		
		addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				tabClicked(e);
			}
		});
		
		initPopupMenu(manageSelectorsAction, databasePropertiesAction, bibtexKeyPatternAction);
	}
	
	private void initPopupMenu(AbstractAction manageSelectorsAction, AbstractAction databasePropertiesAction, AbstractAction bibtexKeyPatternAction) {
		popupMenu = new JPopupMenu();
		
		JMenuItem databasePropertiesBtn = new JMenuItem(Globals.lang("Database properties"));
		databasePropertiesBtn.addActionListener(databasePropertiesAction);
		popupMenu.add(databasePropertiesBtn);
		
		JMenuItem bibtexKeyPatternBtn = new JMenuItem(Globals.lang("Bibtex key patterns"));
		bibtexKeyPatternBtn.addActionListener(bibtexKeyPatternAction);
		popupMenu.add(bibtexKeyPatternBtn);

		JMenuItem manageSelectorsBtn = new JMenuItem(Globals.lang("Manage content selectors"));
		manageSelectorsBtn.addActionListener(manageSelectorsAction);
		popupMenu.add(manageSelectorsBtn);
		
		JMenuItem closeBtn = new JMenuItem(Globals.lang("Close"), GUIGlobals.getImage("close"));
		closeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						closeSelectedTab();
					}
				});
			}
		});		
		popupMenu.add(closeBtn);
	}
	
	
	private void tabClicked(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1 && e.getClickCount() == 1) {   // if is right-click
    	  
			// display popup near location of mouse click
			popupMenu.show(e.getComponent(), e.getX(), e.getY() - 10);
		}
    }
    
    private void closeSelectedTab() {
    	// remove selected tab
    	remove( getSelectedIndex() );  
    }
    

}
