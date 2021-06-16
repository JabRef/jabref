package org.jabref.gui.edit;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

/**
 * An Action for launching mass field.
 * <p>
 * Functionality:
 * <ul>
 *     <li>Defaults to selected entries, or all entries if none are selected.</li>
 *     <li>Input field name</li>
 *     <li>Either set field, or clear field.</li>
 * </ul>
 */
public class MassSetFieldsAction extends SimpleCommand {

    private final StateManager stateManager;
    private DialogService dialogService;
    private UndoManager undoManager;

    public MassSetFieldsAction(StateManager stateManager, DialogService dialogService, UndoManager undoManager) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.undoManager = undoManager;

        this.executable.bind(needsDatabase(stateManager).and(needsEntriesSelected(stateManager)));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        dialogService.showCustomDialogAndWait(new MassSetFieldsDialog(stateManager.getSelectedEntries(), database, dialogService, undoManager));
    }
}
