package org.jabref.gui.filewizard;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class FileWizardAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;

    FileWizardView fileWizardView;

    public FileWizardAction(DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        fileWizardView = new FileWizardView(this,
                dialogService, stateManager);
        fileWizardView.showAndWait();
    }

    public void closeFileWizardControlPanel() {
        fileWizardView.close();
    }
}
