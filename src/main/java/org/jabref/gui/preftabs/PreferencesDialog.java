package org.jabref.gui.preftabs;

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
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import org.jabref.Globals;
import org.jabref.JabRefException;
import org.jabref.gui.DialogService;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.JabRefPreferencesFilter;

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
public class PreferencesDialog extends BaseDialog<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesDialog.class);

    private final JPanel main;

    private final DialogService dialogService;
    private final JabRefFrame frame;
    private final JabRefPreferences prefs;

    public PreferencesDialog(JabRefFrame parent) {
        setTitle(Localization.lang("JabRef preferences"));
        getDialogPane().setPrefSize(1000, 800);
        getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        setResizable(true);

        ButtonType save = new ButtonType(Localization.lang("Save"), ButtonData.OK_DONE);

        getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        ControlHelper.setAction(save, getDialogPane(), event -> {
            storeAllSettings();
            close();
        });

        prefs = JabRefPreferences.getInstance();
        frame = parent;
        dialogService = frame.getDialogService();

        main = new JPanel();

        ControlHelper.setSwingContent(getDialogPane(), constructSwingContent());
    }

    private JComponent constructSwingContent() {
        JPanel mainPanel = new JPanel();
        final CardLayout cardLayout = new CardLayout();
        main.setLayout(cardLayout);

        List<PrefsTab> tabs = new ArrayList<>();
        tabs.add(new GeneralTab(dialogService, prefs));
        tabs.add(new FileTab(dialogService, prefs));
        tabs.add(new TablePrefsTab(prefs));
        tabs.add(new TableColumnsTab(prefs, frame));
        tabs.add(new PreviewPrefsTab(dialogService));
        tabs.add(new ExternalTab(frame, this, prefs));
        tabs.add(new GroupsPrefsTab(prefs));
        tabs.add(new EntryEditorPrefsTab(prefs));
        tabs.add(new BibtexKeyPatternPrefTab(prefs, frame.getCurrentBasePanel()));
        tabs.add(new ImportSettingsTab(prefs));
        tabs.add(new ExportSortingPrefsTab(prefs));
        tabs.add(new NameFormatterTab(prefs));
        tabs.add(new XmpPrefsTab(prefs));
        tabs.add(new NetworkTab(dialogService, prefs));
        tabs.add(new AdvancedTab(dialogService, prefs));
        tabs.add(new AppearancePrefsTab(dialogService, prefs));

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
        JButton importPreferences = new JButton(Localization.lang("Import preferences"));
        buttons.add(importPreferences, 0);
        JButton exportPreferences = new JButton(Localization.lang("Export preferences"));
        buttons.add(exportPreferences, 1);
        JButton showPreferences = new JButton(Localization.lang("Show preferences"));
        buttons.add(showPreferences, 2);
        JButton resetPreferences = new JButton(Localization.lang("Reset preferences"));
        buttons.add(resetPreferences, 3);

        JPanel westPanel = new JPanel();
        westPanel.setLayout(new BorderLayout());
        westPanel.add(chooser, BorderLayout.CENTER);
        westPanel.add(buttons, BorderLayout.SOUTH);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(putPanelInScrollPane(main), BorderLayout.CENTER);
        mainPanel.add(putPanelInScrollPane(westPanel), BorderLayout.WEST);

        // TODO: Key bindings:
        // KeyBinder.bindCloseDialogKeyToCancelAction(this.getRootPane(), cancelAction);

        // Import and export actions:
        exportPreferences.setToolTipText(Localization.lang("Export preferences to file"));
        exportPreferences.addActionListener(new ExportAction());

        importPreferences.setToolTipText(Localization.lang("Import preferences from file"));
        importPreferences.addActionListener(e -> {

            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .addExtensionFilter(StandardFileType.XML)
                    .withDefaultExtension(StandardFileType.XML)
                    .withInitialDirectory(getPrefsExportPath()).build();

            Optional<Path> fileName = DefaultTaskExecutor
                                                         .runInJavaFXThread(() -> dialogService.showFileOpenDialog(fileDialogConfiguration));

            if (fileName.isPresent()) {
                try {
                    prefs.importPreferences(fileName.get().toString());
                    updateAfterPreferenceChanges();

                    DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showWarningDialogAndWait(Localization.lang("Import preferences"),
                                                                                                       Localization.lang("You must restart JabRef for this to come into effect.")));
                } catch (JabRefException ex) {
                    LOGGER.warn(ex.getMessage(), ex);
                    DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showErrorDialogAndWait(Localization.lang("Import preferences"), ex));
                }
            }
        });

        showPreferences.addActionListener(
                e -> new PreferencesFilterDialog(new JabRefPreferencesFilter(prefs)).setVisible(true));
        resetPreferences.addActionListener(e -> {

            boolean resetPreferencesClicked = DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showConfirmationDialogAndWait(Localization.lang("Reset preferences"),
                                                                                                                                      Localization.lang("Are you sure you want to reset all settings to default values?"),
                                                                                                                                      Localization.lang("Reset preferences"), Localization.lang("Cancel")));

            if (resetPreferencesClicked) {
                try {
                    prefs.clear();
                    new SharedDatabasePreferences().clear();

                    DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showWarningDialogAndWait(Localization.lang("Reset preferences"),
                                                                                                       Localization.lang("You must restart JabRef for this to come into effect.")));
                } catch (BackingStoreException ex) {
                    LOGGER.warn(ex.getMessage(), ex);
                    DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showErrorDialogAndWait(Localization.lang("Reset preferences"), ex));
                }
                updateAfterPreferenceChanges();
            }
        });

        setValues();

        return mainPanel;
    }

    private JScrollPane putPanelInScrollPane(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        return scrollPane;
    }

    private String getPrefsExportPath() {
        return prefs.get(JabRefPreferences.PREFS_EXPORT_PATH);
    }

    private void updateAfterPreferenceChanges() {
        setValues();

        Map<String, TemplateExporter> customExporters = prefs.customExports.getCustomExportFormats(prefs, Globals.journalAbbreviationLoader);
        LayoutFormatterPreferences layoutPreferences = prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader);
        SavePreferences savePreferences = prefs.loadForExportFromPreferences();
        XmpPreferences xmpPreferences = prefs.getXMPPreferences();
        Globals.exportFactory = ExporterFactory.create(customExporters, layoutPreferences, savePreferences, xmpPreferences);
        prefs.updateEntryEditorTabList();
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
        prefs.flush();

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

    class ExportAction extends AbstractAction {

        public ExportAction() {
            super("Export");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .addExtensionFilter(StandardFileType.XML)
                    .withDefaultExtension(StandardFileType.XML)
                    .withInitialDirectory(prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
            Optional<Path> path = DefaultTaskExecutor
                    .runInJavaFXThread(() -> dialogService.showFileSaveDialog(fileDialogConfiguration));

            path.ifPresent(exportFile -> {
                try {
                    storeAllSettings();
                    prefs.exportPreferences(exportFile.toString());
                    prefs.put(JabRefPreferences.PREFS_EXPORT_PATH, exportFile.toString());
                } catch (JabRefException ex) {
                    LOGGER.warn(ex.getMessage(), ex);
                    dialogService.showErrorDialogAndWait(Localization.lang("Export preferences"), ex);

                }
            });
        }
    }
}
