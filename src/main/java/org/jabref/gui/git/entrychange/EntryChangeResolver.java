package org.jabref.gui.collab.entrychange;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.GitChange;
import org.jabref.gui.collab.GitChangeResolver;
import org.jabref.gui.mergeentries.EntriesMergeResult;
import org.jabref.gui.mergeentries.MergeEntriesDialog;
import org.jabref.gui.mergeentries.newmergedialog.ShowDiffConfig;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.DiffHighlighter.BasicDiffMethod;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.GitContext;
import org.jabref.preferences.PreferencesService;

public final class EntryChangeResolver extends GitChangeResolver {
    private final EntryChange entryChange;
    private final GitContext databaseContext;

    private final PreferencesService preferencesService;

    public EntryChangeResolver(EntryChange entryChange, DialogService dialogService, GitContext databaseContext, PreferencesService preferencesService) {
        super(dialogService);
        this.entryChange = entryChange;
        this.databaseContext = databaseContext;
        this.preferencesService = preferencesService;
    }

    @Override
    public Optional<GitChange> askUserToResolveChange() {
        MergeEntriesDialog mergeEntriesDialog = new MergeEntriesDialog(entryChange.getOldEntry(), entryChange.getNewEntry(), preferencesService);
        mergeEntriesDialog.setLeftHeaderText(Localization.lang("In JabRef"));
        mergeEntriesDialog.setRightHeaderText(Localization.lang("On disk"));
        mergeEntriesDialog.configureDiff(new ShowDiffConfig(ThreeWayMergeToolbar.DiffView.SPLIT, BasicDiffMethod.WORDS));

        return dialogService.showCustomDialogAndWait(mergeEntriesDialog)
                            .map(this::mapMergeResultToExternalChange);
    }

    private EntryChange mapMergeResultToExternalChange(EntriesMergeResult entriesMergeResult) {
        return new EntryChange(
                entryChange.getOldEntry(),
                entriesMergeResult.mergedEntry(),
                databaseContext
        );
    }
}
