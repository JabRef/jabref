/*
Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

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

import net.sf.jabref.groups.GroupSelector;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Iterator;

public class RightClickMenu extends JPopupMenu 
    implements PopupMenuListener {
    
    BasePanel panel;
    MetaData metaData;
    JMenu groupMenu = new JMenu(Globals.lang("Add to group")),
	groupRemoveMenu = new JMenu(Globals.lang("Remove from group")),
	typeMenu = new JMenu(Globals.lang("Change entry type"));

    public RightClickMenu(BasePanel panel_, MetaData metaData_) {
	panel = panel_;
	metaData = metaData_;

	addPopupMenuListener(this);

	add(new AbstractAction(Globals.lang("Copy")) {
		public void actionPerformed(ActionEvent e) {
		    panel.runCommand("copy");
		}
	    });
	add(new AbstractAction(Globals.lang("Paste")) {
		public void actionPerformed(ActionEvent e) {
		    panel.runCommand("paste");
		}
	    });
	add(new AbstractAction(Globals.lang("Cut")) {
		public void actionPerformed(ActionEvent e) {
		    panel.runCommand("cut");
		}
	    });

	addSeparator();

	add(new AbstractAction(Globals.lang("Open pdf or ps")) {
		public void actionPerformed(ActionEvent e) {
		    panel.runCommand("openFile");
		}
	    });	
	
	add(new AbstractAction(Globals.lang("Copy BibTeX key")) {
		public void actionPerformed(ActionEvent e) {
		    panel.runCommand("copyKey");
		}
	    });

	add(new AbstractAction(Globals.lang("Copy \\cite{BibTeX key}")) {
		public void actionPerformed(ActionEvent e) {
		    panel.runCommand("copyCiteKey");
		}
	    });

	addSeparator();
	populateTypeMenu();
	add(typeMenu);
	addSeparator();
	add(groupMenu);
	add(groupRemoveMenu);
    }

    /**
     * Remove all types from the menu. Then cycle through all available
     * types, and add them.
     */
    public void populateTypeMenu() {
	typeMenu.removeAll();
	for (Iterator i=BibtexEntryType.ALL_TYPES.keySet().iterator();
	     i.hasNext();) {
	    typeMenu.add(new ChangeTypeAction
	    		 (BibtexEntryType.getType((String)i.next())));
	}
    }

    /**
     * Set the dynamic contents of "Add to group ..." submenu.
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
	Vector groups = metaData.getData("groups");
	if (groups == null) {
	    groupMenu.setEnabled(false);
	    groupRemoveMenu.setEnabled(false);
	    return;
	}
	groupMenu.setEnabled(true);
	groupRemoveMenu.setEnabled(true);
	groupMenu.removeAll();
	groupRemoveMenu.removeAll();
	for (int i=GroupSelector.OFFSET; i<groups.size()-2; 
	     i+=GroupSelector.DIM) {
	    String name = (String)groups.elementAt(i+1),
		regexp = (String)groups.elementAt(i+2),
		field = (String)groups.elementAt(i);
	    groupMenu.add(new AddToGroupAction(name, regexp, field));
	    groupRemoveMenu.add
		(new RemoveFromGroupAction(name, regexp, field));
	}

    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

    }

    public void popupMenuCanceled(PopupMenuEvent e) {

    }

    class AddToGroupAction extends AbstractAction {
	String grp, regexp, field;
	public AddToGroupAction(String grp, String regexp, String field) {
	    super(grp);
	    this.grp = grp;
	    this.regexp = regexp;
	    this.field = field;
	}
	public void actionPerformed(ActionEvent evt) {
	    panel.addToGroup(grp, regexp, field);	     
	}
    }

    class RemoveFromGroupAction extends AbstractAction {
	String grp, regexp, field;
	public RemoveFromGroupAction
	    (String grp, String regexp, String field) {

	    super(grp);
	    this.grp = grp;
	    this.regexp = regexp;
	    this.field = field;
	}
	public void actionPerformed(ActionEvent evt) {
	    panel.removeFromGroup(grp, regexp, field);	     
	}
    }

    class ChangeTypeAction extends AbstractAction {
	BibtexEntryType type;
	public ChangeTypeAction(BibtexEntryType type) {
	    super(type.getName());
	    this.type = type;
	}
	public void actionPerformed(ActionEvent evt) {
	    panel.changeType(type);	     
	}
	
    }
    
}
