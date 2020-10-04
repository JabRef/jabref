package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;

import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.push.PushToApplicationsManager;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.JabRefException;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.preferences.ExternalApplicationsPreferences;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.JabRefPreferencesFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesDialogViewModel.class);

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final ObservableList<PreferencesTab> preferenceTabs;
    private final JabRefFrame frame;

    public PreferencesDialogViewModel(DialogService dialogService, JabRefFrame frame) {
        this.dialogService = dialogService;
        this.preferences = Globals.prefs;
        this.frame = frame;

        preferenceTabs = FXCollections.observableArrayList(
                new GeneralTabView(preferences),
                new FileTabView(preferences),
                new TableTabView(preferences),
                new PreviewTabView(preferences),
                new ExternalTabView(preferences, frame.getPushToApplicationsManager()),
                new GroupsTabView(preferences),
                new EntryEditorTabView(preferences),
                new CitationKeyPatternTabView(preferences),
                new LinkedFilesTabView(preferences),
                new NameFormatterTabView(preferences),
                new XmpPrivacyTabView(preferences),
                new NetworkTabView(preferences),
                new AppearanceTabView(preferences)
        );
    }

    public ObservableList<PreferencesTab> getPreferenceTabs() {
        return new ReadOnlyListWrapper<>(preferenceTabs);
    }

    public void importPreferences() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.XML)
                .withDefaultExtension(StandardFileType.XML)
                .withInitialDirectory(preferences.getLastPreferencesExportPath()).build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> {
            try {
                preferences.importPreferences(file);
                updateAfterPreferenceChanges();

                dialogService.showWarningDialogAndWait(Localization.lang("Import preferences"),
                        Localization.lang("You must restart JabRef for this to come into effect."));
            } catch (JabRefException ex) {
                LOGGER.error("Error while importing preferences", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Import preferences"), ex);
            }
        });
    }

    public void exportPreferences() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.XML)
                .withDefaultExtension(StandardFileType.XML)
                .withInitialDirectory(preferences.getLastPreferencesExportPath())
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(exportFile -> {
                         try {
                             storeAllSettings();
                             preferences.exportPreferences(exportFile);
                             preferences.storeLastPreferencesExportPath(exportFile);
                         } catch (JabRefException ex) {
                             LOGGER.warn(ex.getMessage(), ex);
                             dialogService.showErrorDialogAndWait(Localization.lang("Export preferences"), ex);
                         }
                     });
    }

    public void showPreferences() {
        new PreferencesFilterDialog(new JabRefPreferencesFilter(preferences)).showAndWait();
    }

    public void resetPreferences() {
        boolean resetPreferencesConfirmed = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Reset preferences"),
                Localization.lang("Are you sure you want to reset all settings to default values?"),
                Localization.lang("Reset preferences"),
                Localization.lang("Cancel"));
        if (resetPreferencesConfirmed) {
            try {
                preferences.clear();

                dialogService.showWarningDialogAndWait(Localization.lang("Reset preferences"),
                        Localization.lang("You must restart JabRef for this to come into effect."));
            } catch (BackingStoreException ex) {
                LOGGER.error("Error while resetting preferences", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Reset preferences"), ex);
            }

            updateAfterPreferenceChanges();
        }
    }

    /**
     * Reloads the JabRefPreferences into the UI
     */
    private void updateAfterPreferenceChanges() {
        // Reload internal preferences cache
        preferences.updateEntryEditorTabList();
        preferences.updateGlobalCitationKeyPattern();
        preferences.updateMainTableColumns();

        setValues();

        List<TemplateExporter> customExporters = preferences.getCustomExportFormats(Globals.journalAbbreviationRepository);
        LayoutFormatterPreferences layoutPreferences = preferences.getLayoutFormatterPreferences(Globals.journalAbbreviationRepository);
        SavePreferences savePreferences = preferences.getSavePreferencesForExport();
        XmpPreferences xmpPreferences = preferences.getXmpPreferences();
        Globals.exportFactory = ExporterFactory.create(customExporters, layoutPreferences, savePreferences, xmpPreferences);

        ExternalApplicationsPreferences externalApplicationsPreferences = preferences.getExternalApplicationsPreferences();
        PushToApplicationsManager manager = frame.getPushToApplicationsManager();
        manager.updateApplicationAction(manager.getApplicationByName(externalApplicationsPreferences.getPushToApplicationName()));

        frame.getBasePanelList().forEach(panel -> panel.getMainTable().getTableModel().refresh());
    }

    /**
     * Checks if all tabs are valid
     */
    public boolean validSettings() {
        for (PreferencesTab tab : preferenceTabs) {
            if (!tab.validateSettings()) {
                return false;
            }
        }
        return true;
    }

    public void storeAllSettings() {
        List<String> restartWarnings = new ArrayList<>();

        // Run validation checks
        if (!validSettings()) {
            return;
        }

        // Store settings
        for (PreferencesTab tab : preferenceTabs) {
            tab.storeSettings();
            restartWarnings.addAll(tab.getRestartWarnings());
        }
        preferences.flush();

        if (!restartWarnings.isEmpty()) {
            dialogService.showWarningDialogAndWait(Localization.lang("Restart required"),
                    String.join(",\n", restartWarnings)
                            + "\n\n"
                            + Localization.lang("You must restart JabRef for this to come into effect."));
        }

        frame.setupAllTables();
        frame.getGlobalSearchBar().updateHintVisibility();
        dialogService.notify(Localization.lang("Preferences recorded."));

        updateAfterPreferenceChanges();
    }

    /**
     * Inserts the JabRefPreferences-values into the the Properties of the ViewModel
     */
    public void setValues() {
        for (PreferencesTab preferencesTab : preferenceTabs) {
            preferencesTab.setValues();
        }
    }
}
