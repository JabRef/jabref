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
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Preferences dialog. Contains a TabbedPane, and tabs will be defined
 * in separate classes. Tabs MUST implement the PrefsTab interface,
 * since this dialog will call the storeSettings() method of all tabs
 * when the user presses ok.
 *
 * With this design, it should be very easy to add new tabs later.
 *
 */
public class PrefsDialog3 extends JDialog {

    private JabRefPreferences _prefs;
    JPanel upper = new JPanel(),
        lower = new JPanel(),
	main = new JPanel();
    JList chooser;
    CardLayout cardLayout = new CardLayout();
    HashMap panels = new HashMap();
    JabRefFrame frame;

    public PrefsDialog3(JabRefFrame parent, JabRefPreferences prefs) {
        super(parent, Globals.lang("JabRef preferences"), false);
        _prefs = prefs;
        frame = parent;
        getContentPane().setLayout(new BorderLayout());
	getContentPane().add(upper, BorderLayout.CENTER);
        getContentPane().add(lower, BorderLayout.SOUTH);

        // ----------------------------------------------------------------
        // Add tabs to tabbed here. Remember, tabs must implement PrefsTab.
        // ----------------------------------------------------------------
	String
	    GEN = Globals.lang("General"),
	    EXT = Globals.lang("External programs"),
	    APP = Globals.lang("Entry table"),
	    COL = Globals.lang("Entry table columns"),
	    KEY = Globals.lang("Key pattern"),
	    PRE = Globals.lang("Entry preview"),
	    ADV = Globals.lang("Advanced");

	ArrayList al = new ArrayList();
	al.add(GEN);
	al.add(EXT);
	al.add(APP);
	al.add(COL);
	al.add(KEY);
	al.add(PRE);
	
	main.setLayout(cardLayout);

        main.add(new GeneralTab(frame, _prefs), GEN);	
	if (!Globals.ON_MAC) {
	    al.add(ADV);
	    main.add(new AdvancedTab(_prefs, parent.helpDiag), ADV);
	}



	
        main.add(new ExternalTab(frame, _prefs), EXT);
        main.add(new TablePrefsTab(_prefs, parent), APP);
        main.add(new TableColumnsTab(_prefs, parent), COL);
	main.add(new TabLabelPattern(_prefs, parent.helpDiag), KEY);
        main.add(new PreviewPrefsTab(_prefs, parent.helpDiag), PRE);


	upper.setBorder(BorderFactory.createEtchedBorder());

	chooser = new JList(al.toArray());
	chooser.setBorder(BorderFactory.createEtchedBorder());
	// Set a prototype value to control the width of the list:
	chooser.setPrototypeCellValue("This should be wide enough");
	chooser.setSelectedIndex(0);
	chooser.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	// Add the selection listener that will show the correct panel when selection changes:
	chooser.addListSelectionListener(new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
		    if (e.getValueIsAdjusting())
			return;
		    String o = (String)chooser.getSelectedValue();
		    //Util.pr(o);
		    cardLayout.show(main, o);
		    //main.revalidate();
		    //main.repaint();
		}
	    });
	
	upper.setLayout(new BorderLayout());
	upper.add(chooser, BorderLayout.WEST);
	upper.add(main, BorderLayout.CENTER);
	    
	// Add all panels to main panel:
	//for (Iterator i=panels.entrySet().iterator(); i.hasNext();) {


	//}

        JButton
            ok = new JButton(Globals.lang("Ok")),
            cancel = new JButton(Globals.lang("Cancel"));
        ok.addActionListener(new OkAction());
        CancelAction cancelAction = new CancelAction();
        cancel.addActionListener(cancelAction);
        lower.add(ok);
        lower.add(cancel);

        // Key bindings:
        ActionMap am = chooser.getActionMap();
        InputMap im = chooser.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
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
			int count = main.getComponentCount();
			Component[] comps = main.getComponents();
			for (int i = 0; i < count; i++) {
			    if (!((PrefsTab)comps[i]).readyToClose())
				return; // If not, break off.
			}			
			// Then store settings and close:
			for (int i = 0; i < count; i++) {
			    ( (PrefsTab)comps[i]).storeSettings();
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
