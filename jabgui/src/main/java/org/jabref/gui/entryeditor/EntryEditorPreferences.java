package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
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

    public enum StaticTab {
        RELATED_ARTICLES,
        AI_SUMMARY,
        AI_CHAT,
        FILE_ANNOTATIONS,
        LATEX_CITATIONS,
        CITATION_INFORMATION,
        USER_COMMENTS
    }

    /// Ordered list of all configurable tabs. {@link EntryEditorTabModel.FieldSet} entries
    /// always precede {@link EntryEditorTabModel.Feature} entries.
    private final ObservableList<EntryEditorTabModel> tabConfigs;

    private final BooleanProperty shouldOpenOnNewEntry;
    private final BooleanProperty showSourceTabByDefault;
    private final BooleanProperty enableValidation;
    private final BooleanProperty allowIntegerEditionBibtex;
    private final BooleanProperty autoLinkFiles;
    private final ObjectProperty<JournalPopupEnabled> enablementStatus;
    private final ObjectProperty<CitationFetcherType> citationFetcherType;
    private final ObjectProperty<CitationCountFetcherType> citationCountFetcherType;
    private final DoubleProperty previewWidthDividerPosition;

    private EntryEditorPreferences() {
        this(
                getDefaultEntryEditorTabs(),
                true,
                EnumSet.allOf(StaticTab.class),
                false,
                true,
                true,
                true,
                JournalPopupEnabled.DISABLED,
                CitationFetcherType.SEMANTIC_SCHOLAR,
                CitationCountFetcherType.SEMANTIC_SCHOLAR,
                0.5
        );
    }

    public EntryEditorPreferences(Map<String, Set<Field>> entryEditorTabList,
                                  boolean shouldOpenOnNewEntry,
                                  Set<StaticTab> staticTabs,
                                  boolean showSourceTabByDefault,
                                  boolean enableValidation,
                                  boolean allowIntegerEditionBibtex,
                                  boolean autolinkFilesEnabled,
                                  JournalPopupEnabled journalPopupEnabled,
                                  CitationFetcherType citationFetcherType,
                                  CitationCountFetcherType citationCountFetcherType,
                                  double previewWidthDividerPosition) {
        this.tabConfigs = FXCollections.observableList(createEntryEditorTabModels(entryEditorTabList, staticTabs));
        this.shouldOpenOnNewEntry = new SimpleBooleanProperty(shouldOpenOnNewEntry);
        this.showSourceTabByDefault = new SimpleBooleanProperty(showSourceTabByDefault);
        this.enableValidation = new SimpleBooleanProperty(enableValidation);
        this.allowIntegerEditionBibtex = new SimpleBooleanProperty(allowIntegerEditionBibtex);
        this.autoLinkFiles = new SimpleBooleanProperty(autolinkFilesEnabled);
        this.enablementStatus = new SimpleObjectProperty<>(journalPopupEnabled);
        this.citationFetcherType = new SimpleObjectProperty<>(citationFetcherType);
        this.citationCountFetcherType = new SimpleObjectProperty<>(citationCountFetcherType);
        this.previewWidthDividerPosition = new SimpleDoubleProperty(previewWidthDividerPosition);
    }

    private static List<EntryEditorTabModel> createEntryEditorTabModels(Map<String, Set<Field>> entryEditorTabList,
                                                                          Set<StaticTab> staticTabs) {
        List<EntryEditorTabModel> configs = new ArrayList<>();

        entryEditorTabList.forEach((name, fields) ->
                configs.add(new EntryEditorTabModel.FieldSet(name, fields, true)));

        for (StaticTab tab : StaticTab.values()) {
            configs.add(new EntryEditorTabModel.Feature(tab, staticTabs.contains(tab)));
        }
        return configs;
    }

    public ObservableList<EntryEditorTabModel> getTabConfigs() {
        return tabConfigs;
    }

    public boolean isStaticTabVisible(StaticTab tab) {
        for (EntryEditorTabModel config : tabConfigs) {
            if (config instanceof EntryEditorTabModel.Feature(
                    StaticTab type,
                    boolean visible
            ) && type == tab) {
                return visible;
            }
        }
        return false;
    }

    public void setStaticTabVisible(StaticTab tab, boolean show) {
        for (int i = 0; i < tabConfigs.size(); i++) {
            if (tabConfigs.get(i) instanceof EntryEditorTabModel.Feature feature && feature.type() == tab) {
                tabConfigs.set(i, new EntryEditorTabModel.Feature(tab, show));
                return;
            }
        }
    }

    /// Returns a snapshot map of field-set tabs (name → fields). Changes to this map are not reflected
    /// in the preferences; use {@link #setEntryEditorTabList} to persist modifications.
    public Map<String, Set<Field>> getEntryEditorTabs() {
        SequencedMap<String, Set<Field>> map = new LinkedHashMap<>();
        for (EntryEditorTabModel config : tabConfigs) {
            if (config instanceof EntryEditorTabModel.FieldSet fieldSet) {
                map.put(fieldSet.name(), fieldSet.fields());
            }
        }
        return map;
    }

    public void setEntryEditorTabList(Map<String, Set<Field>> tabs) {
        List<EntryEditorTabModel> newFieldSet = tabs.entrySet().stream()
                                                     .<EntryEditorTabModel>map(e ->
                                                             new EntryEditorTabModel.FieldSet(e.getKey(), e.getValue(), true))
                                                     .toList();
        tabConfigs.removeIf(config -> config instanceof EntryEditorTabModel.FieldSet);
        tabConfigs.addAll(0, newFieldSet);
    }

    public static Set<StaticTab> staticTabsFromBoolean(
            boolean showRelatedArticles,
            boolean showAISummary,
            boolean showAIChat,
            boolean showLaTeXCitations,
            boolean showFileAnnotations,
            boolean showCitationInformation,
            boolean showUserComments) {
        Set<StaticTab> tabs = EnumSet.noneOf(StaticTab.class);
        if (showRelatedArticles) {
            tabs.add(StaticTab.RELATED_ARTICLES);
        }
        if (showAISummary) {
            tabs.add(StaticTab.AI_SUMMARY);
        }
        if (showAIChat) {
            tabs.add(StaticTab.AI_CHAT);
        }
        if (showLaTeXCitations) {
            tabs.add(StaticTab.LATEX_CITATIONS);
        }
        if (showFileAnnotations) {
            tabs.add(StaticTab.FILE_ANNOTATIONS);
        }
        if (showCitationInformation) {
            tabs.add(StaticTab.CITATION_INFORMATION);
        }
        if (showUserComments) {
            tabs.add(StaticTab.USER_COMMENTS);
        }
        return tabs;
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

    // endregion

    public void setAll(EntryEditorPreferences preferences) {
        tabConfigs.setAll(preferences.getTabConfigs());
        this.shouldOpenOnNewEntry.set(preferences.shouldOpenOnNewEntry());
        this.showSourceTabByDefault.set(preferences.showSourceTabByDefault());
        this.enableValidation.set(preferences.shouldEnableValidation());
        this.allowIntegerEditionBibtex.set(preferences.shouldAllowIntegerEditionBibtex());
        this.autoLinkFiles.set(preferences.autoLinkFilesEnabled());
        this.enablementStatus.set(preferences.shouldEnableJournalPopup());
        this.citationFetcherType.set(preferences.getCitationFetcherType());
        this.citationCountFetcherType.set(preferences.getCitationCountFetcherType());
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
