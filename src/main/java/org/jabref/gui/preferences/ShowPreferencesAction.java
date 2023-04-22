package org.jabref.gui.preferences;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.TaskExecutor;

import com.airhacks.afterburner.injection.Injector;

public class ShowPreferencesAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    private final TaskExecutor taskExecutor;
    private PreferencesTab preferencesTab;

    public ShowPreferencesAction(JabRefFrame jabRefFrame, TaskExecutor taskExecutor) {
        this.jabRefFrame = jabRefFrame;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Creates an action to open a preferences window at a specific tab
     * @param jabRefFrame
     * @param taskExecutor
     * @param preferencesTab Tab in the preferences window to be shown
     */
    public ShowPreferencesAction(JabRefFrame jabRefFrame, TaskExecutor taskExecutor, PreferencesTab preferencesTab) {
        this(jabRefFrame, taskExecutor);
        this.preferencesTab = preferencesTab;
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);

        if (this.preferencesTab != null) {
            dialogService.showCustomDialog(new PreferencesDialogView(jabRefFrame, this.preferencesTab));
        } else {
            dialogService.showCustomDialog(new PreferencesDialogView(jabRefFrame));
        }
    }
}
