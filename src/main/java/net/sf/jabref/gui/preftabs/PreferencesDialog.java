package net.sf.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.BackingStoreException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefException;
import net.sf.jabref.gui.FileDialog;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.keyboard.KeyBinder;
import net.sf.jabref.gui.maintable.MainTable;
import net.sf.jabref.logic.exporter.ExportFormat;
import net.sf.jabref.logic.exporter.ExportFormats;
import net.sf.jabref.logic.exporter.SavePreferences;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.preferences.JabRefPreferences;
import net.sf.jabref.preferences.JabRefPreferencesFilter;
import net.sf.jabref.shared.prefs.SharedDatabasePreferences;

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
    private static final Log LOGGER = LogFactory.getLog(PreferencesDialog.class);

    private final JPanel main;

    private final JabRefFrame frame;
    private final JButton importPreferences = new JButton(Localization.lang("Import preferences"));
    private final JButton exportPreferences = new JButton(Localization.lang("Export preferences"));
    private final JButton showPreferences = new JButton(Localization.lang("Show preferences"));

    private final JButton resetPreferences = new JButton(Localization.lang("Reset preferences"));


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
        tabs.add(new EntryEditorPrefsTab(prefs));
        tabs.add(new GroupsPrefsTab(prefs));
        tabs.add(new AppearancePrefsTab(prefs));
        tabs.add(new ExternalTab(frame, this, prefs));
        tabs.add(new TablePrefsTab(prefs));
        tabs.add(new TableColumnsTab(prefs, parent));
        tabs.add(new BibtexKeyPatternPrefTab(prefs, parent.getCurrentBasePanel()));
        tabs.add(new PreviewPrefsTab());
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
        exportPreferences.addActionListener(new ExportAction());

        importPreferences.setToolTipText(Localization.lang("Import preferences from file"));
        importPreferences.addActionListener(e -> {
            FileDialog dialog = new FileDialog(frame, getPrefsExportPath()).withExtension(FileExtensions.XML);
            dialog.setDefaultExtension(FileExtensions.XML);
            Optional<Path> fileName = dialog.showDialogAndGetSelectedFile();

            if (fileName.isPresent()) {
                try {
                    prefs.importPreferences(fileName.get().toString());
                    updateAfterPreferenceChanges();
                    JOptionPane.showMessageDialog(PreferencesDialog.this,
                            Localization.lang("You must restart JabRef for this to come into effect."),
                            Localization.lang("Import preferences"), JOptionPane.WARNING_MESSAGE);
                    this.dispose();
                } catch (JabRefException ex) {
                    LOGGER.warn(ex.getMessage(), ex);
                    JOptionPane.showMessageDialog(PreferencesDialog.this, ex.getLocalizedMessage(),
                            Localization.lang("Import preferences"), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        showPreferences.addActionListener(
                e -> new PreferencesFilterDialog(new JabRefPreferencesFilter(prefs), frame).setVisible(true));
        resetPreferences.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(PreferencesDialog.this,
                    Localization.lang("Are you sure you want to reset all settings to default values?"),
                    Localization.lang("Reset preferences"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                try {
                    prefs.clear();
                    new SharedDatabasePreferences().clear();
                    JOptionPane.showMessageDialog(PreferencesDialog.this,
                            Localization.lang("You must restart JabRef for this to come into effect."),
                            Localization.lang("Reset preferences"), JOptionPane.WARNING_MESSAGE);
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

    private String getPrefsExportPath() {
        return Globals.prefs.get(JabRefPreferences.PREFS_EXPORT_PATH);
    }

    private void updateAfterPreferenceChanges() {
        setValues();
        Map<String, ExportFormat> customFormats = Globals.prefs.customExports.getCustomExportFormats(Globals.prefs,
                Globals.journalAbbreviationLoader);
        LayoutFormatterPreferences layoutPreferences = Globals.prefs
                .getLayoutFormatterPreferences(Globals.journalAbbreviationLoader);
        SavePreferences savePreferences = SavePreferences.loadForExportFromPreferences(Globals.prefs);
        ExportFormats.initAllExports(customFormats, layoutPreferences, savePreferences);

        Globals.prefs.updateEntryEditorTabList();
    }

    private void storeAllSettings(){
        // First check that all tabs are ready to close:
        Component[] preferenceTabs = main.getComponents();
        for (Component tab: preferenceTabs) {
            if (!((PrefsTab) tab).validateSettings()) {
                return; // If not, break off.
            }
        }
        // Then store settings and close:
        for (Component tab: preferenceTabs) {
            ((PrefsTab) tab).storeSettings();
        }
        Globals.prefs.flush();

        setVisible(false);
        MainTable.updateRenderers();
        GUIGlobals.updateEntryEditorColors();
        frame.setupAllTables();
        frame.getGroupSelector().revalidateGroups(); // icons may have changed
        frame.output(Localization.lang("Preferences recorded."));
    }


    class OkAction extends AbstractAction {

        public OkAction() {
            super("OK");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            storeAllSettings();
        }
    }

    class ExportAction extends AbstractAction {

        public ExportAction() {
            super("Export");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            FileDialog dialog = new FileDialog(frame).withExtension(FileExtensions.XML);
            dialog.setDefaultExtension(FileExtensions.XML);
            Optional<Path> path = dialog.saveNewFile();

            path.ifPresent(exportFile -> {
                try {
                    storeAllSettings();
                    Globals.prefs.exportPreferences(exportFile.toString());
                    Globals.prefs.put(JabRefPreferences.PREFS_EXPORT_PATH, exportFile.toString());
                } catch (JabRefException ex) {
                    LOGGER.warn(ex.getMessage(), ex);
                    JOptionPane.showMessageDialog(PreferencesDialog.this, ex.getLocalizedMessage(),
                            Localization.lang("Export preferences"), JOptionPane.WARNING_MESSAGE);
                }
            });
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
