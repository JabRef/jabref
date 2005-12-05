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
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.sf.jabref.groups.GroupsPrefsTab;
import net.sf.jabref.gui.MainTable;

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
    main = new JPanel();/* {
		public void add(Component c, Object o) {
		    super.add(c, o);
		    System.out.println(o+" "+c.getPreferredSize());
		    }
		    };*/
    JList chooser;
    JButton importPrefs = new JButton(Globals.lang("Import preferences")),
    exportPrefs = new JButton(Globals.lang("Export preferences"));
    CardLayout cardLayout = new CardLayout();
    HashMap panels = new HashMap();
    JabRefFrame frame;
    PrefsDialog3 ths = this;

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
        APP = Globals.lang("Appearance"),
        GRP = Globals.lang("Groups"), // JZTODO lyrics
        EXT = Globals.lang("External programs"),
        TAB = Globals.lang("Entry table"),
        COL = Globals.lang("Entry table columns"),
        KEY = Globals.lang("Key pattern"),
        PRE = Globals.lang("Entry preview"),
        //JOU = Globals.lang("Journal names"),
        ADV = Globals.lang("Advanced");

    ArrayList al = new ArrayList();
    al.add(GEN);
    al.add(APP);
    al.add(GRP);
    al.add(EXT);
    al.add(TAB);
    al.add(COL);
    al.add(KEY);
    //al.add(JOU);
    al.add(PRE);

    main.setLayout(cardLayout);

        main.add(new GeneralTab(frame, _prefs), GEN);
    if (!Globals.ON_MAC) {
        al.add(ADV);
        main.add(new AdvancedTab(_prefs, parent.helpDiag), ADV);
    }




        main.add(new GroupsPrefsTab(_prefs), GRP);
        main.add(new AppearancePrefsTab(_prefs), APP);
        main.add(new ExternalTab(frame, _prefs), EXT);
        main.add(new TablePrefsTab(_prefs, parent), TAB);
        main.add(new TableColumnsTab(_prefs, parent), COL);
        main.add(new TabLabelPattern(_prefs, parent.helpDiag), KEY);
        main.add(new PreviewPrefsTab(_prefs, parent.helpDiag), PRE);
        //main.add(new ManageJournalsPanel(frame), JOU);

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

    JPanel one = new JPanel(), two = new JPanel();
    one.setLayout(new BorderLayout());
    two.setLayout(new BorderLayout());
    one.add(chooser, BorderLayout.CENTER);
    one.add(importPrefs, BorderLayout.SOUTH);
    two.add(one, BorderLayout.CENTER);
    two.add(exportPrefs, BorderLayout.SOUTH);
    upper.setLayout(new BorderLayout());
    upper.add(two, BorderLayout.WEST);
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

    // Import and export actions:
    exportPrefs.setToolTipText(Globals.lang("Export preferences to file"));
    importPrefs.setToolTipText(Globals.lang("Import preferences from file"));
    exportPrefs.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            String filename = Globals.getNewFile
            (frame, _prefs, new File(System.getProperty("user.home")),
             ".xml", JFileChooser.SAVE_DIALOG, false);
            if (filename == null)
            return;
            File file = new File(filename);
            if (!file.exists() ||
            (JOptionPane.showConfirmDialog
                         (ths, "'"+file.getName()+"' "+Globals.lang("exists. Overwrite file?"),
                          Globals.lang("Export preferences"), JOptionPane.OK_CANCEL_OPTION)
                         == JOptionPane.OK_OPTION)) {

            try {
                _prefs.exportPreferences(filename);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog
                (ths, Globals.lang("Could not export preferences")+": "+ex.getMessage(), Globals.lang("Export preferences"), JOptionPane.ERROR_MESSAGE);
                //ex.printStackTrace();
            }
            }

        }
        });

    importPrefs.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            String filename = Globals.getNewFile
            (frame, _prefs, new File(System.getProperty("user.home")),
             ".xml", JFileChooser.OPEN_DIALOG, false);
            if (filename == null)
            return;

            try {
            _prefs.importPreferences(filename);
            setValues();
                BibtexEntryType.loadCustomEntryTypes(_prefs);
                frame.removeCachedEntryEditors();
            } catch (IOException ex) {
            JOptionPane.showMessageDialog
                (ths, Globals.lang("Could not import preferences")+": "+ex.getMessage(), Globals.lang("Import preferences"), JOptionPane.ERROR_MESSAGE);
            //ex.printStackTrace();
            }
        }


        });

    setValues();

        pack(); //setSize(440, 500);
    }

    class OkAction extends AbstractAction {
        public OkAction() {
            super("Ok");
        }
        public void actionPerformed(ActionEvent e) {

        AbstractWorker worker = new AbstractWorker() {
            boolean ready = true;
            public void run() {
            // First check that all tabs are ready to close:
            int count = main.getComponentCount();
            Component[] comps = main.getComponents();
            for (int i = 0; i < count; i++) {
                if (!((PrefsTab)comps[i]).readyToClose()) {
                ready = false;
                return; // If not, break off.
                }
            }
            // Then store settings and close:
            for (int i = 0; i < count; i++) {
                ( (PrefsTab)comps[i]).storeSettings();
            }
            Globals.prefs.flush();
            //try { Thread.sleep(3000); } catch (InterruptedException ex) {}
            }
            public void update() {
            if (!ready)
                return;
            setVisible(false);
            MainTable.updateRenderers();
            frame.setupAllTables();
            frame.groupSelector.revalidateGroups(); // icons may have changed
            frame.output(Globals.lang("Preferences recorded."));
            }
        };
        worker.getWorker().run();
        worker.getCallBack().update();

        }
    }

    public void setValues() {
    // Update all field values in the tabs:
    int count = main.getComponentCount();
    Component[] comps = main.getComponents();
    for (int i = 0; i < count; i++) {
        ((PrefsTab)comps[i]).setValues();
    }
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            super("Cancel");

        }
        public void actionPerformed(ActionEvent e) {
        setVisible(false);
            // Just close dialog without recording changes.
            /*(new Thread() {
              public void run() {

              }
	      }).start();*/
        }
    }

}
