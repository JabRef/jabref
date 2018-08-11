package org.jabref.gui.preftabs;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.BackingStoreException;

import javax.swing.AbstractAction;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.effect.Effect;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

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

    private final BorderPane main;

    private final DialogService dialogService;
    private final JabRefFrame frame;
    private final JabRefPreferences prefs;
    private final AdvancedTab advancedTab;
    private final AppearancePrefsTab appearancePrefsTab;
    private final BibtexKeyPatternPrefTab bibtexKeyPatternPrefTab;
    private final EntryEditorPrefsTab entryEditorPrefsTab;
    private final ExportSortingPrefsTab exportSortingPrefsTab;
    private final ExternalTab externalTab;
    private final FileTab fileTab;
    private final GeneralTab generalTab;
    private final GroupsPrefsTab groupsPrefsTab;
    private final ImportSettingsTab importSettingsTab;
    private final NameFormatterTab nameFormatterTab;
    private final NetworkTab networkTab;
    private final PreviewPrefsTab previewPrefsTab;
    private final TableColumnsTab tableColumnsTab;
    private final TablePrefsTab tablePrefsTab;
    private final XmpPrefsTab xmpPrefsTab;
    private ArrayList<PrefsTab> arrayList = new ArrayList<>();

    public PreferencesDialog(JabRefFrame parent) {
        setTitle(Localization.lang("JabRef preferences"));
        getDialogPane().setPrefSize(1250, 800);
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
        advancedTab = new AdvancedTab(dialogService,prefs);
        appearancePrefsTab = new AppearancePrefsTab(dialogService,prefs);
        bibtexKeyPatternPrefTab = new BibtexKeyPatternPrefTab(prefs,frame.getCurrentBasePanel());
        entryEditorPrefsTab = new EntryEditorPrefsTab(prefs);
        exportSortingPrefsTab = new ExportSortingPrefsTab(prefs);
        externalTab = new ExternalTab(frame,this,prefs);
        fileTab = new FileTab(dialogService,prefs);
        generalTab = new GeneralTab(dialogService,prefs);
        groupsPrefsTab = new GroupsPrefsTab(prefs);
        importSettingsTab = new ImportSettingsTab(prefs);
        nameFormatterTab = new NameFormatterTab(prefs);
        networkTab = new NetworkTab(dialogService,prefs);
        previewPrefsTab = new PreviewPrefsTab(dialogService);
        tableColumnsTab = new TableColumnsTab(prefs,frame);
        tablePrefsTab = new TablePrefsTab(prefs);
        xmpPrefsTab = new XmpPrefsTab(prefs);

        if (arrayList.isEmpty()) {
            arrayList.add(advancedTab);
            arrayList.add(appearancePrefsTab);
            arrayList.add(bibtexKeyPatternPrefTab);
            arrayList.add(entryEditorPrefsTab);
            arrayList.add(exportSortingPrefsTab);
            arrayList.add(externalTab);
            arrayList.add(fileTab);
            arrayList.add(generalTab);
            arrayList.add(groupsPrefsTab);
            arrayList.add(importSettingsTab);
            arrayList.add(nameFormatterTab);
            arrayList.add(networkTab);
            arrayList.add(previewPrefsTab);
            arrayList.add(tableColumnsTab);
            arrayList.add(tablePrefsTab);
            arrayList.add(xmpPrefsTab);
        }

        main = new BorderPane();
        main.setCenter(generalTab.getBuilder());
        getDialogPane().setContent(main);
        construct();
    }

    private void construct() {
        VBox vBox = new VBox();
        vBox.setPrefSize(150,800);
        Font font = new Font(9);
        Button []button = new Button[20];

        button[0] = new Button(Localization.lang("General"));
        button[0].setOnAction( e -> main.setCenter(generalTab.getBuilder()));
        button[1] = new Button(Localization.lang("File"));
        button[1].setOnAction( e-> main.setCenter(fileTab.getBuilder()));
        button[2] = new Button(Localization.lang("Entry table"));
        button[2].setOnAction(e -> main.setCenter(tablePrefsTab.getBuilder()));
        button[3] = new Button(Localization.lang("Entry table columns"));
        button[3].setOnAction( e -> main.setCenter(tableColumnsTab.getBuilder()));
        button[4] = new Button(Localization.lang("Entry preview"));
        button[4].setOnAction( e -> main.setCenter(previewPrefsTab.getGridPane()));
        button[5] = new Button(Localization.lang("External programs"));
        button[5].setOnAction(e -> main.setCenter(externalTab.getBuilder()));
        button[6] = new Button(Localization.lang("Groups"));
        button[6].setOnAction( e -> main.setCenter(groupsPrefsTab.getBuilder()));
        button[7] = new Button(Localization.lang("Entry editor"));
        button[7].setOnAction( e -> main.setCenter(entryEditorPrefsTab.getBuilder()));
        button[8] = new Button(Localization.lang("BibTeX key generator"));
        button[8].setOnAction( e -> main.setCenter(bibtexKeyPatternPrefTab.getBuilder()));
        button[9] = new Button(Localization.lang("Import"));
        button[9].setOnAction( e -> main.setCenter(importSettingsTab.getBuilder()));
        button[10] = new Button(Localization.lang("Export sorting"));
        button[10].setOnAction( e -> main.setCenter(exportSortingPrefsTab.getBuilder()));
        button[11] = new Button(Localization.lang("Name formatter"));
        button[11].setOnAction( e -> main.setCenter(nameFormatterTab.getBuilder()));
        button[12] = new Button(Localization.lang("XMP-metadata"));
        button[12].setOnAction( e -> main.setCenter(xmpPrefsTab.getBuilder()));
        button[13] = new Button(Localization.lang("Network"));
        button[13].setOnAction(e -> main.setCenter(networkTab.getBuilder()));
        button[14] = new Button(Localization.lang("Advanced"));
        button[14].setOnAction( e -> main.setCenter(advancedTab.getBuilder()));
        button[15] = new Button(Localization.lang("Appearance"));
        button[15].setOnAction( e -> main.setCenter(appearancePrefsTab.getContainer()));
        button[16] = new Button(Localization.lang("Import preferences"));
        button[17] = new Button(Localization.lang("Export preferences"));
        button[18] = new Button(Localization.lang("Show preferences"));
        button[19] = new Button(Localization.lang("Reset preferences"));
        for (int i = 0; i < button.length; i++) {
            button[i].setFont(font);
            button[i].setEffect(null);
            button[i].setAlignment(Pos.BASELINE_LEFT);
            button[i].setPrefSize(120,20);
        }

        for (int i = 0; i < 16; i++) {
            vBox.getChildren().add(button[i]);
        }
        for (int i = 0; i < 12; i++) {
            vBox.getChildren().add(new Label(""));
        }
        
        vBox.getChildren().addAll(button[16], button[17], button[18], button[19]);
        main.setLeft(vBox);

        // TODO: Key bindings:
        // KeyBinder.bindCloseDialogKeyToCancelAction(this.getRootPane(), cancelAction);
        // Import and export actions:
        button[17].setAccessibleText(Localization.lang("Export preferences to file"));
        button[17].setOnAction( e -> new ExportAction());

        button[16].setAccessibleText(Localization.lang("Import preferences from file"));
        button[16].setOnAction(e -> {

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

        button[18].setOnAction(
                e -> new PreferencesFilterDialog(new JabRefPreferencesFilter(prefs)).setVisible(true));
        button[19].setOnAction(e -> {

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
        for (PrefsTab tab : arrayList) {
            if (!tab.validateSettings()) {
                return; // If not, break off.
            }
        }
        // Then store settings and close:
        for (PrefsTab tab : arrayList) {
            tab.storeSettings();
        }
        prefs.flush();

        GUIGlobals.updateEntryEditorColors();
        frame.setupAllTables();
        frame.output(Localization.lang("Preferences recorded."));
    }

    public void setValues() {
        // Update all field values in the tabs:
        for (PrefsTab prefsTab : arrayList) {
            prefsTab.setValues();
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
