package org.jabref.gui.welcome.quicksettings.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.preferences.GuiPreferences;

public class LargeLibraryOptimizationDialogViewModel extends AbstractViewModel {
    private final BooleanProperty disableFulltextIndexingProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty disableCreationDateProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty disableModificationDateProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty disableAutosaveProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty disableGroupCountProperty = new SimpleBooleanProperty(true);

    private final GuiPreferences preferences;

    public LargeLibraryOptimizationDialogViewModel(GuiPreferences preferences) {
        this.preferences = preferences;
    }

    public BooleanProperty disableFulltextIndexingProperty() {
        return disableFulltextIndexingProperty;
    }

    public boolean isDisableFulltextIndexing() {
        return disableFulltextIndexingProperty.get();
    }

    public BooleanProperty disableCreationDateProperty() {
        return disableCreationDateProperty;
    }

    public boolean isDisableCreationDate() {
        return disableCreationDateProperty.get();
    }

    public BooleanProperty disableModificationDateProperty() {
        return disableModificationDateProperty;
    }

    public boolean isDisableModificationDate() {
        return disableModificationDateProperty.get();
    }

    public BooleanProperty disableAutosaveProperty() {
        return disableAutosaveProperty;
    }

    public boolean isDisableAutosave() {
        return disableAutosaveProperty.get();
    }

    public BooleanProperty disableGroupCountProperty() {
        return disableGroupCountProperty;
    }

    public boolean isDisableGroupCount() {
        return disableGroupCountProperty.get();
    }

    public void saveSettings() {
        if (isDisableFulltextIndexing()) {
            preferences.getFilePreferences().setFulltextIndexLinkedFiles(false);
        }
        if (isDisableCreationDate()) {
            preferences.getTimestampPreferences().setAddCreationDate(false);
        }
        if (isDisableModificationDate()) {
            preferences.getTimestampPreferences().setAddModificationDate(false);
        }
        if (isDisableAutosave()) {
            preferences.getLibraryPreferences().setAutoSave(false);
        }
        if (isDisableGroupCount()) {
            preferences.getGroupsPreferences().setDisplayGroupCount(false);
        }
    }
}
