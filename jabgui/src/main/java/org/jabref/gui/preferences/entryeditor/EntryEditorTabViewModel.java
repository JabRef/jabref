package org.jabref.gui.preferences.entryeditor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.jabref.logic.util.strings.StringUtil;

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
    /// Written to preferences only in [#storeSettings()]. Excludes the Preview tab (see [#setValues()]).
    private final ObservableList<EditorTabViewModel> tabs = FXCollections.observableArrayList();

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
        tabs.setAll(entryEditorPreferences.getTabModels().stream()
                                          .filter(model -> !model.isPreview())
                                          .map(EditorTabViewModel::fromModel)
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

    /// Restores the default tab set: every built-in tab visible in default order, no custom tabs.
    public void resetToDefaults() {
        tabs.setAll(EntryEditorPreferences.getDefault().getTabModels().stream()
                                          .filter(model -> !model.isPreview())
                                          .map(EditorTabViewModel::fromModel)
                                          .toList());
    }

    /// Adds a custom tab with the given name, or returns the existing tab when one with that name
    /// (custom or built-in, compared case-insensitively) is already present. Empty for a blank name.
    public Optional<EditorTabViewModel> addCustomTab(String name) {
        if (StringUtil.isBlank(name)) {
            return Optional.empty();
        }
        String trimmed = name.trim();
        Optional<EditorTabViewModel> existing = tabs.stream()
                                                    .filter(tab -> tab.getDisplayName().equalsIgnoreCase(trimmed))
                                                    .findFirst();
        if (existing.isPresent()) {
            return existing;
        }
        EditorTabViewModel tab = EditorTabViewModel.newCustomTab(trimmed);
        tabs.add(tab);
        return Optional.of(tab);
    }

    /// Adds the classic "General" and "Abstract" tabs (see [EntryEditorTabModel.CustomizedFieldsTab#classicTabs()])
    /// directly after "Main". A classic tab is skipped when it is already present (by stored name), when a
    /// tab with its display name exists, or when a custom tab already has exactly its fields.
    // [impl->req~entry-editor.classic-tabs~1]
    public void addClassicTabs() {
        // Located by type, not display name: a custom tab could be named like the "Main" tab.
        int insertAt = 1 + IntStream.range(0, tabs.size())
                                    .filter(index -> tabs.get(index).isBuiltIn(EntryEditorTabModel.BuiltIn.ALL_FIELDS))
                                    .findFirst()
                                    .orElse(-1);
        for (EntryEditorTabModel.CustomizedFieldsTab classicTab : EntryEditorTabModel.CustomizedFieldsTab.classicTabs()) {
            Set<String> classicFields = lowerCaseSet(classicTab.fieldPatterns());
            boolean exists = tabs.stream().anyMatch(tab ->
                    tab.getCustomName().equals(classicTab.name())
                            || tab.getDisplayName().equalsIgnoreCase(classicTab.displayName())
                            || (tab.isCustom() && lowerCaseSet(tab.getFieldPatterns()).equals(classicFields)));
            if (!exists) {
                tabs.add(insertAt++, EditorTabViewModel.fromModel(classicTab));
            }
        }
    }

    private static Set<String> lowerCaseSet(List<String> patterns) {
        // Locale.ROOT: with a Turkish UI locale, "FILE".toLowerCase() is "fıle" and would not match "file".
        return patterns.stream().map(pattern -> pattern.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    public void removeTab(EditorTabViewModel tab) {
        if (tab.isCustom()) {
            tabs.remove(tab);
        }
    }

    /// Adds a field pattern to the given custom tab; `false` if the pattern is blank or already on that tab.
    public boolean addFieldPattern(EditorTabViewModel tab, String pattern) {
        if (StringUtil.isBlank(pattern)) {
            return false;
        }
        String trimmed = pattern.trim();
        if (containsIgnoreCase(tab.getFieldPatterns(), trimmed)) {
            return false;
        }
        tab.getFieldPatterns().add(trimmed);
        return true;
    }

    public void removeFieldPattern(EditorTabViewModel tab, String pattern) {
        tab.getFieldPatterns().remove(pattern);
    }

    /// `true` if the pattern occurs on more than one tab (duplicates within one tab are prevented on add) —
    /// the fields table shows a warning sign on such rows. Literal comparison only; overlap between a regex
    /// pattern and the field names it captures is not detected.
    public boolean isFieldPatternDuplicated(String pattern) {
        return tabs.stream()
                   .filter(tab -> containsIgnoreCase(tab.getFieldPatterns(), pattern))
                   .count() > 1;
    }

    private static boolean containsIgnoreCase(List<String> patterns, String pattern) {
        return patterns.stream().anyMatch(existing -> existing.equalsIgnoreCase(pattern));
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

        // Replace the tab list wholesale from the working copy: the Preview tab (filtered out in
        // setValues()) stays in front, everything else takes the working copy's order and content.
        List<EntryEditorTabModel> newModels = new ArrayList<>();
        entryEditorPreferences.getTabModels().stream()
                              .filter(EntryEditorTabModel::isPreview)
                              .forEach(newModels::add);
        tabs.stream().map(EditorTabViewModel::toModel).forEach(newModels::add);
        entryEditorPreferences.getTabModels().setAll(newModels);
    }

    // region Properties

    public ObservableList<EditorTabViewModel> getTabs() {
        return tabs;
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
