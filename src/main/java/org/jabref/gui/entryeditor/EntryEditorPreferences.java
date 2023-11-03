package org.jabref.gui.entryeditor;

import java.util.Map;
import java.util.Set;

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

import org.jabref.model.entry.field.Field;

public class EntryEditorPreferences {

    /**
     * Specifies the different possible enablement states for online services
     */
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
    private final MapProperty<String, Set<Field>> defaultEntryEditorTabList;
    private final BooleanProperty shouldOpenOnNewEntry;
    private final BooleanProperty shouldShowRecommendationsTab;
    private final BooleanProperty shouldShowLatexCitationsTab;
    private final BooleanProperty showSourceTabByDefault;
    private final BooleanProperty enableValidation;
    private final BooleanProperty allowIntegerEditionBibtex;
    private final DoubleProperty dividerPosition;
    private final BooleanProperty autoLinkFiles;
    private final ObjectProperty<JournalPopupEnabled> enablementStatus;
    private final BooleanProperty shouldShowSciteTab;

    public EntryEditorPreferences(Map<String, Set<Field>> entryEditorTabList,
                                  Map<String, Set<Field>> defaultEntryEditorTabList,
                                  boolean shouldOpenOnNewEntry,
                                  boolean shouldShowRecommendationsTab,
                                  boolean shouldShowLatexCitationsTab,
                                  boolean showSourceTabByDefault,
                                  boolean enableValidation,
                                  boolean allowIntegerEditionBibtex,
                                  double dividerPosition,
                                  boolean autolinkFilesEnabled,
                                  JournalPopupEnabled journalPopupEnabled,
                                  boolean showSciteTab) {

        this.entryEditorTabList = new SimpleMapProperty<>(FXCollections.observableMap(entryEditorTabList));
        this.defaultEntryEditorTabList = new SimpleMapProperty<>(FXCollections.observableMap(defaultEntryEditorTabList));
        this.shouldOpenOnNewEntry = new SimpleBooleanProperty(shouldOpenOnNewEntry);
        this.shouldShowRecommendationsTab = new SimpleBooleanProperty(shouldShowRecommendationsTab);
        this.shouldShowLatexCitationsTab = new SimpleBooleanProperty(shouldShowLatexCitationsTab);
        this.showSourceTabByDefault = new SimpleBooleanProperty(showSourceTabByDefault);
        this.enableValidation = new SimpleBooleanProperty(enableValidation);
        this.allowIntegerEditionBibtex = new SimpleBooleanProperty(allowIntegerEditionBibtex);
        this.dividerPosition = new SimpleDoubleProperty(dividerPosition);
        this.autoLinkFiles = new SimpleBooleanProperty(autolinkFilesEnabled);
        this.enablementStatus = new SimpleObjectProperty<>(journalPopupEnabled);
        this.shouldShowSciteTab = new SimpleBooleanProperty(showSciteTab);
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

    public ObservableMap<String, Set<Field>> getDefaultEntryEditorTabs() {
        return defaultEntryEditorTabList.get();
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

    public boolean shouldShowLatexCitationsTab() {
        return shouldShowLatexCitationsTab.get();
    }

    public BooleanProperty shouldShowLatexCitationsTabProperty() {
        return shouldShowLatexCitationsTab;
    }

    public void setShouldShowLatexCitationsTab(boolean shouldShowLatexCitationsTab) {
        this.shouldShowLatexCitationsTab.set(shouldShowLatexCitationsTab);
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

    public double getDividerPosition() {
        return dividerPosition.get();
    }

    public DoubleProperty dividerPositionProperty() {
        return dividerPosition;
    }

    public void setDividerPosition(double dividerPosition) {
        this.dividerPosition.set(dividerPosition);
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

    public boolean shouldShowSciteTab() {
        return this.shouldShowSciteTab.get();
    }

    public BooleanProperty shouldShowLSciteTabProperty() {
        return this.shouldShowSciteTab;
    }

    public void setShouldShowSciteTab(boolean shouldShowSciteTab) {
        this.shouldShowSciteTab.set(shouldShowSciteTab);
    }
}
