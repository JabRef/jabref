package org.jabref.gui.edit.automaticfiededitor;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

public class AutomaticFieldEditorAction extends SimpleCommand {

    private final StateManager stateManager;
    private final DialogService dialogService;

    public AutomaticFieldEditorAction(StateManager stateManager, DialogService dialogService) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;

        this.executable.bind(needsDatabase(stateManager).and(needsEntriesSelected(stateManager)));
    }

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new AutomaticFieldEditorDialog(stateManager));
    }
}
