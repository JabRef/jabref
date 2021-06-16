package org.jabref.gui.mergeentries;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;

public class MergeWithFetchedEntryAction extends SimpleCommand {

    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final StateManager stateManager;

    public MergeWithFetchedEntryAction(LibraryTab libraryTab, DialogService dialogService, StateManager stateManager) {
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(1, stateManager)
                                         .and(ActionHelper.isAnyFieldSetForSelectedEntry(FetchAndMergeEntry.SUPPORTED_FIELDS, stateManager)));
    }

    @Override
    public void execute() {
        if (stateManager.getSelectedEntries().size() != 1) {
            dialogService.showInformationDialogAndWait(
                    Localization.lang("Merge entry with %0 information", new OrFields(StandardField.DOI, StandardField.ISBN, StandardField.EPRINT).getDisplayName()),
                    Localization.lang("This operation requires exactly one item to be selected."));
        }

        BibEntry originalEntry = stateManager.getSelectedEntries().get(0);
        new FetchAndMergeEntry(libraryTab, Globals.TASK_EXECUTOR).fetchAndMerge(originalEntry);
    }
}
