package net.sf.jabref.gui.mergeentries;

import javax.swing.JOptionPane;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

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
