package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.importer.fetcher.citation.CitationCountFetcherType;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;

public class EntryEditorPreferences {

    /// Specifies the different possible enablement states for online services
    public enum JournalPopupEnabled {
        FIRST_START, // The first time a user uses this service
        ENABLED,
        DISABLED;

        public static JournalPopupEnabled fromString(String status) {
            for (JournalPopupEnabled value : JournalPopupEnabled.values()) {
                if (value.toString().equalsIgnoreCase(status)) {
                    return value;
                }
            }
            throw new IllegalArgumentException("No enum found with value: " + status);
        }
    }

    /// Ordered list of all configurable tabs. Built-in {@linkplain EntryEditorTabModel.BuiltIn#isFieldSet()
    /// field-set} tabs always precede the feature tabs.
    private final ObservableList<EntryEditorTabModel> tabModels;

    private final BooleanProperty shouldOpenOnNewEntry;
    private final BooleanProperty showSourceTabByDefault;
    private final BooleanProperty enableValidation;
    private final BooleanProperty allowIntegerEditionBibtex;
    private final BooleanProperty autoLinkFiles;
    private final ObjectProperty<JournalPopupEnabled> enablementStatus;
    private final ObjectProperty<CitationFetcherType> citationFetcherType;
    private final ObjectProperty<CitationCountFetcherType> citationCountFetcherType;
    private final DoubleProperty previewWidthDividerPosition;

    /// Field-level toggle: whether the user-specific comments field is shown inside the Comments tab.
    /// Distinct from the Comments tab's own visibility ({@link EntryEditorTabModel.BuiltIn#COMMENTS}).
    private final BooleanProperty showUserCommentsFields;

    private EntryEditorPreferences() {
        this(
                getDefaultTabModels(),                     // Default Entry Editor Tabs
                true,                                      // Open editor when a new entry is
                false,                                     // Show BibTeX source by default
                true,                                      // Show validation messages
                true,                                      // Allow integers in 'edition' filed in BibTeX mode
                true,                                      // Automatically search and show unlinked files in the entry editor
                JournalPopupEnabled.DISABLED,              // Fetch journal information online to show
                CitationFetcherType.SEMANTIC_SCHOLAR,      // Citation Fetcher Type
                CitationCountFetcherType.SEMANTIC_SCHOLAR, // Citation Count Fetcher Type
                true,                                      // Show user comments field
                0.5                                        // Preview Width Divider Position
        );
    }

    public EntryEditorPreferences(List<EntryEditorTabModel> tabModels,
                                  boolean shouldOpenOnNewEntry,
                                  boolean showSourceTabByDefault,
                                  boolean enableValidation,
                                  boolean allowIntegerEditionBibtex,
                                  boolean autolinkFilesEnabled,
                                  JournalPopupEnabled journalPopupEnabled,
                                  CitationFetcherType citationFetcherType,
                                  CitationCountFetcherType citationCountFetcherType,
                                  boolean showUserCommentsFields,
                                  double previewWidthDividerPosition) {
        this.tabModels = FXCollections.observableArrayList(tabModels);
        this.shouldOpenOnNewEntry = new SimpleBooleanProperty(shouldOpenOnNewEntry);
        this.showSourceTabByDefault = new SimpleBooleanProperty(showSourceTabByDefault);
        this.enableValidation = new SimpleBooleanProperty(enableValidation);
        this.allowIntegerEditionBibtex = new SimpleBooleanProperty(allowIntegerEditionBibtex);
        this.autoLinkFiles = new SimpleBooleanProperty(autolinkFilesEnabled);
        this.enablementStatus = new SimpleObjectProperty<>(journalPopupEnabled);
        this.citationFetcherType = new SimpleObjectProperty<>(citationFetcherType);
        this.citationCountFetcherType = new SimpleObjectProperty<>(citationCountFetcherType);
        this.showUserCommentsFields = new SimpleBooleanProperty(showUserCommentsFields);
        this.previewWidthDividerPosition = new SimpleDoubleProperty(previewWidthDividerPosition);
    }

    private static List<EntryEditorTabModel> getDefaultTabModels() {
        List<EntryEditorTabModel> tabModels = new ArrayList<>();

        // Always-present leading tab
        tabModels.add(new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.PREVIEW, true));

        Arrays.stream(EntryEditorTabModel.BuiltIn.values())
              .filter(EntryEditorTabModel.BuiltIn::isFieldSet)
              .forEachOrdered(fieldSet -> tabModels.add(new EntryEditorTabModel.BuiltInTab(fieldSet, true)));

