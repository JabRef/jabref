package org.jabref.gui.entryeditor;

import java.util.Map;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.jabref.model.entry.field.Field;

public class EntryEditorPreferences {

    private final MapProperty<String, Set<Field>> entryEditorTabList;
    private final BooleanProperty shouldOpenOnNewEntry;
    private final BooleanProperty shouldShowRecommendationsTab;
    private final BooleanProperty isMrdlibAccepted;
    private final BooleanProperty shouldShowLatexCitationsTab;
    private final BooleanProperty showSourceTabByDefault;
    private final BooleanProperty enableValidation;
    private final BooleanProperty allowIntegerEditionBibtex;
    private final DoubleProperty dividerPosition;

    public EntryEditorPreferences(Map<String, Set<Field>> entryEditorTabList,
                                  boolean shouldOpenOnNewEntry,
                                  boolean shouldShowRecommendationsTab,
                                  boolean isMrdlibAccepted,
                                  boolean shouldShowLatexCitationsTab,
                                  boolean showSourceTabByDefault,
                                  boolean enableValidation,
                                  boolean allowIntegerEditionBibtex,
                                  double dividerPosition) {

        this.entryEditorTabList = new SimpleMapProperty<>(FXCollections.observableMap(entryEditorTabList));
        this.shouldOpenOnNewEntry = new SimpleBooleanProperty(shouldOpenOnNewEntry);
        this.shouldShowRecommendationsTab = new SimpleBooleanProperty(shouldShowRecommendationsTab);
        this.isMrdlibAccepted = new SimpleBooleanProperty(isMrdlibAccepted);
        this.shouldShowLatexCitationsTab = new SimpleBooleanProperty(shouldShowLatexCitationsTab);
        this.showSourceTabByDefault = new SimpleBooleanProperty(showSourceTabByDefault);
        this.enableValidation = new SimpleBooleanProperty(enableValidation);
        this.allowIntegerEditionBibtex = new SimpleBooleanProperty(allowIntegerEditionBibtex);
        this.dividerPosition = new SimpleDoubleProperty(dividerPosition);
    }

    public ObservableMap<String, Set<Field>> getEntryEditorTabList() {
        return entryEditorTabList.get();
    }

    public MapProperty<String, Set<Field>> entryEditorTabListProperty() {
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

    public boolean isMrdlibAccepted() {
        return isMrdlibAccepted.get();
    }

    public BooleanProperty isMrdlibAcceptedProperty() {
        return isMrdlibAccepted;
    }

    public void setIsMrdlibAccepted(boolean isMrdlibAccepted) {
        this.isMrdlibAccepted.set(isMrdlibAccepted);
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
}
