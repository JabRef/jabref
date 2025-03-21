package org.jabref.logic.bibtex.comparator;

import java.util.List;
import java.util.Map;

import org.jabref.gui.mergeentries.MergeEntriesDialog;
import org.jabref.gui.mergeentries.newmergedialog.ShowDiffConfig;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.DiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar.DiffView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class ConflictResolution {

    public static void resolveGitConflicts(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote, GuiPreferences preferences) {
        List<GitBibEntryDiff> conflicts = ConflictDetector.getGitEntryConflicts(base, local, remote);

        for (GitBibEntryDiff diff : conflicts) {
            if (!diff.hasConflicts()) {
                BibEntry mergedEntry = createMergedEntry(diff);
                local.getDatabase().insertEntry(mergedEntry);
                continue;
            }

            BibEntry localEntry = diff.localEntry();
            BibEntry remoteEntry = diff.remoteEntry();
            if (localEntry != null && remoteEntry != null) {
                MergeEntriesDialog mergeDialog = new MergeEntriesDialog(
                        localEntry,
                        remoteEntry,
                        Localization.lang("Local version"),
                        Localization.lang("Remote version"),
                        preferences
                );

                // Create ShowDiffConfig with appropriate parameters
                ShowDiffConfig diffConfig = new ShowDiffConfig(
                        DiffView.UNIFIED,
                        DiffHighlighter.BasicDiffMethod.WORDS
                );

                // Simply pass the diffConfig to configureDiff
                // The ThreeWayMergeView will handle showing the differences
                mergeDialog.configureDiff(diffConfig);

                // The dialog already has access to both entries, so it should be able to
                // detect and display the differences without needing to specify fields

                mergeDialog.showAndWait().ifPresent(result -> {
                    local.getDatabase().insertEntry(result.mergedEntry());
                });
            } else if (localEntry != null) {
                local.getDatabase().insertEntry(localEntry);
            } else if (remoteEntry != null) {
                local.getDatabase().insertEntry(remoteEntry);
            }
        }
    }

    private static BibEntry createMergedEntry(GitBibEntryDiff diff) {
        BibEntry mergedEntry;
        if (diff.localEntry() != null) {
            mergedEntry = new BibEntry(String.valueOf(diff.localEntry()));
        } else if (diff.remoteEntry() != null) {
            mergedEntry = new BibEntry(String.valueOf(diff.remoteEntry()));
        } else {
            return new BibEntry();
        }

        for (Map.Entry<Field, GitBibEntryDiff.FieldChange> entry : diff.getFieldChanges().entrySet()) {
            Field field = entry.getKey();
            GitBibEntryDiff.FieldChange change = entry.getValue();

            String resolvedValue = change.getResolvedValue();
            if (resolvedValue != null) {
                if (resolvedValue.isEmpty()) {
                    mergedEntry.clearField(field);
                } else {
                    mergedEntry.setField(field, resolvedValue);
                }
            }
        }

        return mergedEntry;
    }
}
