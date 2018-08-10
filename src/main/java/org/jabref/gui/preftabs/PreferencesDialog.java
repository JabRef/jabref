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
        getDialogPane().setPrefSize(1200, 800);
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
        constructSwingContent();
    }

    private void constructSwingContent() {
        BorderPane mainPanel = new BorderPane();

        VBox vBox = new VBox();
        Font font = new Font(10);

        Button general = new Button(Localization.lang("General"));
        general.setFont(font);
        general.setAlignment(Pos.BASELINE_LEFT);
        general.setOnAction( e -> main.setCenter(generalTab.getBuilder()));
        general.setPrefSize(120,20);

        Button file = new Button(Localization.lang("File"));
        file.setFont(font);
        file.setAlignment(Pos.BASELINE_LEFT);
        file.setPrefSize(120,20);
        file.setOnAction( e-> main.setCenter(fileTab.getBuilder()));

        Button entryTable = new Button(Localization.lang("Entry table"));
        entryTable.setFont(font);
        entryTable.setAlignment(Pos.BASELINE_LEFT);
        entryTable.setPrefSize(120,20);
        entryTable.setOnAction(e -> main.setCenter(tablePrefsTab.getBuilder()));

        Button tableColumn = new Button(Localization.lang("Entry table columns"));
        tableColumn.setFont(font);
        tableColumn.setAlignment(Pos.BASELINE_LEFT);
        tableColumn.setPrefSize(120,20);
        tableColumn.setOnAction( e -> main.setCenter(tableColumnsTab.getBuilder()));

        Button entryPreview = new Button(Localization.lang("Entry preview"));
        entryPreview.setFont(font);
        entryPreview.setAlignment(Pos.BASELINE_LEFT);
        entryPreview.setPrefSize(120,20);
        entryPreview.setOnAction( e -> main.setCenter(previewPrefsTab.getGridPane()));

        Button externalPrograms = new Button(Localization.lang("External programs"));
        externalPrograms.setFont(font);
        externalPrograms.setAlignment(Pos.BASELINE_LEFT);
        externalPrograms.setPrefSize(120,20);
        externalPrograms.setOnAction(e -> main.setCenter(externalTab.getBuilder()));

        Button groups = new Button(Localization.lang("Groups"));
        groups.setFont(font);
        groups.setAlignment(Pos.BASELINE_LEFT);
        groups.setPrefSize(120,20);
        groups.setOnAction( e -> main.setCenter(groupsPrefsTab.getBuilder()));

        Button entryEditor = new Button(Localization.lang("Entry editor"));
        entryEditor.setFont(font);
        entryEditor.setAlignment(Pos.BASELINE_LEFT);
        entryEditor.setPrefSize(120,20);
        entryEditor.setOnAction( e -> main.setCenter(entryEditorPrefsTab.getBuilder()));

        Button bibkeyGenerator = new Button(Localization.lang("BibTeX key generator"));
        bibkeyGenerator.setPrefSize(120,20);
        bibkeyGenerator.setFont(font);
        bibkeyGenerator.setAlignment(Pos.BASELINE_LEFT);
        bibkeyGenerator.setOnAction( e -> main.setCenter(bibtexKeyPatternPrefTab.getBuilder()));

        Button imports = new Button(Localization.lang("Import"));
        imports.setFont(font);
        imports.setAlignment(Pos.BASELINE_LEFT);
        imports.setPrefSize(120,20);
        imports.setOnAction( e -> main.setCenter(importSettingsTab.getBuilder()));

        Button export = new Button(Localization.lang("Export sorting"));
        export.setFont(font);
        export.setAlignment(Pos.BASELINE_LEFT);
        export.setPrefSize(120,20);
        export.setOnAction( e -> main.setCenter(exportSortingPrefsTab.getBuilder()));

        Button nameFormatter = new Button(Localization.lang("Name formatter"));
        nameFormatter.setFont(font);
        nameFormatter.setAlignment(Pos.BASELINE_LEFT);
        nameFormatter.setPrefSize(120,20);
        nameFormatter.setOnAction( e -> main.setCenter(nameFormatterTab.getBuilder()));

        Button xmp = new Button(Localization.lang("XMP-metadata"));
        xmp.setFont(font);
        xmp.setAlignment(Pos.BASELINE_LEFT);
        xmp.setPrefSize(120,20);
        xmp.setOnAction( e -> main.setCenter(xmpPrefsTab.getBuilder()));

        Button network = new Button(Localization.lang("Network"));
        network.setFont(font);
        network.setAlignment(Pos.BASELINE_LEFT);
        network.setPrefSize(120,20);
        network.setOnAction(e -> main.setCenter(networkTab.getBuilder()));

        Button advanced = new Button(Localization.lang("Advanced"));
        advanced.setFont(font);
        advanced.setAlignment(Pos.BASELINE_LEFT);
        advanced.setPrefSize(120,20);
        advanced.setOnAction( e -> main.setCenter(advancedTab.getBuilder()));

        Button appearance = new Button(Localization.lang("Appearance"));
        appearance.setFont(font);
        appearance.setAlignment(Pos.BASELINE_LEFT);
        appearance.setPrefSize(120,20);
        appearance.setOnAction( e -> main.setCenter(appearancePrefsTab.getContainer()));

        vBox.getChildren().addAll(general, file, entryTable, tableColumn, entryPreview, externalPrograms, groups,
                entryEditor, bibkeyGenerator, imports, export, nameFormatter, xmp, network, advanced, appearance);

        for (int i = 0; i < 18; i++) {
            vBox.getChildren().add(new Label(""));
        }
        Button importPreferences = new Button(Localization.lang("Import preferences"));
        importPreferences.setAlignment(Pos.BASELINE_LEFT);
        importPreferences.setFont(font);
        importPreferences.setPrefSize(120,20);
        Button exportPreferences = new Button(Localization.lang("Export preferences"));
        exportPreferences.setFont(font);
        exportPreferences.setPrefSize(120,20);
        exportPreferences.setAlignment(Pos.BASELINE_LEFT);
        Button showPreferences = new Button(Localization.lang("Show preferences"));
        showPreferences.setFont(font);
        showPreferences.setAlignment(Pos.BASELINE_LEFT);
        showPreferences.setPrefSize(120,20);
        Button resetPreferences = new Button(Localization.lang("Reset preferences"));
        resetPreferences.setFont(font);
        resetPreferences.setPrefSize(120,20);
        resetPreferences.setAlignment(Pos.BASELINE_LEFT);

        vBox.getChildren().addAll(importPreferences, exportPreferences, showPreferences, resetPreferences);
        main.setLeft(vBox);

        // TODO: Key bindings:
        // KeyBinder.bindCloseDialogKeyToCancelAction(this.getRootPane(), cancelAction);

        // Import and export actions:
        exportPreferences.setAccessibleText(Localization.lang("Export preferences to file"));
        exportPreferences.setOnAction( e -> new ExportAction());

        importPreferences.setAccessibleText(Localization.lang("Import preferences from file"));
        importPreferences.setOnAction(e -> {

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

        showPreferences.setOnAction(
                e -> new PreferencesFilterDialog(new JabRefPreferencesFilter(prefs)).setVisible(true));
        resetPreferences.setOnAction(e -> {

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
