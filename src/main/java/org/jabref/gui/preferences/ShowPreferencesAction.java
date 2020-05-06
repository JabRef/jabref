package org.jabref.gui.preferences;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ThemeLoader;
import org.jabref.preferences.JabRefPreferences;

public class ShowPreferencesAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    private final TaskExecutor taskExecutor;
    private final ThemeLoader themeLoader;
    private final JabRefPreferences prefs;

    public ShowPreferencesAction(JabRefFrame jabRefFrame, TaskExecutor taskExecutor, ThemeLoader themeLoader, JabRefPreferences prefs) {
        this.jabRefFrame = jabRefFrame;
        this.taskExecutor = taskExecutor;
        this.themeLoader = themeLoader;
        this.prefs = prefs;
    }

    @Override
    public void execute() {
        PreferencesDialogView preferencesDialogView = new PreferencesDialogView(jabRefFrame);
        preferencesDialogView.show();
        for (PreferencesTab tab: preferencesDialogView.getPreferenceTabList().getItems()) {
            if (tab.getBuilder().getScene() != null) {
                this.themeLoader.installCss(tab.getBuilder().getScene(), this.prefs);
            }
        }
    }
}
