package org.jabref.gui.edit;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

public class ReplaceStringAction extends SimpleCommand {
    private final JabRefFrame frame;

    public ReplaceStringAction(JabRefFrame frame, StateManager stateManager) {
        this.frame = frame;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialogAndWait(new ReplaceStringView(frame.getCurrentLibraryTab()));
    }
}
