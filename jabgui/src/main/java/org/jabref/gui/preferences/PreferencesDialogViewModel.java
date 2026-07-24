package org.jabref.gui.preferences;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.prefs.BackingStoreException;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.ai.AiTab;
import org.jabref.gui.preferences.autocompletion.AutoCompletionTab;
import org.jabref.gui.preferences.citationkeypattern.CitationKeyPatternTab;
import org.jabref.gui.preferences.customentrytypes.CustomEntryTypesTab;
import org.jabref.gui.preferences.customexporter.CustomExporterTab;
import org.jabref.gui.preferences.customimporter.CustomImporterTab;
import org.jabref.gui.preferences.entry.EntryTab;
import org.jabref.gui.preferences.entryeditor.EntryEditorTab;
import org.jabref.gui.preferences.export.ExportTab;
import org.jabref.gui.preferences.external.ExternalTab;
import org.jabref.gui.preferences.externalfiletypes.ExternalFileTypesTab;
import org.jabref.gui.preferences.general.GeneralTab;
import org.jabref.gui.preferences.groups.GroupsTab;
import org.jabref.gui.preferences.journals.JournalAbbreviationsTab;
import org.jabref.gui.preferences.keybindings.KeyBindingsTab;
import org.jabref.gui.preferences.linkedfiles.LinkedFilesTab;
import org.jabref.gui.preferences.nameformatter.NameFormatterTab;
import org.jabref.gui.preferences.network.NetworkTab;
import org.jabref.gui.preferences.ocr.OcrTab;
import org.jabref.gui.preferences.preview.PreviewTab;
import org.jabref.gui.preferences.protectedterms.ProtectedTermsTab;
import org.jabref.gui.preferences.table.TableTab;
import org.jabref.gui.preferences.websearch.WebSearchTab;
import org.jabref.gui.preferences.xmp.XmpPrivacyTab;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesDialogViewModel.class);

    private final SimpleBooleanProperty memoryStickProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final ObservableList<PreferencesTab> preferenceTabs;

    public PreferencesDialogViewModel(DialogService dialogService, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;

        // This enables passing unsaved preference values from the AI tab to the "web search" tab.
        Optional<AiTab> aiTab = createAiTab(AiTab::new);
        ReadOnlyBooleanProperty aiEnabled = aiTab.map(AiTab::aiEnabledProperty)
                                                 .orElseGet(() -> new SimpleBooleanProperty(
                                                         preferences.getAiPreferences().getAiFeaturesEnabledCurrently()));

        preferenceTabs = FXCollections.observableArrayList(
                new GeneralTab(),
                new KeyBindingsTab(),
                new GroupsTab(),
                new WebSearchTab(aiEnabled)
        );
        aiTab.ifPresent(preferenceTabs::add);
        preferenceTabs.addAll(
                new EntryTab(),
                new TableTab(),
                new PreviewTab(),
                new EntryEditorTab(),
                new CustomEntryTypesTab(),
                new CitationKeyPatternTab(),
                new LinkedFilesTab(),
                new OcrTab(),
                new ExportTab(),
                new AutoCompletionTab(),
                new ProtectedTermsTab(),
                new ExternalTab(),
                new ExternalFileTypesTab(),
                new JournalAbbreviationsTab(),
                new NameFormatterTab(),
                new XmpPrivacyTab(),
                new CustomImporterTab(),
                new CustomExporterTab(),
                new NetworkTab()
        );
    }

    static Optional<AiTab> createAiTab(Supplier<AiTab> aiTabSupplier) {
        try {
            return Optional.of(aiTabSupplier.get());
        } catch (NoClassDefFoundError error) {
            LOGGER.error("Could not initialize AI preferences because a required class could not be loaded", error);
            return Optional.empty();
        } catch (RuntimeException error) {
            if (causedByNoClassDefFoundError(error)) {
                LOGGER.error("Could not initialize AI preferences because a required class could not be loaded", error);
                return Optional.empty();
            }
            throw error;
        }
    }

    private static boolean causedByNoClassDefFoundError(Throwable error) {
        if (error instanceof NoClassDefFoundError) {
            return true;
        }
        return Optional.ofNullable(error.getCause())
                       .map(PreferencesDialogViewModel::causedByNoClassDefFoundError)
                       .orElse(false);
    }

    public ObservableList<PreferencesTab> getPreferenceTabs() {
        return new ReadOnlyListWrapper<>(preferenceTabs);
    }

    public boolean importPreferences() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.XML)
                .withDefaultExtension(StandardFileType.XML)
                .withInitialDirectory(preferences.getInternalPreferences().getLastPreferencesExportPath()).build();

        Optional<Path> importFileOpt = dialogService.showFileOpenDialog(fileDialogConfiguration);
        if (importFileOpt.isPresent()) {
            try {
                preferences.importPreferences(importFileOpt.get());
                setValues();

                return true;
            } catch (JabRefException ex) {
                LOGGER.error("Error while importing preferences", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Import preferences"), ex);
            }
        }

        return false;
    }

    public void exportPreferences() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.XML)
                .withDefaultExtension(StandardFileType.XML)
                .withInitialDirectory(preferences.getInternalPreferences().getLastPreferencesExportPath())
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(exportFile -> {
                         try {
                             storeAllSettings();
                             preferences.exportPreferences(exportFile);
                             preferences.getInternalPreferences().setLastPreferencesExportPath(exportFile);
                         } catch (JabRefException ex) {
                             LOGGER.warn(ex.getMessage(), ex);
                             dialogService.showErrorDialogAndWait(Localization.lang("Export preferences"), ex);
                         }
                     });
    }

    public void showPreferences() {
        dialogService.showCustomDialogAndWait(new PreferencesFilterDialog(new PreferencesFilter(preferences)));
    }

    /// returns true if the preferences have been reset
    public boolean resetPreferences() {
        boolean resetPreferencesConfirmed = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Reset preferences"),
                Localization.lang("Are you sure you want to reset all settings to default values?"),
                Localization.lang("Reset preferences"),
                Localization.lang("Cancel"));
        if (resetPreferencesConfirmed) {
            try {
                preferences.clear();
                setValues();

                return true;
            } catch (BackingStoreException ex) {
                LOGGER.error("Error while resetting preferences", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Reset preferences"), ex);
            }
        }

        return false;
    }

    /// Checks if all tabs are valid
    public boolean validSettings() {
        for (PreferencesTab tab : preferenceTabs) {
            if (!tab.validateSettings()) {
                return false;
            }
        }
        return true;
    }

    public void storeAllSettings() {
        if (!validSettings()) {
            return;
        }

        // Store settings
        preferences.getInternalPreferences().setMemoryStickMode(memoryStickProperty.get());
        List<String> restartWarnings = new ArrayList<>();
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

        Injector.setModelOrService(BibEntryTypesManager.class, preferences.getCustomEntryTypesRepository());
        dialogService.notify(Localization.lang("Preferences recorded."));
    }

    /// Inserts the preference values into the Properties of the ViewModel
    public void setValues() {
        memoryStickProperty.setValue(preferences.getInternalPreferences().isMemoryStickMode());

        for (PreferencesTab preferencesTab : preferenceTabs) {
            preferencesTab.setValues();
        }
    }

    public BooleanProperty getMemoryStickProperty() {
        return memoryStickProperty;
    }
}
