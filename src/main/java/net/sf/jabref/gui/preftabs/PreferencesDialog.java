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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefException;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.JabRefPreferencesFilter;
import net.sf.jabref.JabRefPreferencesFilterDialog;
import net.sf.jabref.exporter.ExportFormats;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.keyboard.KeyBinder;
import net.sf.jabref.gui.maintable.MainTable;
import net.sf.jabref.logic.l10n.Localization;

import com.jgoodies.forms.builder.ButtonBarBuilder;
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

    private final JButton importPreferences = new JButton(Localization.lang("Import preferences"));
    private final JButton exportPreferences = new JButton(Localization.lang("Export preferences"));
    private final JButton showPreferences = new JButton(Localization.lang("Show preferences"));
    private final JButton resetPreferences = new JButton(Localization.lang("Reset preferences"));

    private static final Log LOGGER = LogFactory.getLog(PreferencesDialog.class);


    public PreferencesDialog(JabRefFrame parent) {
        super(parent, Localization.lang("JabRef preferences"), false);
        JabRefPreferences prefs = JabRefPreferences.getInstance();
        frame = parent;

        main = new JPanel();
        JPanel mainPanel = new JPanel();
        JPanel lower = new JPanel();

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(lower, BorderLayout.SOUTH);

        final CardLayout cardLayout = new CardLayout();
        main.setLayout(cardLayout);

        List<PrefsTab> tabs = new ArrayList<>();
        tabs.add(new GeneralTab(prefs));
        tabs.add(new NetworkTab(prefs));
        tabs.add(new FileTab(frame, prefs));
        tabs.add(new FileSortTab(prefs));
        tabs.add(new EntryEditorPrefsTab(frame, prefs));
        tabs.add(new GroupsPrefsTab(prefs));
        tabs.add(new AppearancePrefsTab(prefs));
        tabs.add(new ExternalTab(frame, this, prefs));
        tabs.add(new TablePrefsTab(prefs));
        tabs.add(new TableColumnsTab(prefs, parent));
        tabs.add(new LabelPatternPrefTab(prefs, parent.getCurrentBasePanel()));
        tabs.add(new PreviewPrefsTab(prefs));
        tabs.add(new NameFormatterTab(prefs));
        tabs.add(new ImportSettingsTab(prefs));
        tabs.add(new XmpPrefsTab(prefs));
        tabs.add(new AdvancedTab(prefs));

        // add all tabs
        tabs.forEach(tab -> main.add((Component) tab, tab.getTabName()));

        mainPanel.setBorder(BorderFactory.createEtchedBorder());

        String[] tabNames = tabs.stream().map(PrefsTab::getTabName).toArray(String[]::new);
        JList<String> chooser = new JList<>(tabNames);
        chooser.setBorder(BorderFactory.createEtchedBorder());
        // Set a prototype value to control the width of the list:
        chooser.setPrototypeCellValue("This should be wide enough");
        chooser.setSelectedIndex(0);
        chooser.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add the selection listener that will show the correct panel when
        // selection changes:
        chooser.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            String o = chooser.getSelectedValue();
            cardLayout.show(main, o);
        });


        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(4, 1));
        buttons.add(importPreferences, 0);
        buttons.add(exportPreferences, 1);
        buttons.add(showPreferences, 2);
        buttons.add(resetPreferences, 3);

        JPanel westPanel = new JPanel();
        westPanel.setLayout(new BorderLayout());
        westPanel.add(chooser, BorderLayout.CENTER);
        westPanel.add(buttons, BorderLayout.SOUTH);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(main, BorderLayout.CENTER);
        mainPanel.add(westPanel, BorderLayout.WEST);

        JButton ok = new JButton(Localization.lang("OK"));
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
        KeyBinder.bindCloseDialogKeyToCancelAction(this.getRootPane(), cancelAction);

        // Import and export actions:
        exportPreferences.setToolTipText(Localization.lang("Export preferences to file"));
        exportPreferences.addActionListener(e -> {
            String filename = FileDialogs.getNewFile(frame, new File(System.getProperty("user.home")), ".xml",
                    JFileChooser.SAVE_DIALOG, false);
            if (filename == null) {
                return;
            }
            File file = new File(filename);
            if (!file.exists() || (JOptionPane.showConfirmDialog(PreferencesDialog.this,
                    Localization.lang("'%0' exists. Overwrite file?", file.getName()),
                    Localization.lang("Export preferences"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)) {

                try {
                    prefs.exportPreferences(filename);
                } catch (JabRefException ex) {
                    LOGGER.warn(ex.getMessage(), ex);
                    JOptionPane.showMessageDialog(PreferencesDialog.this, ex.getLocalizedMessage(),
                            Localization.lang("Export preferences"), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        importPreferences.setToolTipText(Localization.lang("Import preferences from file"));
        importPreferences.addActionListener(e -> {
            String filename = FileDialogs.getNewFile(frame, new File(System.getProperty("user.home")), ".xml",
                    JFileChooser.OPEN_DIALOG, false);
            if (filename != null) {
                try {
                    prefs.importPreferences(filename);
                    updateAfterPreferenceChanges();
                    JOptionPane.showMessageDialog(PreferencesDialog.this,
                            Localization.lang("You must restart JabRef for this to come into effect."),
                            Localization.lang("Import preferences"),
                            JOptionPane.WARNING_MESSAGE);
                } catch (JabRefException ex) {
                    LOGGER.warn(ex.getMessage(), ex);
                    JOptionPane.showMessageDialog(PreferencesDialog.this, ex.getLocalizedMessage(),
                            Localization.lang("Import preferences"), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        showPreferences.addActionListener(
                e -> new JabRefPreferencesFilterDialog(new JabRefPreferencesFilter(Globals.prefs), frame)
                        .setVisible(true));
        resetPreferences.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(PreferencesDialog.this,
                    Localization.lang("Are you sure you want to reset all settings to default values?"),
                    Localization.lang("Reset preferences"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                try {
                    prefs.clear();
                    JOptionPane.showMessageDialog(PreferencesDialog.this,
                            Localization.lang("You must restart JabRef for this to come into effect."),
                            Localization.lang("Reset preferences"),
                            JOptionPane.WARNING_MESSAGE);
                } catch (BackingStoreException ex) {
                    LOGGER.warn(ex.getMessage(), ex);
                    JOptionPane.showMessageDialog(PreferencesDialog.this, ex.getLocalizedMessage(),
                            Localization.lang("Reset preferences"), JOptionPane.ERROR_MESSAGE);
                }
                updateAfterPreferenceChanges();
            }
        });

        setValues();

        pack();

    }

    private void updateAfterPreferenceChanges() {
        setValues();
        ExportFormats.initAllExports();
        frame.removeCachedEntryEditors();
        Globals.prefs.updateEntryEditorTabList();
    }

    class OkAction extends AbstractAction {

        public OkAction() {
            super("OK");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            // First check that all tabs are ready to close:
            int count = main.getComponentCount();
            Component[] comps = main.getComponents();
            for (int i = 0; i < count; i++) {
                if (!((PrefsTab) comps[i]).validateSettings()) {
                    return; // If not, break off.
                }
            }
            // Then store settings and close:
            for (int i = 0; i < count; i++) {
                ((PrefsTab) comps[i]).storeSettings();
            }
            Globals.prefs.flush();

            setVisible(false);
            MainTable.updateRenderers();
            GUIGlobals.updateEntryEditorColors();
            frame.setupAllTables();
            frame.getGroupSelector().revalidateGroups(); // icons may have changed
            frame.output(Localization.lang("Preferences recorded."));
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
