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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.jabref.export.ExportFormats;
import net.sf.jabref.groups.GroupsPrefsTab;
import net.sf.jabref.gui.MainTable;
import net.sf.jabref.gui.FileDialogs;

import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 * Preferences dialog. Contains a TabbedPane, and tabs will be defined in
 * separate classes. Tabs MUST implement the PrefsTab interface, since this
 * dialog will call the storeSettings() method of all tabs when the user presses
 * ok.
 * 
 * With this design, it should be very easy to add new tabs later.
 * 
 */
public class PrefsDialog3 extends JDialog {

	JPanel main;

	JabRefFrame frame;

	public PrefsDialog3(JabRefFrame parent) {
		super(parent, Globals.lang("JabRef preferences"), false);
		final JabRefPreferences prefs = JabRefPreferences.getInstance();
		frame = parent;

		final JList chooser;

		JButton importPrefs = new JButton(Globals.lang("Import preferences"));
		JButton exportPrefs = new JButton(Globals.lang("Export preferences"));

		main = new JPanel();
		JPanel upper = new JPanel();
		JPanel lower = new JPanel();

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(upper, BorderLayout.CENTER);
		getContentPane().add(lower, BorderLayout.SOUTH);

		final CardLayout cardLayout = new CardLayout();
		main.setLayout(cardLayout);

		// ----------------------------------------------------------------
		// Add tabs to tabbed here. Remember, tabs must implement PrefsTab.
		// ----------------------------------------------------------------
		ArrayList<PrefsTab> tabs = new ArrayList<PrefsTab>();
		tabs.add(new GeneralTab(frame, prefs));
        tabs.add(new FileTab(frame, prefs));
        tabs.add(new EntryEditorPrefsTab(frame, prefs));
        tabs.add(new GroupsPrefsTab(prefs));
		tabs.add(new AppearancePrefsTab(prefs));
		tabs.add(new ExternalTab(frame, this, prefs, parent.helpDiag));
		tabs.add(new TablePrefsTab(prefs, parent));
		tabs.add(new TableColumnsTab(prefs, parent));
		tabs.add(new TabLabelPattern(prefs, parent.helpDiag));
		tabs.add(new PreviewPrefsTab(prefs));
		tabs.add(new NameFormatterTab(parent.helpDiag));
		tabs.add(new XmpPrefsTab());
        tabs.add(new AdvancedTab(prefs, parent.helpDiag));
		
		Iterator<PrefsTab> it = tabs.iterator();
		String[] names = new String[tabs.size()];
		int i = 0;
        //ArrayList<Component> comps = new ArrayList<Component>();
        while (it.hasNext()) {
			PrefsTab tab = it.next();
			names[i++] = tab.getTabName(); 
			main.add((Component) tab, tab.getTabName());
        }

		upper.setBorder(BorderFactory.createEtchedBorder());

		chooser = new JList(names);
		chooser.setBorder(BorderFactory.createEtchedBorder());
		// Set a prototype value to control the width of the list:
		chooser.setPrototypeCellValue("This should be wide enough");
		chooser.setSelectedIndex(0);
		chooser.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Add the selection listener that will show the correct panel when
		// selection changes:
		chooser.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;
				String o = (String) chooser.getSelectedValue();
				cardLayout.show(main, o);
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

		JButton ok = new JButton(Globals.lang("Ok")), cancel = new JButton(Globals.lang("Cancel"));
		ok.addActionListener(new OkAction());
		CancelAction cancelAction = new CancelAction();
		cancel.addActionListener(cancelAction);
		lower.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		ButtonBarBuilder bb = new ButtonBarBuilder(lower);
		bb.addGlue();
		bb.addGridded(ok);
		bb.addGridded(cancel);
		bb.addGlue();
		// lower.add(ok);
		// lower.add(cancel);

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
				String filename = FileDialogs.getNewFile(frame, new File(System
					.getProperty("user.home")), ".xml", JFileChooser.SAVE_DIALOG, false);
				if (filename == null)
					return;
				File file = new File(filename);
				if (!file.exists()
					|| (JOptionPane.showConfirmDialog(PrefsDialog3.this, "'" + file.getName()
						+ "' " + Globals.lang("exists. Overwrite file?"), Globals
						.lang("Export preferences"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)) {

					try {
						prefs.exportPreferences(filename);
					} catch (IOException ex) {
						JOptionPane.showMessageDialog(PrefsDialog3.this, Globals
							.lang("Could not export preferences")
							+ ": " + ex.getMessage(), Globals.lang("Export preferences"),
							JOptionPane.ERROR_MESSAGE);
						// ex.printStackTrace();
					}
				}

			}
		});

		importPrefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String filename = FileDialogs.getNewFile(frame, new File(System
					.getProperty("user.home")), ".xml", JFileChooser.OPEN_DIALOG, false);
				if (filename == null)
					return;

				try {
					prefs.importPreferences(filename);
					setValues();
					BibtexEntryType.loadCustomEntryTypes(prefs);
                    ExportFormats.initAllExports();
					frame.removeCachedEntryEditors();
                    Globals.prefs.updateEntryEditorTabList();
                } catch (IOException ex) {
					JOptionPane.showMessageDialog(PrefsDialog3.this, Globals
						.lang("Could not import preferences")
						+ ": " + ex.getMessage(), Globals.lang("Import preferences"),
						JOptionPane.ERROR_MESSAGE);
					// ex.printStackTrace();
				}
			}

		});

		setValues();

		pack(); // setSize(440, 500);

        /** Look through component sizes to find which tab is to blame
         *  when the dialog grows too large:
        for (Component co : comps) {
            System.out.println(co.getPreferredSize());
        }*/
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
						if (!((PrefsTab) comps[i]).readyToClose()) {
							ready = false;
							return; // If not, break off.
						}
					}
					// Then store settings and close:
					for (int i = 0; i < count; i++) {
						((PrefsTab) comps[i]).storeSettings();
					}
					Globals.prefs.flush();
				}

				public void update() {
					if (!ready)
						return;
					setVisible(false);
					MainTable.updateRenderers();
					frame.setupAllTables();
					frame.groupSelector.revalidateGroups(); // icons may have
					// changed
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
			((PrefsTab) comps[i]).setValues();
		}
	}

	class CancelAction extends AbstractAction {
		public CancelAction() {
			super("Cancel");
		}

		public void actionPerformed(ActionEvent e) {
			setVisible(false);
		}
	}

}
