package org.jabref.gui.mergeentries;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

public class MergeWithFetchedEntryAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final PreferencesService preferencesService;
    private final UndoManager undoManager;
    private final TaskExecutor taskExecutor;

    public MergeWithFetchedEntryAction(DialogService dialogService,
                                       StateManager stateManager,
                                       TaskExecutor taskExecutor,
                                       PreferencesService preferencesService,
                                       UndoManager undoManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.preferencesService = preferencesService;
        this.undoManager = undoManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(1, stateManager)
                                         .and(ActionHelper.isAnyFieldSetForSelectedEntry(FetchAndMergeEntry.SUPPORTED_FIELDS, stateManager)));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        if (stateManager.getSelectedEntries().size() != 1) {
            dialogService.showInformationDialogAndWait(
                    Localization.lang("Merge entry with %0 information", new OrFields(StandardField.DOI, StandardField.ISBN, StandardField.EPRINT).getDisplayName()),
                    Localization.lang("This operation requires exactly one item to be selected."));
        }

        BibEntry originalEntry = stateManager.getSelectedEntries().get(0);
        new FetchAndMergeEntry(stateManager.getActiveDatabase().get(), taskExecutor, preferencesService, dialogService, undoManager).fetchAndMerge(originalEntry);
    }
}
