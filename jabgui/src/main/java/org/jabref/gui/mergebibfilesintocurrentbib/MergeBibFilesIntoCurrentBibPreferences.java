package org.jabref.gui.mergebibfilesintocurrentbib;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class MergeBibFilesIntoCurrentBibPreferences {
    private final BooleanProperty shouldMergeSameKeyEntries = new SimpleBooleanProperty();
    private final BooleanProperty shouldMergeDuplicateEntries = new SimpleBooleanProperty();

    public MergeBibFilesIntoCurrentBibPreferences(boolean shouldMergeSameKeyEntries, boolean shouldMergeDuplicateEntries) {
        this.shouldMergeSameKeyEntries.set(shouldMergeSameKeyEntries);
        this.shouldMergeDuplicateEntries.set(shouldMergeDuplicateEntries);
    }

    public boolean shouldMergeSameKeyEntries() {
        return this.shouldMergeSameKeyEntries.get();
    }

    public void setShouldMergeSameKeyEntries(boolean decision) {
        this.shouldMergeSameKeyEntries.set(decision);
    }

    public BooleanProperty shouldMergeSameKeyEntriesProperty() {
        return this.shouldMergeSameKeyEntries;
    }

    public boolean shouldMergeDuplicateEntries() {
        return this.shouldMergeDuplicateEntries.get();
    }

    public void setShouldMergeDuplicateEntries(boolean decision) {
        this.shouldMergeDuplicateEntries.set(decision);
    }

    public BooleanProperty shouldMergeDuplicateEntriesProperty() {
        return this.shouldMergeDuplicateEntries;
    }
}
