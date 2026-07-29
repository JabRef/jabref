package org.jabref.gui.preferences.entryeditor;

import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.importer.fetcher.MrDlibPreferences;
import org.jabref.logic.importer.fetcher.citation.CitationCountFetcherType;
import org.jabref.logic.journals.AbbreviationPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.msc.MscCodeLoader;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.URLUtil;

import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntryEditorTabViewModel implements PreferenceTabViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryEditorTabViewModel.class);

    private final BooleanProperty openOnNewEntryProperty = new SimpleBooleanProperty();
    private final BooleanProperty defaultSourceProperty = new SimpleBooleanProperty();
    private final BooleanProperty acceptRecommendationsProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableValidationProperty = new SimpleBooleanProperty();
    private final BooleanProperty allowIntegerEditionProperty = new SimpleBooleanProperty();
    private final BooleanProperty journalPopupProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoLinkEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableMscKeywordDescriptionsProperty = new SimpleBooleanProperty();
    private final ObjectProperty<CitationCountFetcherType> citationCountFetcherTypeProperty = new SimpleObjectProperty<>();
    private final ListProperty<CitationCountFetcherType> citationCountFetcherTypes =
            new SimpleListProperty<>(FXCollections.observableArrayList(CitationCountFetcherType.values()));

    /// Working copy of tab configurations — not the live preferences list.
    /// Written to preferences only in {@link #storeSettings()}.
    private final ObservableList<EntryEditorTabModel> tabModels = FXCollections.observableArrayList();

    private final DialogService dialogService;
    private final EntryEditorPreferences entryEditorPreferences;
    private final MrDlibPreferences mrDlibPreferences;
    private final AbbreviationPreferences abbreviationPreferences;
    private final TaskExecutor taskExecutor;
    private boolean mscKeywordDescriptionsInitialized;

    public EntryEditorTabViewModel(DialogService dialogService,
                                   EntryEditorPreferences entryEditorPreferences,
                                   MrDlibPreferences mrDlibPreferences,
                                   AbbreviationPreferences abbreviationPreferences,
                                   TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.entryEditorPreferences = entryEditorPreferences;
        this.mrDlibPreferences = mrDlibPreferences;
        this.abbreviationPreferences = abbreviationPreferences;
        this.taskExecutor = taskExecutor;

        EasyBind.subscribe(enableMscKeywordDescriptionsProperty, this::onMscKeywordDescriptionsChanged);
    }

    @Override
    public void setValues() {
        // The Preview tab is configured via the "show preview as a separate tab" preference, not here,
        // so it is omitted from the configurable tab list (its model visibility bit is unused).
        tabModels.setAll(entryEditorPreferences.getTabModels().stream()
                                               .filter(model -> !model.isPreview())
                                               .toList());

        openOnNewEntryProperty.setValue(entryEditorPreferences.shouldOpenOnNewEntry());
        defaultSourceProperty.setValue(entryEditorPreferences.showSourceTabByDefault());
        acceptRecommendationsProperty.setValue(mrDlibPreferences.shouldAcceptRecommendations());
        enableValidationProperty.setValue(entryEditorPreferences.shouldEnableValidation());
        allowIntegerEditionProperty.setValue(entryEditorPreferences.shouldAllowIntegerEditionBibtex());
        journalPopupProperty.setValue(entryEditorPreferences.shouldEnableJournalPopup() == EntryEditorPreferences.JournalPopupEnabled.ENABLED);
        autoLinkEnabledProperty.setValue(entryEditorPreferences.autoLinkFilesEnabled());
        enableMscKeywordDescriptionsProperty.setValue(abbreviationPreferences.shouldEnableMscKeywordDescriptions());
        citationCountFetcherTypeProperty.setValue(entryEditorPreferences.getCitationCountFetcherType());
        mscKeywordDescriptionsInitialized = true;
    }

    public void resetToDefaults() {
        for (int i = 0; i < tabModels.size(); i++) {
            if (tabModels.get(i) instanceof EntryEditorTabModel.BuiltInTab builtIn && !builtIn.isVisible()) {
                tabModels.set(i, builtIn.withVisible(true));
            }
        }
    }

    /// Toggles the visibility of a tab. Called by the cell's checkbox.
    public void toggleTabVisibility(EntryEditorTabModel config) {
        int index = tabModels.indexOf(config);
        if (index < 0) {
            return;
        }
        tabModels.set(index, config.withVisible(!config.isVisible()));
    }

    @Override
    public void storeSettings() {
        entryEditorPreferences.setShouldOpenOnNewEntry(openOnNewEntryProperty.getValue());
        entryEditorPreferences.setShowSourceTabByDefault(defaultSourceProperty.getValue());
        entryEditorPreferences.setEnableValidation(enableValidationProperty.getValue());
        entryEditorPreferences.setAllowIntegerEditionBibtex(allowIntegerEditionProperty.getValue());
        entryEditorPreferences.setEnableJournalPopup(journalPopupProperty.getValue()
                                                     ? EntryEditorPreferences.JournalPopupEnabled.ENABLED
                                                     : EntryEditorPreferences.JournalPopupEnabled.DISABLED);
        entryEditorPreferences.setAutoLinkFilesEnabled(autoLinkEnabledProperty.getValue());
        mrDlibPreferences.setAcceptRecommendations(acceptRecommendationsProperty.getValue());
        entryEditorPreferences.setCitationCountFetcherType(citationCountFetcherTypeProperty.getValue());
        abbreviationPreferences.setShouldEnableMscKeywordDescriptions(enableMscKeywordDescriptionsProperty.getValue());

        // Write tab visibility from the working copy
        for (EntryEditorTabModel tabModel : tabModels) {
            if (tabModel instanceof EntryEditorTabModel.BuiltInTab(
                    EntryEditorTabModel.BuiltIn key,
                    boolean _
            )) {
                entryEditorPreferences.setTabVisible(key, tabModel.isVisible());
            }
        }
    }

    // region Properties

    public ObservableList<EntryEditorTabModel> getTabModels() {
        return tabModels;
    }

    public BooleanProperty openOnNewEntryProperty() {
        return openOnNewEntryProperty;
    }

    public BooleanProperty defaultSourceProperty() {
        return defaultSourceProperty;
    }

    public BooleanProperty acceptRecommendationsProperty() {
        return acceptRecommendationsProperty;
    }

    public BooleanProperty enableValidationProperty() {
        return enableValidationProperty;
    }

    public BooleanProperty allowIntegerEditionProperty() {
        return allowIntegerEditionProperty;
    }

    public BooleanProperty journalPopupProperty() {
        return journalPopupProperty;
    }

    public BooleanProperty autoLinkFilesEnabledProperty() {
        return autoLinkEnabledProperty;
    }

    public BooleanProperty enableMscKeywordDescriptionsProperty() {
        return enableMscKeywordDescriptionsProperty;
    }

    public ObjectProperty<CitationCountFetcherType> citationCountFetcherTypeProperty() {
        return citationCountFetcherTypeProperty;
    }

    public ReadOnlyListProperty<CitationCountFetcherType> citationCountFetcherTypes() {
        return citationCountFetcherTypes;
    }

    // endregion

    private void onMscKeywordDescriptionsChanged(Boolean newValue) {
        if (!mscKeywordDescriptionsInitialized) {
            return;
        }

        if (Boolean.TRUE.equals(newValue)) {
            boolean accepted = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("License agreement for MSC codes"),
                    Localization.lang("The MSC codes are provided under the Creative Commons Attribution-ShareAlike-NonCommercial 4.0 International License.")
                            + "\n\n"
                            + Localization.lang("By enabling this feature, you agree to the terms of this license.")
                            + "\n"
                            + "https://creativecommons.org/licenses/by-nc-sa/4.0/",
                    Localization.lang("Accept"),
                    Localization.lang("Decline"));

            if (accepted) {
                downloadMscCodes();
            } else {
                enableMscKeywordDescriptionsProperty.setValue(false);
            }
        }
    }

    private void downloadMscCodes() {
        Path mscMvFile = Directories.getMscDirectory().resolve(MscCodeLoader.MSC_FILE_NAME);
        if (MscCodeLoader.isMvStoreAvailableWithData(mscMvFile)) {
            return;
        }

        dialogService.notify(Localization.lang("Downloading MSC codes..."));

        BackgroundTask.wrap(() -> {
                          MscCodeLoader.downloadAndConvert(URLUtil.create(MscCodeLoader.MSC_CSV_URL), mscMvFile);
                          return null;
                      })
                      .onSuccess(_ -> dialogService.notify(Localization.lang("MSC codes downloaded successfully.")))
                      .onFailure(e -> {
                          LOGGER.error("Error downloading MSC codes", e);
                          dialogService.showErrorDialogAndWait(Localization.lang("Error downloading MSC codes"), e);
                      })
                      .executeWith(taskExecutor);
    }
}
