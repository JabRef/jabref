package org.jabref.gui.preferences;

import java.util.List;
import java.util.prefs.BackingStoreException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.Globals;
import org.jabref.JabRefException;
import org.jabref.gui.DialogService;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.JabRefPreferencesFilter;

import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Preferences dialog. Contains a TabbedPane, and tabs will be defined in separate classes. Tabs MUST implement the
 * PrefsTab interface, since this dialog will call the storeSettings() method of all tabs when the user presses ok.
 *
 * With this design, it should be very easy to add new tabs later.
 */
public class PreferencesDialog extends BaseDialog<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesDialog.class);

    private final BorderPane container;

    private final DialogService dialogService;
    private final JabRefFrame frame;
    private final JabRefPreferences prefs;
    private final ObservableList<PrefsTab> preferenceTabs;

    public PreferencesDialog(JabRefFrame parent, TaskExecutor taskExecutor) {
        setTitle(Localization.lang("JabRef preferences"));
        getDialogPane().getScene().getStylesheets().add(this.getClass().getResource("PreferencesDialog.css").toExternalForm());

        ButtonType save = new ButtonType(Localization.lang("Save"), ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        ControlHelper.setAction(save, getDialogPane(), event -> {
            storeAllSettings();
            close();
        });

        prefs = Globals.prefs;
        frame = parent;
        dialogService = frame.getDialogService();

        preferenceTabs = FXCollections.observableArrayList();
        preferenceTabs.add(new GeneralTab(dialogService, prefs));
        preferenceTabs.add(new FileTab(dialogService, prefs));
        preferenceTabs.add(new TablePrefsTab(prefs));
        preferenceTabs.add(new TableColumnsTab(prefs, frame));
        preferenceTabs.add(new PreviewPrefsTab(dialogService, ExternalFileTypes.getInstance(), taskExecutor));
        preferenceTabs.add(new ExternalTab(frame, this, prefs));
        preferenceTabs.add(new GroupsPrefsTab(prefs));
        preferenceTabs.add(new EntryEditorPrefsTab(prefs));
        preferenceTabs.add(new BibtexKeyPatternPrefTab(prefs, frame.getCurrentBasePanel()));
        preferenceTabs.add(new ImportSettingsTab(prefs));
        preferenceTabs.add(new ExportSortingPrefsTab(prefs));
        preferenceTabs.add(new NameFormatterTab(prefs));
        preferenceTabs.add(new XmpPrefsTab(prefs));
        preferenceTabs.add(new NetworkTab(dialogService, prefs));
        preferenceTabs.add(new AdvancedTab(dialogService, prefs));
        preferenceTabs.add(new AppearancePrefsTab(dialogService, prefs));

        container = new BorderPane();
        getDialogPane().setContent(container);
        construct();
    }

    private void construct() {
        VBox vBox = new VBox();
        vBox.setPrefWidth(160);

        ListView<PrefsTab> tabsList = new ListView<>();
        tabsList.setId("sideMenu");
        tabsList.itemsProperty().setValue(preferenceTabs);
        EasyBind.subscribe(tabsList.getSelectionModel().selectedItemProperty(), tab -> {
            if (tab != null) {
                ScrollPane preferencePaneContainer = new ScrollPane(tab.getBuilder());
                preferencePaneContainer.getStyleClass().add("preferencePaneContainer");
                container.setCenter(preferencePaneContainer);
            }
        });
        tabsList.getSelectionModel().selectFirst();
        new ViewModelListCellFactory<PrefsTab>()
                .withText(PrefsTab::getTabName)
                .install(tabsList);

        VBox buttonContainer = new VBox();
        buttonContainer.setAlignment(Pos.BOTTOM_LEFT);
        buttonContainer.setSpacing(3.0);
        Button importPreferences = new Button(Localization.lang("Import preferences"));
        importPreferences.setTooltip(new Tooltip(Localization.lang("Import preferences from file")));
        importPreferences.setOnAction(e -> importPreferences());
        importPreferences.setMaxWidth(Double.MAX_VALUE);
        Button exportPreferences = new Button(Localization.lang("Export preferences"));
        exportPreferences.setTooltip(new Tooltip(Localization.lang("Export preferences to file")));
        exportPreferences.setOnAction(e -> exportPreferences());
        exportPreferences.setMaxWidth(Double.MAX_VALUE);
        Button showPreferences = new Button(Localization.lang("Show preferences"));
        showPreferences.setOnAction(e -> new PreferencesFilterDialog(new JabRefPreferencesFilter(prefs)).showAndWait());
        showPreferences.setMaxWidth(Double.MAX_VALUE);
        Button resetPreferences = new Button(Localization.lang("Reset preferences"));
        resetPreferences.setOnAction(e -> resetPreferences());
        resetPreferences.setMaxWidth(Double.MAX_VALUE);
        buttonContainer.getChildren().addAll(
                importPreferences,
                exportPreferences,
                showPreferences,
                resetPreferences);

        VBox spacer = new VBox();
        spacer.setPrefHeight(10.0);
        VBox.setVgrow(tabsList, Priority.ALWAYS);
        VBox.setVgrow(spacer, Priority.SOMETIMES);
        vBox.getChildren().addAll(
                tabsList,
                spacer,
                buttonContainer);

        container.setLeft(vBox);

        setValues();
    }

    private void resetPreferences() {
        boolean resetPreferencesConfirmed = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Reset preferences"),
                Localization.lang("Are you sure you want to reset all settings to default values?"),
                Localization.lang("Reset preferences"),
                Localization.lang("Cancel"));
        if (resetPreferencesConfirmed) {
            try {
                prefs.clear();

                dialogService.showWarningDialogAndWait(Localization.lang("Reset preferences"),
                        Localization.lang("You must restart JabRef for this to come into effect."));
            } catch (BackingStoreException ex) {
                LOGGER.error("Error while resetting preferences", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Reset preferences"), ex);
            }
            updateAfterPreferenceChanges();
        }
    }

    private void importPreferences() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.XML)
                .withDefaultExtension(StandardFileType.XML)
                .withInitialDirectory(prefs.setLastPreferencesExportPath()).build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> {
            try {
                prefs.importPreferences(file);
                updateAfterPreferenceChanges();

                dialogService.showWarningDialogAndWait(Localization.lang("Import preferences"),
                        Localization.lang("You must restart JabRef for this to come into effect."));
            } catch (JabRefException ex) {
                LOGGER.error("Error while importing preferences", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Import preferences"), ex);
            }
        });
    }

    private void updateAfterPreferenceChanges() {
        setValues();
        List<TemplateExporter> customExporters = prefs.getCustomExportFormats(Globals.journalAbbreviationLoader);
        LayoutFormatterPreferences layoutPreferences = prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader);
        SavePreferences savePreferences = prefs.loadForExportFromPreferences();
        XmpPreferences xmpPreferences = prefs.getXMPPreferences();
        Globals.exportFactory = ExporterFactory.create(customExporters, layoutPreferences, savePreferences, xmpPreferences);
        prefs.updateEntryEditorTabList();
    }

    private void storeAllSettings() {
        // First check that all tabs are ready to close:
        for (PrefsTab tab : preferenceTabs) {
            if (!tab.validateSettings()) {
                return; // If not, break off.
            }
        }
        // Then store settings and close:
        for (PrefsTab tab : preferenceTabs) {
            tab.storeSettings();
        }
        prefs.flush();

        GUIGlobals.updateEntryEditorColors();
        frame.setupAllTables();
        frame.output(Localization.lang("Preferences recorded."));
    }

    public void setValues() {
        // Update all field values in the tabs:
        for (PrefsTab prefsTab : preferenceTabs) {
            prefsTab.setValues();
        }
    }

    private void exportPreferences() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.XML)
                .withDefaultExtension(StandardFileType.XML)
                .withInitialDirectory(prefs.setLastPreferencesExportPath())
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(exportFile -> {
                         try {
                             storeAllSettings();
                             prefs.exportPreferences(exportFile);
                             prefs.setLastPreferencesExportPath(exportFile);
                         } catch (JabRefException ex) {
                             LOGGER.warn(ex.getMessage(), ex);
                             dialogService.showErrorDialogAndWait(Localization.lang("Export preferences"), ex);
                         }
                     });
    }
}
