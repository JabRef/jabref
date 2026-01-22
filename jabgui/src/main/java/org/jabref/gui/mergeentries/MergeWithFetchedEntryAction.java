package org.jabref.gui.mergeentries;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;

public class MergeWithFetchedEntryAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final UndoManager undoManager;
    private final TaskExecutor taskExecutor;

    public MergeWithFetchedEntryAction(DialogService dialogService,
                                       StateManager stateManager,
                                       TaskExecutor taskExecutor,
                                       GuiPreferences preferences,
                                       UndoManager undoManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;
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
                    Localization.lang("Merge entry with %0 information", FieldTextMapper.getDisplayName(new OrFields(StandardField.DOI, StandardField.ISBN, StandardField.EPRINT))),
                    Localization.lang("This operation requires exactly one item to be selected."));
        }

        BibEntry originalEntry = stateManager.getSelectedEntries().getFirst();
        new FetchAndMergeEntry(stateManager.getActiveDatabase().get(), taskExecutor, preferences, dialogService, undoManager, stateManager).fetchAndMerge(originalEntry);
    }
}
