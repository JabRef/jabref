package org.jabref.gui.mergeentries;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.gui.undo.UndoableRemoveEntry;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

public class MergeEntriesAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    private final DialogService dialogService;

    public MergeEntriesAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
        dialogService = jabRefFrame.getDialogService();
    }

    @Override
    public void execute() {
        BasePanel basePanel = jabRefFrame.getCurrentBasePanel();

        // Check if there are two entries selected
        List<BibEntry> selectedEntries = basePanel.getSelectedEntries();
        if (selectedEntries.size() != 2) {
            // Inform the user to select entries first.
            dialogService.showInformationDialogAndWait(
                    Localization.lang("Merge entries"),
                    Localization.lang("You have to choose exactly two entries to merge."));

            return;
        }

        // Store the two entries
        BibEntry one = selectedEntries.get(0);
        BibEntry two = selectedEntries.get(1);

        MergeEntriesDialog dlg = new MergeEntriesDialog(one, two, basePanel.getBibDatabaseContext().getMode());
        dlg.setTitle(Localization.lang("Merge entries"));
        Optional<BibEntry> mergedEntry = dlg.showAndWait();
        if (mergedEntry.isPresent()) {
            basePanel.insertEntry(mergedEntry.get());

            // Create a new entry and add it to the undo stack
            // Remove the other two entries and add them to the undo stack (which is not working...)
            NamedCompound ce = new NamedCompound(Localization.lang("Merge entries"));
            ce.addEdit(new UndoableInsertEntry(basePanel.getDatabase(), mergedEntry.get()));
            ce.addEdit(new UndoableRemoveEntry(basePanel.getDatabase(), one, basePanel));
            basePanel.getDatabase().removeEntry(one);
            ce.addEdit(new UndoableRemoveEntry(basePanel.getDatabase(), two, basePanel));
            basePanel.getDatabase().removeEntry(two);
            ce.end();
            basePanel.getUndoManager().addEdit(ce);

            dialogService.notify(Localization.lang("Merged entries"));
        } else {
            dialogService.notify(Localization.lang("Canceled merging entries"));
        }
    }

}
