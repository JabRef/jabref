package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        return Arrays.stream(EntryEditorTabModel.BuiltIn.values())
                     .<EntryEditorTabModel>map(tab -> new EntryEditorTabModel.BuiltInTab(tab, true))
                     .collect(Collectors.toCollection(ArrayList::new));
    }

    public static EntryEditorPreferences getDefault() {
        return new EntryEditorPreferences();
    }

    public ObservableList<EntryEditorTabModel> getTabModels() {
        return tabModels;
    }

    public boolean isTabVisible(EntryEditorTabModel.BuiltIn key) {
        return tabModels.stream()
                        .anyMatch(model -> model instanceof EntryEditorTabModel.BuiltInTab(
                                EntryEditorTabModel.BuiltIn type,
                                boolean visible
                        ) && type == key && visible);
    }

    public ObservableValue<Boolean> tabVisibleProperty(EntryEditorTabModel.BuiltIn tabModel) {
        return Bindings.createBooleanBinding(() -> isTabVisible(tabModel), tabModels);
    }

    public void setTabVisible(EntryEditorTabModel.BuiltIn key, boolean show) {
        for (int i = 0; i < tabModels.size(); i++) {
            if (tabModels.get(i) instanceof EntryEditorTabModel.BuiltInTab(
                    EntryEditorTabModel.BuiltIn type,
                    boolean _
            ) && type == key) {
                tabModels.set(i, tabModels.get(i).withVisible(show));
                return;
            }
        }
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

    public ObjectProperty<CitationFetcherType> citationFetcherTypeProperty() {
        return citationFetcherType;
    }

    public void setCitationFetcherType(CitationFetcherType citationFetcherType) {
        this.citationFetcherType.set(citationFetcherType);
    }

    public CitationCountFetcherType getCitationCountFetcherType() {
        return citationCountFetcherType.get();
    }

    public ObjectProperty<CitationCountFetcherType> citationCountFetcherTypeProperty() {
        return citationCountFetcherType;
    }

    public void setCitationCountFetcherType(CitationCountFetcherType citationCountFetcherType) {
        this.citationCountFetcherType.set(citationCountFetcherType);
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

    public Double getPreviewWidthDividerPosition() {
        return previewWidthDividerPosition.get();
    }

    /// Holds the horizontal divider position when the Preview is shown in the entry editor
    public DoubleProperty previewWidthDividerPositionProperty() {
        return previewWidthDividerPosition;
    }

    public void setPreviewWidthDividerPosition(double previewWidthDividerPosition) {
        this.previewWidthDividerPosition.set(previewWidthDividerPosition);
    }
}
