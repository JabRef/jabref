package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.preferences.PreferencesDialog;
import org.jabref.gui.util.TaskExecutor;

public class ShowPreferencesAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    private final TaskExecutor taskExecutor;

    public ShowPreferencesAction(JabRefFrame jabRefFrame, TaskExecutor taskExecutor) {
        this.jabRefFrame = jabRefFrame;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        PreferencesDialog preferencesDialog = new PreferencesDialog(jabRefFrame, taskExecutor);
        preferencesDialog.showAndWait();
    }
}
