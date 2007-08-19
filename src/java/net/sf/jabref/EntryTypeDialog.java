/*
Copyright (C) 2003 Morten O. Alver

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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class EntryTypeDialog extends JDialog implements ActionListener {

    /*
     * Dialog that prompts the user to choose a type for an entry.
     * Returns null if cancelled.
     */

    BibtexEntryType type = null;
    CancelAction cancelAction = new CancelAction();
    private final int COLNUM = 3;

    class TypeButton extends JButton implements Comparable<TypeButton> {
		BibtexEntryType type;

		public TypeButton(String label, BibtexEntryType type_) {
			super(label);
			type = type_;
		}

		public int compareTo(TypeButton o) {
			return type.getName().compareTo(o.type.getName());
		}
	}

    public EntryTypeDialog(JabRefFrame baseFrame_) {
	super(baseFrame_, true); // Set modal on.


	setTitle(Globals.lang("Select entry type"));

	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    cancelAction.actionPerformed(null);
		}
	    });

	getContentPane().setLayout(new BorderLayout());
	JPanel pan = new JPanel();
	getContentPane().add(pan, BorderLayout.CENTER);
	JPanel buttons = new JPanel();
	JButton // ok = new JButton("Ok"),
	    cancel = new JButton(Globals.lang("Cancel"));
	//ok.addActionListener(this);
	cancel.addActionListener(this);

    // Make ESC close dialog, equivalent to clicking Cancel.
	cancel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
	    .put(baseFrame_.prefs.getKey("Close dialog"), "close");
	cancel.getActionMap().put("close", cancelAction);

	//buttons.add(ok);
    ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
    //buttons.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
    bb.addGlue();
    bb.addGridded(cancel);
    bb.addGlue();

    getContentPane().add(buttons, BorderLayout.SOUTH);
	GridBagLayout gbl = new GridBagLayout();
	pan.setLayout(gbl);
	GridBagConstraints con = new GridBagConstraints();
	con.anchor = GridBagConstraints.WEST;
	con.fill = GridBagConstraints.HORIZONTAL;
	con.insets = new Insets(4, 4, 4, 4);
	int col = 0;

	for (BibtexEntryType tp : BibtexEntryType.ALL_TYPES.values()){
            //System.out.println(tp.getName()+"\n"+tp);
	    TypeButton b = new TypeButton(Util.nCase(tp.getName()), tp);
	    b.setAlignmentX(SwingConstants.LEFT);
	    b.addActionListener(this);
	    // Check if we should finish the row.
	    col++;
	    if (col == COLNUM) {
		col = 0;
		con.gridwidth = GridBagConstraints.REMAINDER;
	    } else
		con.gridwidth = 1;
	    gbl.setConstraints(b, con);
	    pan.add(b);
	}
	pan.setBorder(BorderFactory.createTitledBorder
		      (BorderFactory.createEtchedBorder(),
		       Globals.lang("Entry types")));
	//pan.setBackground(Color.white);
	//buttons.setBackground(Color.white);
	pack();
	setResizable(false);
    }

    public void actionPerformed(ActionEvent e) {
	if (e.getSource() instanceof TypeButton) {
	    type = ((TypeButton)e.getSource()).type;
	}
	dispose();
    }

    public BibtexEntryType getChoice() {
	//return type;
	return type;
    }

    class CancelAction extends AbstractAction {
	public CancelAction() {
	    super("Cancel");
	    //  new ImageIcon(GUIGlobals.imagepath+GUIGlobals.closeIconFile));
	    //putValue(SHORT_DESCRIPTION, "Cancel");
	    //putValue(MNEMONIC_KEY, GUIGlobals.closeKeyCode);
	}
	public void actionPerformed(ActionEvent e) {
	    dispose();
	}
    }


}
