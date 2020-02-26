package org.jabref.gui.referencemetadata;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.entry.BibEntry;

public class ReferenceMetadataUtils {
    public static ObservableList<BibEntry> getAllEntriesStartingWithGivenIndex(int index, ObservableList<BibEntry> entries) {
        ObservableList<BibEntry> collectedEntries = FXCollections.observableArrayList();

        if (entries == null) {
            return collectedEntries;
        }

        for (int currIndex = index; currIndex < entries.size(); currIndex++) {
            collectedEntries.add(entries.get(currIndex));
        }

        return collectedEntries;
    }
}
