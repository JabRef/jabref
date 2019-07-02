package org.jabref.gui.preferences;

import java.util.List;
import java.util.prefs.BackingStoreException;

import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.Globals;
import org.jabref.JabRefException;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.JabRefPreferencesFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesDialogViewModel.class);

    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final JabRefPreferences prefs;
    private final ObservableList<PrefsTab> preferenceTabs;
    private final JabRefFrame frame;

    public PreferencesDialogViewModel(DialogService dialogService, TaskExecutor taskExecutor, JabRefFrame frame) {
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.prefs = Globals.prefs;
        this.frame = frame;

        preferenceTabs = FXCollections.observableArrayList();
        preferenceTabs.add(new GeneralTabView(prefs));
        preferenceTabs.add(new FileTabView(prefs));
        preferenceTabs.add(new TablePrefsTab(prefs));
        preferenceTabs.add(new TableColumnsTab(prefs, frame));
        preferenceTabs.add(new PreviewPreferencesTab(dialogService, this.taskExecutor));
        preferenceTabs.add(new ExternalTabView(prefs, frame));
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
    }

    public ObservableList<PrefsTab> getPreferenceTabs() {
        return new ReadOnlyListWrapper<>(preferenceTabs);
    }

    public void importPreferences() {
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

    public void exportPreferences() {
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

    public void showPreferences() {
        new PreferencesFilterDialog(new JabRefPreferencesFilter(prefs)).showAndWait();
    }

    public void resetPreferences() {
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

    /**
     * Reloads the JabRefPreferences into the UI
     */
    private void updateAfterPreferenceChanges() {
        setValues();

        List<TemplateExporter> customExporters = prefs.getCustomExportFormats(Globals.journalAbbreviationLoader);
        LayoutFormatterPreferences layoutPreferences = prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader);
        SavePreferences savePreferences = prefs.loadForExportFromPreferences();
        XmpPreferences xmpPreferences = prefs.getXMPPreferences();
        Globals.exportFactory = ExporterFactory.create(customExporters, layoutPreferences, savePreferences, xmpPreferences);
        prefs.updateEntryEditorTabList();
    }

    /**
     * Checks if all tabs are valid
     * ToDo: After conversion of all tabs use mvvmfx-validator
     * ToDo: should be observable for binding of OK-button in View
     */

    public boolean validSettings() {
        for (PrefsTab tab : preferenceTabs) {
            if (!tab.validateSettings()) {
                return false;
            }
        }
        return true;
    }

    public void storeAllSettings() {
        // Run validation checks
        if (!validSettings()) {
            return;
        }

        // Store settings
        for (PrefsTab tab : preferenceTabs) {
            tab.storeSettings(); // ToDo: After conversion of all tabs: prefsTab.getViewModel().storeSettings();
        }
        prefs.flush();

        GUIGlobals.updateEntryEditorColors();
        frame.setupAllTables();
        frame.getGlobalSearchBar().updateHintVisibility();
        dialogService.notify(Localization.lang("Preferences recorded."));
    }

    /**
     * Inserts the JabRefPreferences-values into the the Properties of the ViewModel
     * ToDo: Reword after conversion of all tabs: resetValues()
     */
    public void setValues() {
        for (PrefsTab prefsTab : preferenceTabs) {
            prefsTab.setValues(); // ToDo: After conversion of all tabs: prefsTab.getViewModel().setValues();
        }
    }
}
