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
package net.sf.jabref.groups;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Vector;
import java.util.HashSet;
import java.util.Iterator;
import net.sf.jabref.*;
import net.sf.jabref.undo.*;

/**
 * Dialog for creating or modifying groups. Operates directly on the 
 * Vector containing group information.
 */
class AutoGroupDialog extends JDialog {

    JTextField
        remove = new JTextField(60),
	field = new JTextField(60);
    JLabel
	nr = new JLabel(Globals.lang("Characters to ignore")+":"),
	nf = new JLabel(Globals.lang("Field to group by")+":");
    JButton
	ok = new JButton(Globals.lang("Ok")),
	cancel = new JButton(Globals.lang("Cancel"));
    JPanel
	main = new JPanel(),
	opt = new JPanel();
    private boolean ok_pressed = false;
    private Vector groups;
    private JabRefFrame frame;
    private BasePanel panel;
    private GroupSelector gs;

    private String /*name, regexp, field,*/ oldRemove, oldField;

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    
    public AutoGroupDialog(JabRefFrame frame_, BasePanel panel_,
			   GroupSelector gs_,
			   Vector groups_,
			   String defaultField, 
			   String defaultRemove) {
	super(frame_, Globals.lang("Automatically create groups"), true);
	frame = frame_;
	gs = gs_;
	panel = panel_;
	groups = groups_;
	field.setText(defaultField);
	remove.setText(defaultRemove);

	ActionListener okListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    
		    // Check that there are no empty strings.
		    if (field.getText().equals("")) {
			JOptionPane.showMessageDialog
			    (frame, Globals.lang("You must provide a field name "+
			     "as basis for the group creation."),
			     Globals.lang("Automatically create groups"), 
			     JOptionPane.ERROR_MESSAGE);
			return;						      
		    } 

		    ok_pressed = true;
		    dispose();

		    HashSet hs = Util.findAllWordsInField
			(panel.getDatabase(), field().toLowerCase(),
			 " "+remove());
		    Vector added = new Vector(20, 20);
		    NamedCompound ce = new NamedCompound(Globals.lang("Autogenerate groups"));
		    //boolean any = false; // To see if _any_ groups were created.
		    Iterator i = hs.iterator();
		    while (i.hasNext()) {
			String regExp = i.next().toString().toLowerCase();
			boolean found = false;
			// Check if a group with this search term already exists.
			for (int j=GroupSelector.OFFSET+1; j<groups.size();
			     j += GroupSelector.DIM) {
			    
			    if (regExp.equals(((String)groups.elementAt(j)).toLowerCase()))
				found = true;
			}
			if (!found) {
			    //any = true; // Ok, at least one was created.
			    // Add this as a new group.
			    int index = GroupSelector.findPos
				(groups, regExp);
			    added.add(new UndoableAddOrRemoveGroup
				      (gs, groups, index, true,
				       field(), Util.nCase(regExp), regExp));
			    groups.add(index, regExp);
			    groups.add(index, Util.nCase(regExp));
			    groups.add(index, field());
			}
			
		    }

		    if (added.size() > 0) {
			panel.markBaseChanged();
			gs.revalidateList(0);//(gd.index()-OFFSET)/DIM +1);
			frame.output(Globals.lang("Created groups."));

			if (added.size() > 2) {
			    for (int k=1; k<added.size()-1; k++)
				((UndoableAddOrRemoveGroup)added.elementAt(k)).
				    setRevalidate(false);
			    
			}
			for (int k=0; k<added.size(); k++)
			    ce.addEdit((UndoableAddOrRemoveGroup)added.
				       elementAt(k));
			ce.end();
			panel.undoManager.addEdit(ce);
		    }
		    

		    /*
		    if (index < 0) {
			// New group.
			index = GroupSelector.findPos(groups, name.getText());
			groups.add(index, regexp.getText());
			groups.add(index, name.getText());
			groups.add(index, field.getText());
		    } else if (index < groups.size()) {
			// Change group.
			for (int i=0; i<GroupSelector.DIM; i++)
			    groups.removeElementAt(index);
			index = GroupSelector.findPos(groups, name.getText());
			groups.add(index, regexp.getText());
			groups.add(index, name.getText());
			groups.add(index, field.getText());
		    }
		    */
		}
	    };
	remove.addActionListener(okListener);
	field.addActionListener(okListener);

	/*cancel.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    dispose();
		}
		});*/

	AbstractAction cancelAction = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    dispose();
		}
	    };

	cancel.addActionListener(cancelAction);
	ok.addActionListener(okListener);

	// Key bindings:
	ActionMap am = main.getActionMap();
	InputMap im = main.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	im.put(GUIGlobals.exitDialog, "close");
	am.put("close", cancelAction);


	// Layout starts here.
	main.setLayout(gbl);
	opt.setLayout(gbl);
	main.setBorder(BorderFactory.createTitledBorder
		       (BorderFactory.createEtchedBorder(),
			Globals.lang("Group properties")));

	// Main panel:
	con.weightx = 0;
	con.gridwidth = 1;
	con.insets = new Insets(3, 5, 3, 5);
	con.anchor = GridBagConstraints.EAST;
	con.fill = GridBagConstraints.NONE;
	con.gridx = 0;
	con.gridy = 0;
	gbl.setConstraints(nf, con);
	main.add(nf); 
	con.gridy = 1;
	gbl.setConstraints(nr, con);
	main.add(nr); 

	con.weightx = 1;
	con.anchor = GridBagConstraints.WEST;
	con.fill = GridBagConstraints.HORIZONTAL;
	con.gridy = 0; 
	con.gridx = 1;
	gbl.setConstraints(field, con);
	main.add(field); 
	con.gridy = 1;
	gbl.setConstraints(remove, con);
	main.add(remove); 
	
       	// Option buttons:
	con.gridx = GridBagConstraints.RELATIVE;
	con.gridy = GridBagConstraints.RELATIVE;
	con.weightx = 1;
	con.gridwidth = 1;
	con.anchor = GridBagConstraints.EAST;
	con.fill = GridBagConstraints.NONE;
	gbl.setConstraints(ok, con);
	opt.add(ok);
	con.anchor = GridBagConstraints.WEST;
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(cancel, con);
	opt.add(cancel);

	getContentPane().add(main, BorderLayout.CENTER);
	getContentPane().add(opt, BorderLayout.SOUTH);

	//pack();
	setSize(400, 140);

	Util.placeDialog(this, frame);
    }

    public boolean okPressed() {
	return ok_pressed;
    }

    public String oldField() { return oldField; }
    public String oldRemove() { return oldRemove; }
    public String field() { return field.getText(); }
    public String remove() { return remove.getText(); }

}
