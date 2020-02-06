package org.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class MergeEntriesAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    private final DialogService dialogService;

    public MergeEntriesAction(JabRefFrame jabRefFrame, StateManager stateManager) {
        this.jabRefFrame = jabRefFrame;
        this.dialogService = jabRefFrame.getDialogService();

        this.executable.bind(needsDatabase(stateManager));
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

        MergeEntriesDialog dlg = new MergeEntriesDialog(one, two);
        dlg.setTitle(Localization.lang("Merge entries"));
        Optional<BibEntry> mergedEntry = dlg.showAndWait();
        if (mergedEntry.isPresent()) {
            basePanel.insertEntry(mergedEntry.get());

            // Create a new entry and add it to the undo stack
            // Remove the other two entries and add them to the undo stack (which is not working...)
            NamedCompound ce = new NamedCompound(Localization.lang("Merge entries"));
            ce.addEdit(new UndoableInsertEntries(basePanel.getDatabase(), mergedEntry.get()));
            List<BibEntry> entriesToRemove = Arrays.asList(one, two);
            ce.addEdit(new UndoableRemoveEntries(basePanel.getDatabase(), entriesToRemove));
            basePanel.getDatabase().removeEntries(entriesToRemove);
            ce.end();
            basePanel.getUndoManager().addEdit(ce);

            dialogService.notify(Localization.lang("Merged entries"));
        } else {
            dialogService.notify(Localization.lang("Canceled merging entries"));
        }
    }

}
