package org.jabref.logic.bibtex.comparator;

import java.util.List;

import org.jabref.gui.mergeentries.MergeEntriesDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class ConflictResolution {
    public static void resolveConflicts(BibDatabaseContext local, BibDatabaseContext remote, GuiPreferences preferences) {
        List<BibEntryDiff> conflicts = ConflictDetector.getEntryConflicts(local, remote);

        for (BibEntryDiff diff: conflicts) {
            BibEntry localEntry = diff.originalEntry();
            BibEntry remoteEntry = diff.newEntry();

            if (localEntry != null && remoteEntry != null) {
                MergeEntriesDialog mergeDialog = new MergeEntriesDialog(localEntry, remoteEntry, preferences);
                mergeDialog.showAndWait().ifPresent(result -> {
                    local.getDatabase().insertEntry(result.mergedEntry());
                });
            }
        }
    }
}
