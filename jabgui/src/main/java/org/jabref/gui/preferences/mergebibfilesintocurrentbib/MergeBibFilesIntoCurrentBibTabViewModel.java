package org.jabref.gui.preferences.mergebibfilesintocurrentbib;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.mergebibfilesintocurrentbib.MergeBibFilesIntoCurrentBibPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;

public class MergeBibFilesIntoCurrentBibTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty mergeSameKeyEntriesProperty = new SimpleBooleanProperty();
    private final BooleanProperty mergeDuplicateEntriesProperty = new SimpleBooleanProperty();

    private final MergeBibFilesIntoCurrentBibPreferences mergeBibFilesIntoCurrentBibPreferences;

    public MergeBibFilesIntoCurrentBibTabViewModel(GuiPreferences preferences) {
        this.mergeBibFilesIntoCurrentBibPreferences = preferences.getMergeBibFilesIntoCurrentBibPreferences();
    }

    @Override
    public void setValues() {
        mergeSameKeyEntriesProperty.setValue(mergeBibFilesIntoCurrentBibPreferences.getShouldMergeSameKeyEntries());
        mergeDuplicateEntriesProperty.setValue(mergeBibFilesIntoCurrentBibPreferences.getShouldMergeDuplicateEntries());
    }

    @Override
    public void storeSettings() {
        mergeBibFilesIntoCurrentBibPreferences.setShouldMergeSameKeyEntries(mergeSameKeyEntriesProperty.getValue());
        mergeBibFilesIntoCurrentBibPreferences.setShouldMergeDuplicateEntries(mergeDuplicateEntriesProperty.getValue());
    }

    public BooleanProperty mergeSameKeyEntriesProperty() {
        return this.mergeSameKeyEntriesProperty;
    }

    public BooleanProperty mergeDuplicateEntriesProperty() {
        return this.mergeDuplicateEntriesProperty;
    }
}
