package org.jabref.gui.mergeentries;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.BaseAction;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

public class MergeWithFetchedEntryAction implements BaseAction {

    private final BasePanel basePanel;
    private final JabRefFrame frame;

    public MergeWithFetchedEntryAction(BasePanel basePanel, JabRefFrame frame) {
        this.basePanel = basePanel;
        this.frame = frame;
    }

    @Override
    public void action() {
        if (basePanel.getMainTable().getSelectedEntries().size() == 1) {
            BibEntry originalEntry = basePanel.getMainTable().getSelectedEntries().get(0);
            new FetchAndMergeEntry(originalEntry, basePanel, FetchAndMergeEntry.SUPPORTED_FIELDS);
        } else {
            frame.getDialogService().showInformationDialogAndWait(Localization.lang("Merge entry with %0 information",
                    FieldName.orFields(FieldName.getDisplayName(FieldName.DOI),
                            FieldName.getDisplayName(FieldName.ISBN),
                            FieldName.getDisplayName(FieldName.EPRINT))),
                    Localization.lang("This operation requires exactly one item to be selected."));

        }
    }
}
