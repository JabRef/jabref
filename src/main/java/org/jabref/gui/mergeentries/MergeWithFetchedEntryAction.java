package org.jabref.gui.mergeentries;

import javax.swing.JOptionPane;

import org.jabref.gui.BasePanel;
import org.jabref.gui.actions.BaseAction;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

public class MergeWithFetchedEntryAction implements BaseAction {

    private final BasePanel basePanel;

    public MergeWithFetchedEntryAction(BasePanel basePanel) {
        this.basePanel = basePanel;
    }

    @Override
    public void action() {
        if (basePanel.getMainTable().getSelectedEntries().size() == 1) {
            BibEntry originalEntry = basePanel.getMainTable().getSelectedEntries().get(0);
            new FetchAndMergeEntry(originalEntry, basePanel, FetchAndMergeEntry.SUPPORTED_FIELDS);
        } else {
            JOptionPane.showMessageDialog(basePanel.frame(),
                    Localization.lang("This operation requires exactly one item to be selected."),
                    Localization.lang("Merge entry with %0 information",
                            FieldName.orFields(FieldName.getDisplayName(FieldName.DOI),
                                    FieldName.getDisplayName(FieldName.ISBN),
                                    FieldName.getDisplayName(FieldName.EPRINT))),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
