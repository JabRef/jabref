package org.jabref.gui.externalfiles;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class FindUnlinkedFilesAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;

    public FindUnlinkedFilesAction(DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(needsDatabase(this.stateManager));
    }

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new UnlinkedFilesDialogView());
    }
}
