package org.jabref.gui.preferences;

import org.jabref.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
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
        PreferencesDialogView preferencesDialogView = new PreferencesDialogView(jabRefFrame);
        preferencesDialogView.show();
        for (PreferencesTab tab: preferencesDialogView.getPreferenceTabList().getItems()) {
            if (tab.getBuilder().getScene() != null) {
                Globals.getThemeLoader().installCss(tab.getBuilder().getScene(), Globals.prefs);
            }
        }
    }
}
