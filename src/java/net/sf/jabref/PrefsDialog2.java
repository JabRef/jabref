/*
Copyright (C) 2003 JabRef project

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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Preferences dialog. Contains a TabbedPane, and tabs will be defined
 * in separate classes. Tabs MUST implement the PrefsTab interface,
 * since this dialog will call the storeSettings() method of all tabs
 * when the user presses ok.
 *
 * With this design, it should be very easy to add new tabs later.
 *
 */
public class PrefsDialog2 extends JDialog {

    private JabRefPreferences _prefs;
    JPanel upper = new JPanel(), 
	lower = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JTabbedPane tabbed = new JTabbedPane();
    JabRefFrame frame;

    public PrefsDialog2(JabRefFrame parent, JabRefPreferences prefs) {
	super(parent, Globals.lang("JabRef preferences"), true);
	_prefs = prefs;
	frame = parent;
	getContentPane().setLayout(gbl);
	con.weighty = 1;
	con.weightx = 1;
	con.fill = GridBagConstraints.BOTH;
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(tabbed, con);
	getContentPane().add(tabbed);	
	con.weighty = 0;
	con.gridheight = GridBagConstraints.REMAINDER;
	gbl.setConstraints(lower, con);
	getContentPane().add(lower);	

	// ----------------------------------------------------------------
	// Add tabs to tabbed here. Remember, tabs must implement PrefsTab.
	// ----------------------------------------------------------------
	tabbed.addTab("Table", new TablePrefsTab(_prefs));
	tabbed.addTab("External programs", new ExternalProgramsTab(_prefs));

	JButton 
	    ok = new JButton("Ok"),
	    cancel = new JButton("Cancel");
	ok.addActionListener(new OkAction());
	cancel.addActionListener(new CancelAction());
	lower.add(ok);
	lower.add(cancel);

	setSize(440, 570);
    }

    class OkAction extends AbstractAction {
	public OkAction() {
	    super("Ok");
	}    
	public void actionPerformed(ActionEvent e) {
	    for (int i=0; i<tabbed.getTabCount(); i++) {
		((PrefsTab)tabbed.getComponentAt(i)).storeSettings();
	    }
	    frame.output(Globals.lang("Preferences recorded."));
	    dispose();
	}
    }

    class CancelAction extends AbstractAction {
	public CancelAction() {
	    super("Cancel");

	}    
	public void actionPerformed(ActionEvent e) {
	    // Just close dialog without recording changes.
	    dispose();
	}
    }

}
