package org.jabref.gui.mergeentries;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.bibtex.comparator.EntryComparator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.preferences.PreferencesService;

public class MergeEntriesAction extends SimpleCommand {
    private static final int NUMBER_OF_ENTRIES_NEEDED = 2;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final PreferencesService preferencesService;

    public MergeEntriesAction(DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;

        this.executable.bind(ActionHelper.needsEntriesSelected(NUMBER_OF_ENTRIES_NEEDED, stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

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

        MergeEntriesDialog dialog = new MergeEntriesDialog(first, second, preferencesService);
        dialog.setTitle(Localization.lang("Merge entries"));

        Optional<EntriesMergeResult> mergeResultOpt = dialogService.showCustomDialogAndWait(dialog);
        mergeResultOpt.ifPresentOrElse(entriesMergeResult -> {
            new MergeTwoEntriesAction(entriesMergeResult, stateManager).execute();

            dialogService.notify(Localization.lang("Merged entries"));
        }, () -> dialogService.notify(Localization.lang("Canceled merging entries")));
    }
}
