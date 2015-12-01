/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.jabref.*;
import net.sf.jabref.exporter.ExportFormats;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.MainTable;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Preferences dialog. Contains a TabbedPane, and tabs will be defined in
 * separate classes. Tabs MUST implement the PrefsTab interface, since this
 * dialog will call the storeSettings() method of all tabs when the user presses
 * ok.
 *
 * With this design, it should be very easy to add new tabs later.
 *
 */
public class PreferencesDialog extends JDialog {

    private final JPanel main;

    private final JabRefFrame frame;

    private static final Log LOGGER = LogFactory.getLog(PreferencesDialog.class);

    public PreferencesDialog(JabRefFrame parent, JabRef jabRef) {
        super(parent, Localization.lang("JabRef preferences"), false);
        final JabRefPreferences prefs = JabRefPreferences.getInstance();
        frame = parent;

        final JList<String> chooser;

        JButton importPrefs = new JButton(Localization.lang("Import preferences"));
        JButton exportPrefs = new JButton(Localization.lang("Export preferences"));

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
        List<PrefsTab> tabs = new ArrayList<>();
        tabs.add(new GeneralTab(frame, prefs));
        tabs.add(new NetworkTab(prefs));
        tabs.add(new FileTab(frame, prefs));
        tabs.add(new FileSortTab(prefs));
        tabs.add(new EntryEditorPrefsTab(frame, prefs));
        tabs.add(new GroupsPrefsTab(prefs));
        tabs.add(new AppearancePrefsTab(prefs));
        tabs.add(new ExternalTab(frame, this, prefs, parent.helpDiag));
        tabs.add(new TablePrefsTab(prefs));
        tabs.add(new TableColumnsTab(prefs, parent));
        tabs.add(new LabelPatternPrefTab(prefs, parent.helpDiag));
        tabs.add(new PreviewPrefsTab(prefs));
        tabs.add(new NameFormatterTab(parent.helpDiag));
        tabs.add(new ImportSettingsTab());
        tabs.add(new XmpPrefsTab());
        tabs.add(new AdvancedTab(prefs, parent.helpDiag, jabRef));

        Iterator<PrefsTab> prefTabs = tabs.iterator();
        String[] names = new String[tabs.size()];
        int index = 0;

        while (prefTabs.hasNext()) {
            PrefsTab tab = prefTabs.next();
            names[index] = tab.getTabName();
            index++;
            main.add((Component) tab, tab.getTabName());
        }

        upper.setBorder(BorderFactory.createEtchedBorder());

        chooser = new JList<>(names);
        chooser.setBorder(BorderFactory.createEtchedBorder());
        // Set a prototype value to control the width of the list:
        chooser.setPrototypeCellValue("This should be wide enough");
        chooser.setSelectedIndex(0);
        chooser.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add the selection listener that will show the correct panel when
        // selection changes:
        chooser.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                String o = chooser.getSelectedValue();
                cardLayout.show(main, o);
            }
        });

        JPanel one = new JPanel();
        JPanel two = new JPanel();
        one.setLayout(new BorderLayout());
        two.setLayout(new BorderLayout());
        one.add(chooser, BorderLayout.CENTER);
        one.add(importPrefs, BorderLayout.SOUTH);
        two.add(one, BorderLayout.CENTER);
        two.add(exportPrefs, BorderLayout.SOUTH);
        upper.setLayout(new BorderLayout());
        upper.add(two, BorderLayout.WEST);
        upper.add(main, BorderLayout.CENTER);

        JButton ok = new JButton(Localization.lang("Ok"));
        JButton cancel = new JButton(Localization.lang("Cancel"));
        ok.addActionListener(new OkAction());
        CancelAction cancelAction = new CancelAction();
        cancel.addActionListener(cancelAction);
        lower.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        ButtonBarBuilder buttonBarBuilder = new ButtonBarBuilder(lower);
        buttonBarBuilder.addGlue();
        buttonBarBuilder.addButton(ok);
        buttonBarBuilder.addButton(cancel);
        buttonBarBuilder.addGlue();

        // Key bindings:
        Util.bindCloseDialogKeyToCancelAction(this.getRootPane(), cancelAction);

        // Import and export actions:
        exportPrefs.setToolTipText(Localization.lang("Export preferences to file"));
        importPrefs.setToolTipText(Localization.lang("Import preferences from file"));
        exportPrefs.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String filename = FileDialogs.getNewFile(frame, new File(System
                        .getProperty("user.home")), ".xml", JFileChooser.SAVE_DIALOG, false);
                if (filename == null) {
                    return;
                }
                File file = new File(filename);
                if (!file.exists() || (JOptionPane.showConfirmDialog(PreferencesDialog.this,
                        Localization.lang("'%0' exists. Overwrite file?", file.getName()),
                        Localization.lang("Export preferences"),
                        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)) {

                    try {
                        prefs.exportPreferences(filename);
                    } catch (JabRefException ex) {
                        LOGGER.warn(ex.getMessage(), ex);
                        JOptionPane.showMessageDialog(PreferencesDialog.this, ex.getLocalizedMessage(),
                                Localization.lang("Export preferences"), JOptionPane.ERROR_MESSAGE);
                    }
                }

            }
        });

        importPrefs.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String filename = FileDialogs.getNewFile(frame, new File(System
                        .getProperty("user.home")), ".xml", JFileChooser.OPEN_DIALOG, false);
                if (filename == null) {
                    return;
                }

                try {
                    prefs.importPreferences(filename);
                    setValues();
                    CustomEntryTypesManager.loadCustomEntryTypes(prefs);
                    ExportFormats.initAllExports();
                    frame.removeCachedEntryEditors();
                    Globals.prefs.updateEntryEditorTabList();
                } catch (JabRefException ex) {
                    LOGGER.warn(ex.getMessage(), ex);
                    JOptionPane.showMessageDialog(PreferencesDialog.this, ex.getLocalizedMessage(),
                            Localization.lang("Import preferences"), JOptionPane.ERROR_MESSAGE);
                }
            }

        });

        setValues();

        pack();

    }


    class OkAction extends AbstractAction {

        public OkAction() {
            super("Ok");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            AbstractWorker worker = new AbstractWorker() {

                boolean ready = true;


                @Override
                public void run() {
                    // First check that all tabs are ready to close:
                    int count = main.getComponentCount();
                    Component[] comps = main.getComponents();
                    for (int i = 0; i < count; i++) {
                        if (!((PrefsTab) comps[i]).validateSettings()) {
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

                @Override
                public void update() {
                    if (!ready) {
                        return;
                    }
                    setVisible(false);
                    MainTable.updateRenderers();
                    GUIGlobals.updateEntryEditorColors();
                    frame.setupAllTables();
                    frame.groupSelector.revalidateGroups(); // icons may have
                    // changed
                    frame.output(Localization.lang("Preferences recorded."));
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

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }

}
