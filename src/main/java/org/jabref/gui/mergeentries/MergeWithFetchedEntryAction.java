package org.jabref.gui.mergeentries;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.actions.BaseAction;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

public class MergeWithFetchedEntryAction implements BaseAction {

    private final BasePanel basePanel;
    private final DialogService dialogService;

    public MergeWithFetchedEntryAction(BasePanel basePanel, DialogService dialogService) {
        this.basePanel = basePanel;
        this.dialogService = dialogService;
    }

    @Override
    public void action() {
        if (basePanel.getMainTable().getSelectedEntries().size() == 1) {
            BibEntry originalEntry = basePanel.getMainTable().getSelectedEntries().get(0);
            new FetchAndMergeEntry(basePanel, Globals.TASK_EXECUTOR).fetchAndMerge(originalEntry);
        } else {
            dialogService.showInformationDialogAndWait(Localization.lang("Merge entry with %0 information",
                    FieldName.orFields(FieldName.getDisplayName(FieldName.DOI),
                            FieldName.getDisplayName(FieldName.ISBN),
                            FieldName.getDisplayName(FieldName.EPRINT))),
                    Localization.lang("This operation requires exactly one item to be selected."));

        }
    }
}
