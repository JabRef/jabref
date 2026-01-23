package org.jabref.gui.entryeditor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

import static org.jabref.logic.preferences.JabRefCliPreferences.STRINGLIST_DELIMITER;

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

    private final MapProperty<String, Set<Field>> entryEditorTabList;
    private final BooleanProperty shouldOpenOnNewEntry;
    private final BooleanProperty shouldShowRecommendationsTab;
    private final BooleanProperty shouldShowAiSummaryTab;
    private final BooleanProperty shouldShowAiChatTab;
    private final BooleanProperty shouldShowLatexCitationsTab;
    private final BooleanProperty shouldShowFileAnnotationsTab;
    private final BooleanProperty showSourceTabByDefault;
    private final BooleanProperty enableValidation;
    private final BooleanProperty allowIntegerEditionBibtex;
    private final BooleanProperty autoLinkFiles;
    private final ObjectProperty<JournalPopupEnabled> enablementStatus;
    private final ObjectProperty<CitationFetcherType> citationFetcherType;
    private final BooleanProperty shouldShowSciteTab;
    private final BooleanProperty showUserCommentsFields;
    private final DoubleProperty previewWidthDividerPosition;

    private EntryEditorPreferences() {
        this(
                getDefaultEntryEditorTabs(), //Default Entry Editor Tabs
                true, //Open editor when a new entry is created
                true, //Show tab 'Related articles'
                true, //Show tab 'AI Summary'
                true, //Show tab 'AI Chat'
                true, //Show tab 'LaTeX citations'
                true, //Show tab 'File annotations' only if its contains highlights or comments
                true, //Show BibTeX source by default
                true, //Show validation messages
                true, //Allow integers in 'edition' filed in BibTeX mode
                true, //Automatically search and show unlinked files in the entry editor
                JournalPopupEnabled.DISABLED, //Fetch journal information online to show
                CitationFetcherType.SEMANTIC_SCHOLAR, //Citation Fetcher Type
                true, //Show tab 'Citation information'
                true, //Show user comments field
                0.5 //Preview Width Divider Position
        );
    }

    public EntryEditorPreferences(Map<String, Set<Field>> entryEditorTabList,
                                  boolean shouldOpenOnNewEntry,
                                  boolean shouldShowRecommendationsTab,
                                  boolean shouldShowAiSummaryTab,
                                  boolean shouldShowAiChatTab,
                                  boolean shouldShowLatexCitationsTab,
                                  boolean shouldShowFileAnnotationsTab,
                                  boolean showSourceTabByDefault,
                                  boolean enableValidation,
                                  boolean allowIntegerEditionBibtex,
                                  boolean autolinkFilesEnabled,
                                  JournalPopupEnabled journalPopupEnabled,
                                  CitationFetcherType citationFetcherType,
                                  boolean showSciteTab,
                                  boolean showUserCommentsFields,
                                  double previewWidthDividerPosition) {

        this.entryEditorTabList = new SimpleMapProperty<>(FXCollections.observableMap(entryEditorTabList));
        this.shouldOpenOnNewEntry = new SimpleBooleanProperty(shouldOpenOnNewEntry);
        this.shouldShowRecommendationsTab = new SimpleBooleanProperty(shouldShowRecommendationsTab);
        this.shouldShowAiSummaryTab = new SimpleBooleanProperty(shouldShowAiSummaryTab);
        this.shouldShowAiChatTab = new SimpleBooleanProperty(shouldShowAiChatTab);
        this.shouldShowLatexCitationsTab = new SimpleBooleanProperty(shouldShowLatexCitationsTab);
        this.shouldShowFileAnnotationsTab = new SimpleBooleanProperty(shouldShowFileAnnotationsTab);
        this.showSourceTabByDefault = new SimpleBooleanProperty(showSourceTabByDefault);
        this.enableValidation = new SimpleBooleanProperty(enableValidation);
        this.allowIntegerEditionBibtex = new SimpleBooleanProperty(allowIntegerEditionBibtex);
        this.autoLinkFiles = new SimpleBooleanProperty(autolinkFilesEnabled);
        this.enablementStatus = new SimpleObjectProperty<>(journalPopupEnabled);
        this.citationFetcherType = new SimpleObjectProperty<>(citationFetcherType);
        this.shouldShowSciteTab = new SimpleBooleanProperty(showSciteTab);
        this.showUserCommentsFields = new SimpleBooleanProperty(showUserCommentsFields);
        this.previewWidthDividerPosition = new SimpleDoubleProperty(previewWidthDividerPosition);
    }

    public static SequencedMap<String, Set<Field>> getDefaultEntryEditorTabs() {
        SequencedMap<String, Set<Field>> defaultTabsMap = new LinkedHashMap<>();
        String defaultFields = FieldFactory.getDefaultGeneralFields().stream().map(Field::getName).collect(Collectors.joining(STRINGLIST_DELIMITER.toString()));
        defaultTabsMap.put("General", FieldFactory.parseFieldList(defaultFields));
        defaultTabsMap.put("Abstract", FieldFactory.parseFieldList(StandardField.ABSTRACT.getName()));

        return defaultTabsMap;
    }

    public static EntryEditorPreferences getDefaultEntryEditorPreferences() {
        return new EntryEditorPreferences();
    }

    public void setAll(EntryEditorPreferences preferences) {
        this.entryEditorTabList.set(preferences.entryEditorTabs());
        this.shouldOpenOnNewEntry.set(preferences.shouldOpenOnNewEntry());
        this.shouldShowRecommendationsTab.set(preferences.shouldShowRecommendationsTab());
        this.shouldShowAiSummaryTab.set(preferences.shouldShowAiSummaryTab());
        this.shouldShowAiChatTab.set(preferences.shouldShowAiChatTab());
        this.shouldShowLatexCitationsTab.set(preferences.shouldShowLatexCitationsTab());
        this.shouldShowFileAnnotationsTab.set(preferences.shouldShowFileAnnotationsTab());
        this.showSourceTabByDefault.set(preferences.showSourceTabByDefault());
        this.enableValidation.set(preferences.shouldEnableValidation());
        this.allowIntegerEditionBibtex.set(preferences.shouldAllowIntegerEditionBibtex());
        this.autoLinkFiles.set(preferences.autoLinkFilesEnabled());
        this.enablementStatus.set(preferences.shouldEnableJournalPopup());
        this.citationFetcherType.set(preferences.getCitationFetcherType());
        this.shouldShowSciteTab.set(preferences.shouldShowSciteTab());
        this.showUserCommentsFields.set(preferences.shouldShowUserCommentsFields());
        this.previewWidthDividerPosition.set(preferences.getPreviewWidthDividerPosition());
    }

    public ObservableMap<String, Set<Field>> getEntryEditorTabs() {
        return entryEditorTabList.get();
    }

    public MapProperty<String, Set<Field>> entryEditorTabs() {
        return entryEditorTabList;
    }

    public void setEntryEditorTabList(Map<String, Set<Field>> entryEditorTabList) {
        this.entryEditorTabList.set(FXCollections.observableMap(entryEditorTabList));
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

    public boolean shouldShowRecommendationsTab() {
        return shouldShowRecommendationsTab.get();
    }

    public BooleanProperty shouldShowRecommendationsTabProperty() {
        return shouldShowRecommendationsTab;
    }

    public void setShouldShowRecommendationsTab(boolean shouldShowRecommendationsTab) {
        this.shouldShowRecommendationsTab.set(shouldShowRecommendationsTab);
    }

    public boolean shouldShowAiSummaryTab() {
        return shouldShowAiSummaryTab.get();
    }

    public BooleanProperty shouldShowAiSummaryTabProperty() {
        return shouldShowAiSummaryTab;
    }

    public void setShouldShowAiSummaryTab(boolean shouldShowAiSummaryTab) {
        this.shouldShowAiSummaryTab.set(shouldShowAiSummaryTab);
    }

    public boolean shouldShowAiChatTab() {
        return shouldShowAiChatTab.get();
    }

    public BooleanProperty shouldShowAiChatTabProperty() {
        return shouldShowAiChatTab;
    }

    public void setShouldShowAiChatTab(boolean shouldShowAiChatTab) {
        this.shouldShowAiChatTab.set(shouldShowAiChatTab);
    }

    public boolean shouldShowLatexCitationsTab() {
        return shouldShowLatexCitationsTab.get();
    }

    public BooleanProperty shouldShowLatexCitationsTabProperty() {
        return shouldShowLatexCitationsTab;
    }

    public void setShouldShowLatexCitationsTab(boolean shouldShowLatexCitationsTab) {
        this.shouldShowLatexCitationsTab.set(shouldShowLatexCitationsTab);
    }

    public boolean shouldShowFileAnnotationsTab() {
        return shouldShowFileAnnotationsTab.get();
    }

    public BooleanProperty shouldShowFileAnnotationsTabProperty() {
        return shouldShowFileAnnotationsTab;
    }

    public void setShouldShowFileAnnotationsTab(boolean shouldShowFileAnnotationsTab) {
        this.shouldShowFileAnnotationsTab.set(shouldShowFileAnnotationsTab);
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

    public boolean shouldShowSciteTab() {
        return this.shouldShowSciteTab.get();
    }

    public BooleanProperty shouldShowLSciteTabProperty() {
        return this.shouldShowSciteTab;
    }

    public void setShouldShowSciteTab(boolean shouldShowSciteTab) {
        this.shouldShowSciteTab.set(shouldShowSciteTab);
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
