package org.jabref.gui.git;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.mergeentries.MergeEntriesDialog;
import org.jabref.gui.mergeentries.newmergedialog.ShowDiffConfig;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.DiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

/// A wrapper around {@link MergeEntriesDialog} for Git feature
///
/// Receives a semantic conflict (ThreeWayEntryConflict), pops up an interactive GUI (belonging to mergeentries), and returns a user-confirmed BibEntry merge result.
public class GitConflictResolverDialog {
    private final DialogService dialogService;
    private final GuiPreferences preferences;

    public GitConflictResolverDialog(DialogService dialogService, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    public Optional<BibEntry> resolveConflict(ThreeWayEntryConflict conflict) {
        BibEntry base = conflict.base();
        BibEntry local = conflict.local();
        BibEntry remote = conflict.remote();

        MergeEntriesDialog dialog = new MergeEntriesDialog(local, remote, preferences);
        dialog.setLeftHeaderText(Localization.lang("Local"));
        dialog.setRightHeaderText(Localization.lang("Remote"));
        ShowDiffConfig diffConfig = new ShowDiffConfig(
                ThreeWayMergeToolbar.DiffView.SPLIT,
                DiffHighlighter.BasicDiffMethod.WORDS
        );
        dialog.configureDiff(diffConfig);

        return dialogService.showCustomDialogAndWait(dialog)
                            .map(result -> result.mergedEntry());
    }
}
