package org.jabref.gui.externalfiles;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

import static org.jabref.gui.actions.ActionHelper.needsSavedLocalDatabase;

public class FindUnlinkedFilesAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;

    public FindUnlinkedFilesAction(DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(needsSavedLocalDatabase(this.stateManager));
    }

    @Override
    public void execute() {
        UnlinkedFilesWizard wizard = (UnlinkedFilesWizard) Injector.instantiateModelOrService(UnlinkedFilesWizard.class);
        wizard.show();
    }
}
