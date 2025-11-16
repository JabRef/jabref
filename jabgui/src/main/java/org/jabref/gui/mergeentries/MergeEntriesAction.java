package org.jabref.gui.mergeentries;

import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.bibtex.comparator.EntryComparator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;

public class MergeEntriesAction extends SimpleCommand {
    private static final int NUMBER_OF_ENTRIES_NEEDED = 2;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final UndoManager undoManager;
    private final GuiPreferences preferences;

    public MergeEntriesAction(DialogService dialogService, StateManager stateManager, UndoManager undoManager, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        this.preferences = preferences;

        this.executable.bind(ActionHelper.needsEntriesSelected(NUMBER_OF_ENTRIES_NEEDED, stateManager));
    }

    @Override
    public void execute() {
        if (isNoActiveDatabase()) {
            return;
        }

        Optional<BibEntry[]> orderedEntriesOpt = getOrderedSelectedEntries();
        if (orderedEntriesOpt.isEmpty()) {
            return;
        }

        BibEntry first = orderedEntriesOpt.get()[0];
        BibEntry second = orderedEntriesOpt.get()[1];

        showMergeDialogAndHandleResult(first, second);
    }

    private boolean isNoActiveDatabase() {
        return stateManager.getActiveDatabase().isEmpty();
    }

    private Optional<BibEntry[]> getOrderedSelectedEntries() {
        List<BibEntry> selectedEntries = stateManager.getSelectedEntries();
        if (selectedEntries.size() != NUMBER_OF_ENTRIES_NEEDED) {
            dialogService.showInformationDialogAndWait(
                    Localization.lang("Merge entries"),
                    Localization.lang("You have to choose exactly two entries to merge."));
            return Optional.empty();
        }

        BibEntry one = selectedEntries.get(0);
        BibEntry two = selectedEntries.get(1);

        EntryComparator entryComparator = new EntryComparator(false, false, InternalField.KEY_FIELD);
        if (entryComparator.compare(one, two) <= 0) {
            return Optional.of(new BibEntry[]{one, two});
        } else {
            return Optional.of(new BibEntry[]{two, one});
        }
    }

    private void showMergeDialogAndHandleResult(BibEntry first, BibEntry second) {
        MergeEntriesDialog dialog = new MergeEntriesDialog(first, second, preferences);
        dialog.setTitle(Localization.lang("Merge entries"));

        Optional<EntriesMergeResult> mergeResultOpt = dialogService.showCustomDialogAndWait(dialog);
        mergeResultOpt.ifPresentOrElse(entriesMergeResult -> {
            new MergeTwoEntriesAction(entriesMergeResult, stateManager, undoManager).execute();
            dialogService.notify(Localization.lang("Merged entries"));
        }, () -> dialogService.notify(Localization.lang("Canceled merging entries")));
    }
}
