package org.jabref.gui.externalfiles;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

import static org.jabref.gui.actions.ActionHelper.needsSavedLocalDatabase;

public class FindUnlinkedFilesAction extends SimpleCommand {

    public FindUnlinkedFilesAction(StateManager stateManager) {
        this.executable.bind(needsSavedLocalDatabase(stateManager));
    }

    @Override
    public void execute() {
        UnlinkedFilesWizard wizard = Injector.instantiateModelOrService(UnlinkedFilesWizard.class);

        wizard.show();
    }
}
