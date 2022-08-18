package org.jabref.gui.collab.experimental.entrychange;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.experimental.ExternalChange;
import org.jabref.gui.collab.experimental.ExternalChangeResolver;
import org.jabref.gui.mergeentries.EntriesMergeResult;
import org.jabref.gui.mergeentries.MergeEntriesDialog;
import org.jabref.gui.mergeentries.newmergedialog.ShowDiffConfig;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.DiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

public final class EntryChangeResolver extends ExternalChangeResolver {
    private final EntryChange entryChange;
    private final BibDatabaseContext databaseContext;

    public EntryChangeResolver(EntryChange entryChange, DialogService dialogService, BibDatabaseContext databaseContext) {
        super(dialogService);
        this.entryChange = entryChange;
        this.databaseContext = databaseContext;
    }

    @Override
    public Optional<ExternalChange> askUserToResolveChange() {
        MergeEntriesDialog mergeEntriesDialog = new MergeEntriesDialog(entryChange.getOldEntry(), entryChange.getNewEntry());
        mergeEntriesDialog.setLeftHeaderText(Localization.lang("On JabRef"));
        mergeEntriesDialog.setRightHeaderText(Localization.lang("On disk"));
        mergeEntriesDialog.configureDiff(new ShowDiffConfig(ThreeWayMergeToolbar.DiffView.SPLIT, DiffHighlighter.DiffMethod.WORDS));

        return dialogService.showCustomDialogAndWait(mergeEntriesDialog)
                            .map(this::mapMergeResultToExternalChange);
    }

    private EntryChange mapMergeResultToExternalChange(EntriesMergeResult entriesMergeResult) {
        return new EntryChange(
                entryChange.getOldEntry(),
                entryChange.getNewEntry(),
                databaseContext
        );
    }
}
