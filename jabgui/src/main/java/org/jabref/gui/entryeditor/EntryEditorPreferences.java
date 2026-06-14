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
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

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

    private final ObservableMap<String, Set<Field>> entryEditorTabList;
    private final ObservableSet<StaticTab> staticTabs;
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
                getDefaultStaticTabs(),
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

        this.entryEditorTabList = FXCollections.observableMap(new LinkedHashMap<>(entryEditorTabList));
        this.staticTabs = FXCollections.observableSet(EnumSet.copyOf(staticTabs.isEmpty() ? EnumSet.noneOf(StaticTab.class) : staticTabs));
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

    public static Set<StaticTab> getDefaultStaticTabs() {
        return EnumSet.allOf(StaticTab.class);
    }

    public ObservableSet<StaticTab> getStaticTabs() {
        return staticTabs;
    }

    public boolean isStaticTabVisible(StaticTab tab) {
        return staticTabs.contains(tab);
    }

    public void setStaticTabVisible(StaticTab tab, boolean show) {
        if (show) {
            staticTabs.add(tab);
        } else {
            staticTabs.remove(tab);
        }
    }

    public void setStaticTabs(Set<StaticTab> tabs) {
        staticTabs.clear();
        staticTabs.addAll(tabs);
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

    public void setAll(EntryEditorPreferences preferences) {
        setEntryEditorTabList(preferences.getEntryEditorTabs());
        this.shouldOpenOnNewEntry.set(preferences.shouldOpenOnNewEntry());
        setStaticTabs(preferences.getStaticTabs());
        this.showSourceTabByDefault.set(preferences.showSourceTabByDefault());
        this.enableValidation.set(preferences.shouldEnableValidation());
        this.allowIntegerEditionBibtex.set(preferences.shouldAllowIntegerEditionBibtex());
        this.autoLinkFiles.set(preferences.autoLinkFilesEnabled());
        this.enablementStatus.set(preferences.shouldEnableJournalPopup());
        this.citationFetcherType.set(preferences.getCitationFetcherType());
        this.citationCountFetcherType.set(preferences.getCitationCountFetcherType());
        this.previewWidthDividerPosition.set(preferences.getPreviewWidthDividerPosition());
    }

    public ObservableMap<String, Set<Field>> getEntryEditorTabs() {
        return entryEditorTabList;
    }

    public void setEntryEditorTabList(Map<String, Set<Field>> tabs) {
        this.entryEditorTabList.clear();
        this.entryEditorTabList.putAll(tabs);
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
