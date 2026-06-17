package org.jabref.gui.preferences.entryeditor;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.gui.preferences.GuiPreferences;
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
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;

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

    /// Working copy of tab configurations — not the live preferences list.
    /// Written to preferences only in {@link #storeSettings()}.
    private final ObservableList<EntryEditorTabModel> tabConfigs = FXCollections.observableArrayList();

    /// The tab currently selected in the list view. Drives {@link #selectedTabName} and
    /// {@link #selectedTabFields}. Updated by the view via {@code EasyBind.subscribe} on the
    /// list's {@code selectedItemProperty}. Changing it commits the previous FieldSet edit first.
    private final ObjectProperty<EntryEditorTabModel> selectedTab = new SimpleObjectProperty<>();

    /// Staging property for the name of the currently selected field-set tab.
    /// Not written to {@link #tabConfigs} until selection changes or {@link #storeSettings()} is called.
    private final StringProperty selectedTabName = new SimpleStringProperty("");

    /// Staging list for the fields of the currently selected field-set tab.
    private final ObservableList<Field> selectedTabFields = FXCollections.observableArrayList();

    private final BooleanProperty fieldSetTabSelected = new SimpleBooleanProperty(false);
    private final BooleanProperty canRemoveSelectedTab = new SimpleBooleanProperty(false);

    /// All known fields, sorted by name, provided as suggestions in the fields editor.
    private final List<Field> allKnownFields = Stream.<Field>concat(
                                                             java.util.Arrays.stream(StandardField.values()),
                                                             java.util.Arrays.stream(SpecialField.values()))
                                                     .sorted(Comparator.comparing(Field::getName))
                                                     .toList();

    private final DialogService dialogService;
    private final EntryEditorPreferences entryEditorPreferences;
    private final MrDlibPreferences mrDlibPreferences;
    private final AbbreviationPreferences abbreviationPreferences;
    private final TaskExecutor taskExecutor;
    private boolean mscKeywordDescriptionsInitialized;

    public EntryEditorTabViewModel(DialogService dialogService, GuiPreferences preferences, TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.entryEditorPreferences = preferences.getEntryEditorPreferences();
        this.mrDlibPreferences = preferences.getMrDlibPreferences();
        this.abbreviationPreferences = preferences.getAbbreviationPreferences();
        this.taskExecutor = taskExecutor;

        selectedTab.addListener((_, oldItem, newItem) -> {
            commitCurrentEdit(oldItem);
            loadSelection(newItem);
        });

        EasyBind.subscribe(enableMscKeywordDescriptionsProperty, this::onMscKeywordDescriptionsChanged);
    }

    /// Commits any pending name/fields edit for {@code oldItem} back into {@link #tabConfigs}.
    private void commitCurrentEdit(EntryEditorTabModel oldItem) {
        if (!(oldItem instanceof EntryEditorTabModel.CustomizedFieldSet)) {
            return;
        }
        int index = tabConfigs.indexOf(oldItem);
        if (index < 0) {
            return;
        }
        tabConfigs.set(index, new EntryEditorTabModel.CustomizedFieldSet(
                selectedTabName.get(), new LinkedHashSet<>(selectedTabFields)));
    }

    private void loadSelection(EntryEditorTabModel newItem) {
        if (newItem instanceof EntryEditorTabModel.CustomizedFieldSet fieldSet) {
            selectedTabName.set(fieldSet.name());
            selectedTabFields.setAll(fieldSet.fields());
            fieldSetTabSelected.set(true);
            canRemoveSelectedTab.set(true);
        } else {
            selectedTabName.set("");
            selectedTabFields.clear();
            fieldSetTabSelected.set(false);
            canRemoveSelectedTab.set(false);
        }
    }

    @Override
    public void setValues() {
        // The Preview tab is configured via the "show preview as a separate tab" preference, not here,
        // so it is omitted from the configurable tab list (its model visibility bit is unused).
        tabConfigs.setAll(entryEditorPreferences.getTabModels().stream()
                                                .filter(model -> !model.isPreview())
                                                .toList());
        selectedTab.set(null);

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
        tabConfigs.removeIf(config -> config instanceof EntryEditorTabModel.CustomizedFieldSet);
        List<EntryEditorTabModel> defaultFieldSets = EntryEditorPreferences.getDefaultEntryEditorTabs()
                                                                           .entrySet().stream()
                                                                           .<EntryEditorTabModel>map(e -> new EntryEditorTabModel.CustomizedFieldSet(e.getKey(), e.getValue()))
                                                                           .toList();
        tabConfigs.addAll(EntryEditorTabModel.indexAfterBuiltInFieldSets(tabConfigs), defaultFieldSets);
        // selectedTab is cleared automatically when the ListView loses the old selected item
    }

    public void addFieldSetTab() {
        int insertIndex = EntryEditorTabModel.indexAfterBuiltInFieldSets(tabConfigs);
        for (int i = 0; i < tabConfigs.size(); i++) {
            if (tabConfigs.get(i) instanceof EntryEditorTabModel.CustomizedFieldSet) {
                insertIndex = i + 1;
            }
        }
        EntryEditorTabModel newTab = new EntryEditorTabModel.CustomizedFieldSet(
                Localization.lang("New Tab"), Set.of());
        tabConfigs.add(insertIndex, newTab);
        selectedTab.set(newTab); // commits old, loads new empty state; view mirrors via EasyBind
    }

    public void removeSelectedFieldSetTab() {
        if (selectedTab.get() instanceof EntryEditorTabModel.CustomizedFieldSet toRemove) {
            tabConfigs.remove(toRemove);
            // ListView auto-updates selection; view listener propagates it back to selectedTab
        }
    }

    /// Toggles the visibility of a feature or built-in field-set tab. Called by the cell's checkbox.
    public void toggleFeatureTabVisibility(EntryEditorTabModel config) {
        int index = tabConfigs.indexOf(config);
        if (index < 0) {
            return;
        }
        switch (config) {
            case EntryEditorTabModel.Feature feature ->
                    tabConfigs.set(index, new EntryEditorTabModel.Feature(feature.type(), !feature.visible()));
            case EntryEditorTabModel.FieldSet fieldSet ->
                    tabConfigs.set(index, new EntryEditorTabModel.FieldSet(fieldSet.type(), !fieldSet.visible()));
            case EntryEditorTabModel.CustomizedFieldSet ignored -> {
                // Custom tabs are always visible; toggled only by adding/removing them.
            }
        }
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

        // Write feature- and built-in field-set-tab visibility from working copy
        for (EntryEditorTabModel config : tabConfigs) {
            switch (config) {
                case EntryEditorTabModel.Feature feature ->
                        entryEditorPreferences.setStaticTabVisible(feature.type(), feature.visible());
                case EntryEditorTabModel.FieldSet fieldSet ->
                        entryEditorPreferences.setFieldSetVisible(fieldSet.type(), fieldSet.visible());
                case EntryEditorTabModel.CustomizedFieldSet ignored -> {
                    // handled below
                }
            }
        }

        // Write customized field-set tabs. The currently selected one may have pending edits that have
        // not yet been committed to tabConfigs (they only commit on navigation). Read them directly
        // from the staging properties so nothing is lost when the user clicks Apply without switching tabs.
        EntryEditorTabModel pendingItem = selectedTab.get();
        int pendingIndex = (pendingItem instanceof EntryEditorTabModel.CustomizedFieldSet)
                           ? tabConfigs.indexOf(pendingItem) : -1;

        Map<String, Set<Field>> fieldSetMap = new LinkedHashMap<>();
        for (int i = 0; i < tabConfigs.size(); i++) {
            if (tabConfigs.get(i) instanceof EntryEditorTabModel.CustomizedFieldSet fieldSet) {
                if (i == pendingIndex) {
                    fieldSetMap.put(selectedTabName.get(), new LinkedHashSet<>(selectedTabFields));
                } else {
                    fieldSetMap.put(fieldSet.name(), fieldSet.fields());
                }
            }
        }
        entryEditorPreferences.setEntryEditorTabList(fieldSetMap);
    }

    // region Properties

    public ObservableList<EntryEditorTabModel> getTabConfigs() {
        return tabConfigs;
    }

    public ObjectProperty<EntryEditorTabModel> selectedTabProperty() {
        return selectedTab;
    }

    public StringProperty selectedTabNameProperty() {
        return selectedTabName;
    }

    public ObservableList<Field> getSelectedTabFields() {
        return selectedTabFields;
    }

    public List<Field> getAllKnownFields() {
        return allKnownFields;
    }

    public BooleanProperty fieldSetTabSelectedProperty() {
        return fieldSetTabSelected;
    }

    public BooleanProperty canRemoveSelectedTabProperty() {
        return canRemoveSelectedTab;
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
