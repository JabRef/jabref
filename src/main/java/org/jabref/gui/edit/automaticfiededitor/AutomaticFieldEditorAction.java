package org.jabref.gui.edit.automaticfiededitor;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

public class AutomaticFieldEditorAction extends SimpleCommand {
    private final StateManager stateManager;

    public AutomaticFieldEditorAction(StateManager stateManager) {
        this.stateManager = stateManager;
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialogAndWait(new AutomaticFieldEditorDialog(stateManager.getSelectedEntries(),
                stateManager.getActiveDatabase().orElseThrow()));
    }
}
