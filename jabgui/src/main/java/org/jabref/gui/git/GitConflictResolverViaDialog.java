package org.jabref.gui.git;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.mergeentries.MergeEntriesDialog;
import org.jabref.gui.mergeentries.newmergedialog.ShowDiffConfig;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.DiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.model.entry.BibEntry;

/**
 * UI wrapper
 * Receives a semantic conflict (ThreeWayEntryConflict), pops up an interactive GUI (belonging to mergeentries), and returns a user-confirmed BibEntry merge result.
 */
public class GitConflictResolverViaDialog implements GitConflictResolver {
    private final DialogService dialogService;
    private final GuiPreferences preferences;

    public GitConflictResolverViaDialog(DialogService dialogService, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    @Override
    public Optional<BibEntry> resolveConflict(ThreeWayEntryConflict conflict) {
        BibEntry base = conflict.base();
        BibEntry local = conflict.local();
        BibEntry remote = conflict.remote();

        // Create Dialog + Set Title + Configure Diff Highlighting
        MergeEntriesDialog dialog = new MergeEntriesDialog(local, remote, preferences);
        dialog.setLeftHeaderText("Local");
        dialog.setRightHeaderText("Remote");
        ShowDiffConfig diffConfig = new ShowDiffConfig(
                ThreeWayMergeToolbar.DiffView.SPLIT,
                DiffHighlighter.BasicDiffMethod.WORDS
        );
        dialog.configureDiff(diffConfig);

        return dialogService.showCustomDialogAndWait(dialog)
                            .map(result -> result.mergedEntry());
    }
}
