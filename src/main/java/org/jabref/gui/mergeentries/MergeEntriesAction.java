package org.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.logic.bibtex.comparator.EntryComparator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;

public class MergeEntriesAction extends SimpleCommand {

    private final JabRefFrame frame;
    private final DialogService dialogService;
    private final StateManager stateManager;

    public MergeEntriesAction(JabRefFrame frame, DialogService dialogService, StateManager stateManager) {
        this.frame = frame;
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(2, stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }
        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().get();

        // Check if there are two entries selected
        List<BibEntry> selectedEntries = stateManager.getSelectedEntries();
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

        // compare two entries
        BibEntry first;
        BibEntry second;
        EntryComparator entryComparator = new EntryComparator(false, false, InternalField.KEY_FIELD);
        if (entryComparator.compare(one, two) <= 0) {
            first = one;
            second = two;
        } else {
            first = two;
            second = one;
        }

        MergeEntriesDialog dialog = new MergeEntriesDialog(first, second);
        dialog.setTitle(Localization.lang("Merge entries"));
        Optional<EntriesMergeResult> mergeResultOpt = dialogService.showCustomDialogAndWait(dialog);
        mergeResultOpt.ifPresentOrElse(entriesMergeResult -> {
            // TODO: BibDatabase::insertEntry does not contain logic to mark the BasePanel as changed and to mark
            //  entries with a timestamp, only BasePanel::insertEntry does. Workaround for the moment is to get the
            //  BasePanel from the constructor injected JabRefFrame. Should be refactored and extracted!
            frame.getCurrentLibraryTab().insertEntry(entriesMergeResult.mergedEntry());

            NamedCompound ce = new NamedCompound(Localization.lang("Merge entries"));
            ce.addEdit(new UndoableInsertEntries(databaseContext.getDatabase(), entriesMergeResult.mergedEntry()));
            List<BibEntry> entriesToRemove = Arrays.asList(entriesMergeResult.originalLeftEntry(), entriesMergeResult.originalRightEntry());
            ce.addEdit(new UndoableRemoveEntries(databaseContext.getDatabase(), entriesToRemove));
            databaseContext.getDatabase().removeEntries(entriesToRemove);
            ce.end();
            Globals.undoManager.addEdit(ce);

            dialogService.notify(Localization.lang("Merged entries"));
        }, () -> dialogService.notify(Localization.lang("Canceled merging entries")));
    }
}