        getDefaultEntryEditorTabs().forEach((name, fields) ->
                tabModels.add(new EntryEditorTabModel.CustomizedFieldSet(name, fields)));

        // The leading Preview and the field-set tabs are already added above
        Arrays.stream(EntryEditorTabModel.BuiltIn.values())
              .filter(tab -> tab != EntryEditorTabModel.BuiltIn.PREVIEW && !tab.isFieldSet())
              .forEachOrdered(tab -> tabModels.add(new EntryEditorTabModel.BuiltInTab(tab, true)));

        return tabModels;
    }

    public ObservableList<EntryEditorTabModel> getTabModels() {
        return tabModels;
    }

    public boolean isTabVisible(EntryEditorTabModel.BuiltIn key) {
        return tabModels.stream()
                        .filter(model -> model.key().filter(key::equals).isPresent())
                        .findFirst()
                        .map(EntryEditorTabModel::isVisible)
                        .orElse(false);
    }

    public ObservableValue<Boolean> tabVisibleProperty(EntryEditorTabModel.BuiltIn key) {
        return Bindings.createBooleanBinding(() -> isTabVisible(key), tabModels);
    }

    public void setTabVisible(EntryEditorTabModel.BuiltIn key, boolean show) {
        for (int i = 0; i < tabModels.size(); i++) {
            if (tabModels.get(i).key().filter(key::equals).isPresent()) {
                tabModels.set(i, tabModels.get(i).withVisible(show));
                return;
            }
        }
    }

    /// Returns a snapshot map of customized field-set tabs (name → fields). Changes to this map are not
    /// reflected in the preferences; use {@link #setEntryEditorTabList} to persist modifications.
    public Map<String, Set<Field>> getEntryEditorTabs() {
        SequencedMap<String, Set<Field>> map = new LinkedHashMap<>();
        for (EntryEditorTabModel config : tabModels) {
            if (config instanceof EntryEditorTabModel.CustomizedFieldSet fieldSet) {
                map.put(fieldSet.name(), fieldSet.fields());
            }
        }
        return map;
    }

    public void setEntryEditorTabList(Map<String, Set<Field>> tabs) {
        List<EntryEditorTabModel> newFieldSet = tabs.entrySet().stream()
                                                    .<EntryEditorTabModel>map(model ->
                                                            new EntryEditorTabModel.CustomizedFieldSet(model.getKey(), model.getValue()))
                                                    .toList();
        tabModels.removeIf(config -> config instanceof EntryEditorTabModel.CustomizedFieldSet);
        // Customized field-set tabs sit right after the built-in field-set tabs — same position the persisted
        // list uses, so saving does not reorder the tabs.
        tabModels.addAll(EntryEditorTabModel.indexAfterBuiltInFieldSets(tabModels), newFieldSet);
    }

    public static SequencedMap<String, Set<Field>> getDefaultEntryEditorTabs() {
        SequencedMap<String, Set<Field>> defaultTabsMap = new LinkedHashMap<>();
        String defaultFields = getDefaultGeneralFields().stream()
                                                        .map(Field::getName)
                                                        .collect(Collectors.joining(JabRefCliPreferences.STRINGLIST_DELIMITER.toString()));
        defaultTabsMap.put(Localization.lang("General"), FieldFactory.parseFieldList(defaultFields));
        defaultTabsMap.put(Localization.lang("Abstract"), FieldFactory.parseFieldList(StandardField.ABSTRACT.getName()));

        return defaultTabsMap;
    }

    public static List<Field> getDefaultGeneralFields() {
        List<Field> defaultGeneralFields = new ArrayList<>(List.of(
                StandardField.DOI,
                StandardField.ICORERANKING,
                StandardField.CITATIONCOUNT,
                StandardField.CROSSREF,
                StandardField.KEYWORDS,
                StandardField.EPRINT,
                StandardField.EPRINTTYPE,
                StandardField.URL,
                StandardField.FILE,
                StandardField.GROUPS,
                StandardField.OWNER,
                StandardField.TIMESTAMP
        ));
        defaultGeneralFields.addAll(EnumSet.allOf(SpecialField.class));
        return defaultGeneralFields;
    }

    public static EntryEditorPreferences getDefault() {
        return new EntryEditorPreferences();
    }

    public void setAll(EntryEditorPreferences preferences) {
        tabModels.setAll(preferences.getTabModels());
        this.shouldOpenOnNewEntry.set(preferences.shouldOpenOnNewEntry());
        this.showSourceTabByDefault.set(preferences.showSourceTabByDefault());
        this.enableValidation.set(preferences.shouldEnableValidation());
        this.allowIntegerEditionBibtex.set(preferences.shouldAllowIntegerEditionBibtex());
        this.autoLinkFiles.set(preferences.autoLinkFilesEnabled());
        this.enablementStatus.set(preferences.shouldEnableJournalPopup());
        this.citationFetcherType.set(preferences.getCitationFetcherType());
        this.citationCountFetcherType.set(preferences.getCitationCountFetcherType());
        this.showUserCommentsFields.set(preferences.shouldShowUserCommentsFields());
        this.previewWidthDividerPosition.set(preferences.getPreviewWidthDividerPosition());
    }

    public boolean shouldOpenOnNewEntry() {
        return shouldOpenOnNewEntry.get();
    }

    public BooleanProperty shouldOpenOnNewEntryProperty() {
        return shouldOpenOnNewEntry;
    }

    public void setShouldOpenOnNewEntry(boolean shouldOpenOnNewEntry) {
        this.shouldOpenOnNewEntry.set(shouldOpenOnNewEntry);
    }

    public boolean showSourceTabByDefault() {
        return showSourceTabByDefault.get();
    }

    public BooleanProperty showSourceTabByDefaultProperty() {
        return showSourceTabByDefault;
    }

    public void setShowSourceTabByDefault(boolean showSourceTabByDefault) {
        this.showSourceTabByDefault.set(showSourceTabByDefault);
    }

    public boolean shouldEnableValidation() {
        return enableValidation.get();
    }

    public BooleanProperty enableValidationProperty() {
        return enableValidation;
    }

    public void setEnableValidation(boolean enableValidation) {
        this.enableValidation.set(enableValidation);
    }

    public boolean shouldAllowIntegerEditionBibtex() {
        return allowIntegerEditionBibtex.get();
    }

    public BooleanProperty allowIntegerEditionBibtexProperty() {
        return allowIntegerEditionBibtex;
    }

    public void setAllowIntegerEditionBibtex(boolean allowIntegerEditionBibtex) {
        this.allowIntegerEditionBibtex.set(allowIntegerEditionBibtex);
    }

    public boolean autoLinkFilesEnabled() {
        return this.autoLinkFiles.getValue();
    }

    public BooleanProperty autoLinkEnabledProperty() {
        return this.autoLinkFiles;
    }

    public void setAutoLinkFilesEnabled(boolean enabled) {
        this.autoLinkFiles.setValue(enabled);
    }

    public JournalPopupEnabled shouldEnableJournalPopup() {
        return enablementStatus.get();
    }

    public ObjectProperty<JournalPopupEnabled> enableJournalPopupProperty() {
        return enablementStatus;
    }

    public void setEnableJournalPopup(JournalPopupEnabled journalPopupEnabled) {
        this.enablementStatus.set(journalPopupEnabled);
    }

    public CitationFetcherType getCitationFetcherType() {
        return citationFetcherType.get();
    }

    public void setCitationFetcherType(CitationFetcherType citationFetcherType) {
        this.citationFetcherType.set(citationFetcherType);
    }

    public ObjectProperty<CitationFetcherType> citationFetcherTypeProperty() {
        return citationFetcherType;
    }

    public CitationCountFetcherType getCitationCountFetcherType() {
        return citationCountFetcherType.get();
    }

    public void setCitationCountFetcherType(CitationCountFetcherType citationCountFetcherType) {
        this.citationCountFetcherType.set(citationCountFetcherType);
    }

    public ObjectProperty<CitationCountFetcherType> citationCountFetcherTypeProperty() {
        return citationCountFetcherType;
    }

    public boolean shouldShowUserCommentsFields() {
        return showUserCommentsFields.get();
    }

    public BooleanProperty showUserCommentsFieldsProperty() {
        return showUserCommentsFields;
    }

    public void setShowUserCommentsFields(boolean showUserCommentsFields) {
        this.showUserCommentsFields.set(showUserCommentsFields);
    }

    public void setPreviewWidthDividerPosition(double previewWidthDividerPosition) {
        this.previewWidthDividerPosition.set(previewWidthDividerPosition);
    }

    /// Holds the horizontal divider position when the Preview is shown in the entry editor
    public DoubleProperty previewWidthDividerPositionProperty() {
        return previewWidthDividerPosition;
    }

    public Double getPreviewWidthDividerPosition() {
        return previewWidthDividerPosition.get();
    }
}
