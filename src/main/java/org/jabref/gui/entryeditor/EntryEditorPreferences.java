package org.jabref.gui.entryeditor;

import java.util.Map;
import java.util.Set;

import org.jabref.model.entry.field.Field;

public class EntryEditorPreferences {

    private final Map<String, Set<Field>> entryEditorTabList;
    private boolean shouldOpenOnNewEntry;
    private final boolean shouldShowRecommendationsTab;
    private final boolean isMrdlibAccepted;
    private final boolean shouldShowLatexCitationsTab;
    private boolean showSourceTabByDefault;
    private boolean enableValidation;
    private double dividerPosition;

    public EntryEditorPreferences(Map<String, Set<Field>> entryEditorTabList,
                                  boolean shouldOpenOnNewEntry,
                                  boolean shouldShowRecommendationsTab,
                                  boolean isMrdlibAccepted,
                                  boolean shouldShowLatexCitationsTab,
                                  boolean showSourceTabByDefault,
                                  boolean enableValidation,
                                  double dividerPosition) {

        this.entryEditorTabList = entryEditorTabList;
        this.shouldOpenOnNewEntry = shouldOpenOnNewEntry;
        this.shouldShowRecommendationsTab = shouldShowRecommendationsTab;
        this.isMrdlibAccepted = isMrdlibAccepted;
        this.shouldShowLatexCitationsTab = shouldShowLatexCitationsTab;
        this.showSourceTabByDefault = showSourceTabByDefault;
        this.enableValidation = enableValidation;
        this.dividerPosition = dividerPosition;
    }

    public Map<String, Set<Field>> getEntryEditorTabList() {
        return entryEditorTabList;
    }

    public boolean shouldOpenOnNewEntry() {
        return shouldOpenOnNewEntry;
    }

    public boolean shouldShowRecommendationsTab() {
        return shouldShowRecommendationsTab;
    }

    public boolean isMrdlibAccepted() {
        return isMrdlibAccepted;
    }

    public boolean showSourceTabByDefault() {
        return showSourceTabByDefault;
    }

    public boolean shouldShowLatexCitationsTab() {
        return shouldShowLatexCitationsTab;
    }

    public boolean isEnableValidation() {
        return enableValidation;
    }

    public double getDividerPosition() {
        return dividerPosition;
    }

    public EntryEditorPreferences withDividerPosition(double dividerPosition) {
        this.dividerPosition = dividerPosition;
        return this;
    }
}
