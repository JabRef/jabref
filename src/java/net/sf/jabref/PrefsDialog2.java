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
        super(parent, Globals.lang("JabRef preferences"), false);
        _prefs = prefs;
        frame = parent;
        getContentPane().setLayout(gbl);
        con.weighty = 1;
        con.weightx = 1;
        con.fill = GridBagConstraints.BOTH;
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.insets = new Insets(5, 5, 0, 5);
        gbl.setConstraints(tabbed, con);
        getContentPane().add(tabbed);
        con.weighty = 0;
        con.gridheight = GridBagConstraints.REMAINDER;
        gbl.setConstraints(lower, con);
        //lower.setBackground(GUIGlobals.lightGray);
        //getContentPane().setBackground(GUIGlobals.lightGray);
        getContentPane().add(lower);

        // ----------------------------------------------------------------
        // Add tabs to tabbed here. Remember, tabs must implement PrefsTab.
        // ----------------------------------------------------------------
        tabbed.addTab(Globals.lang("General"), new GeneralTab(frame, _prefs));
        tabbed.addTab(Globals.lang("Appearance"), new TablePrefsTab(_prefs, parent));
        //tabbed.addTab(Globals.lang("External programs"), new ExternalProgramsTab(_prefs));
        tabbed.addTab(Globals.lang("Key pattern"), new TabLabelPattern(_prefs, parent.helpDiag));
        tabbed.addTab(Globals.lang("Entry preview"), new PreviewPrefsTab(_prefs, parent.helpDiag));
	
	if (!Globals.ON_MAC)
	    tabbed.addTab(Globals.lang("Advanced"), new AdvancedTab(_prefs, parent.helpDiag));
        JButton
            ok = new JButton(Globals.lang("Ok")),
            cancel = new JButton(Globals.lang("Cancel"));
        ok.addActionListener(new OkAction());
        CancelAction cancelAction = new CancelAction();
        cancel.addActionListener(cancelAction);
        lower.add(ok);
        lower.add(cancel);

        // Key bindings:
        ActionMap am = tabbed.getActionMap();
        InputMap im = tabbed.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(frame.prefs().getKey("Close dialog"), "close");
        am.put("close", cancelAction);

        pack(); //setSize(440, 500);
    }

    class OkAction extends AbstractAction {
        public OkAction() {
            super("Ok");
        }
        public void actionPerformed(ActionEvent e) {

	    AbstractWorker worker = new AbstractWorker() {
		    public void run() {
			// First check that all tabs are ready to close:
			for (int i = 0; i < tabbed.getTabCount(); i++) {
			    if (!((PrefsTab)tabbed.getComponentAt(i)).readyToClose())
				return; // If not, break off.
			}			
			// Then store settings and close:
			for (int i = 0; i < tabbed.getTabCount(); i++) {
			    ( (PrefsTab) tabbed.getComponentAt(i)).storeSettings();
			}

			//try { Thread.sleep(3000); } catch (InterruptedException ex) {}
		    }
		    public void update() {
			dispose();
			frame.setupAllTables();
			frame.output(Globals.lang("Preferences recorded."));
		    }
		};
            worker.getWorker().run();
            worker.getCallBack().update();
            
        }
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            super("Cancel");

        }
        public void actionPerformed(ActionEvent e) {
	    dispose();
            // Just close dialog without recording changes.
            /*(new Thread() {
              public void run() {

              }
	      }).start();*/
        }
    }

}
