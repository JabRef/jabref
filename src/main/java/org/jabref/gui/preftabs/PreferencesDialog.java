package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.BackingStoreException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.jabref.Globals;
import org.jabref.JabRefException;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.keyboard.KeyBinder;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.FileType;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.JabRefPreferencesFilter;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Preferences dialog. Contains a TabbedPane, and tabs will be defined in
 * separate classes. Tabs MUST implement the PrefsTab interface, since this
 * dialog will call the storeSettings() method of all tabs when the user presses
 * ok.
 *
 * With this design, it should be very easy to add new tabs later.
 *
 */
public class PreferencesDialog extends JabRefDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesDialog.class);

    private final JPanel main;

    private final JabRefFrame frame;
    private final JButton importPreferences = new JButton(Localization.lang("Import preferences"));
    private final JButton exportPreferences = new JButton(Localization.lang("Export preferences"));
    private final JButton showPreferences = new JButton(Localization.lang("Show preferences"));

    private final JButton resetPreferences = new JButton(Localization.lang("Reset preferences"));

    public PreferencesDialog(JabRefFrame parent) {
        super(parent, Localization.lang("JabRef preferences"), false, PreferencesDialog.class);
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
        tabs.add(new FileTab(frame, prefs));
        tabs.add(new TablePrefsTab(prefs));
        tabs.add(new TableColumnsTab(prefs, parent));
        tabs.add(new PreviewPrefsTab());
        tabs.add(new ExternalTab(frame, this, prefs));
        tabs.add(new GroupsPrefsTab(prefs));
        tabs.add(new EntryEditorPrefsTab(prefs));
        tabs.add(new BibtexKeyPatternPrefTab(prefs, parent.getCurrentBasePanel()));
        tabs.add(new ImportSettingsTab(prefs));
        tabs.add(new ExportSortingPrefsTab(prefs));
        tabs.add(new NameFormatterTab(prefs));
        tabs.add(new XmpPrefsTab(prefs));
        tabs.add(new NetworkTab(prefs));
        tabs.add(new AdvancedTab(prefs));
        tabs.add(new AppearancePrefsTab(prefs));

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
        mainPanel.add(putPanelInScrollPane(main), BorderLayout.CENTER);
        mainPanel.add(putPanelInScrollPane(westPanel), BorderLayout.WEST);

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

            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .addExtensionFilter(FileType.XML)
                    .withDefaultExtension(FileType.XML)
                    .withInitialDirectory(getPrefsExportPath()).build();
            DialogService ds = new FXDialogService();

            Optional<Path> fileName = DefaultTaskExecutor
                    .runInJavaFXThread(() -> ds.showFileOpenDialog(fileDialogConfiguration));

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

    private JScrollPane putPanelInScrollPane(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        return scrollPane;
    }

    private String getPrefsExportPath() {
        return Globals.prefs.get(JabRefPreferences.PREFS_EXPORT_PATH);
    }

    private void updateAfterPreferenceChanges() {
        setValues();

        Globals.exportFactory = Globals.prefs.getExporterFactory(Globals.journalAbbreviationLoader);

        Globals.prefs.updateEntryEditorTabList();
    }

    private void storeAllSettings() {
        // First check that all tabs are ready to close:
        Component[] preferenceTabs = main.getComponents();
        for (Component tab : preferenceTabs) {
            if (!((PrefsTab) tab).validateSettings()) {
                return; // If not, break off.
            }
        }
        // Then store settings and close:
        for (Component tab : preferenceTabs) {
            ((PrefsTab) tab).storeSettings();
        }
        Globals.prefs.flush();

        setVisible(false);
        MainTable.updateRenderers();
        GUIGlobals.updateEntryEditorColors();
        frame.setupAllTables();
        frame.output(Localization.lang("Preferences recorded."));
    }

    public void setValues() {
        // Update all field values in the tabs:
        int count = main.getComponentCount();
        Component[] comps = main.getComponents();
        for (int i = 0; i < count; i++) {
            ((PrefsTab) comps[i]).setValues();
        }
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

            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .addExtensionFilter(FileType.XML)
                    .withDefaultExtension(FileType.XML)
                    .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
            DialogService ds = new FXDialogService();
            Optional<Path> path = DefaultTaskExecutor
                    .runInJavaFXThread(() -> ds.showFileSaveDialog(fileDialogConfiguration));

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
